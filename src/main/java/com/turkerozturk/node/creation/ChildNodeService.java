package com.turkerozturk.node.creation;

import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class ChildNodeService {
    private final NodePropertiesService properties;
    private final CustomPropertiesHolder settings;
    @PersistenceContext private EntityManager entityManager;

    public ChildNodeService(NodePropertiesService properties, CustomPropertiesHolder settings) {
        this.properties = properties;
        this.settings = settings;
    }

    @Transactional
    public long create(long parentId) {
        if (!properties.writable()) {
            throw new AccessDeniedException("The selected CTB is read-only.");
        }
        properties.realNode(parentId); // Shared nodes cannot own children.
        Map<String, String> defaults = settings.getCustomProperties(TenantContext.getCurrentTenant());
        String name = defaults == null ? "New node" : defaults.getOrDefault("custom.newNodeName", "New node");
        String tags = defaults == null ? "" : defaults.getOrDefault("custom.newNodeTags", "");
        long timestamp = Instant.now().getEpochSecond();

        // Alias ids live only in children; reserve a number above both tables.
        long id = ((Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(node_id), 0) + 1 FROM "
                + "(SELECT node_id FROM node UNION ALL SELECT node_id FROM children)")
                .getSingleResult()).longValue();
        long sequence = ((Number) entityManager.createNativeQuery(
                "SELECT COALESCE(MAX(sequence), 0) + 1 FROM children WHERE father_id = :parent")
                .setParameter("parent", parentId).getSingleResult()).longValue();

        entityManager.createNativeQuery("INSERT INTO node (node_id, name, txt, syntax, tags, "
                + "is_ro, is_richtxt, has_codebox, has_table, has_image, level, ts_creation, ts_lastsave) "
                + "VALUES (:id, :name, '', 'plain-text', :tags, 0, 0, 0, 0, 0, 0, :timestamp, :timestamp)")
                .setParameter("id", id).setParameter("name", name).setParameter("tags", tags)
                .setParameter("timestamp", timestamp).executeUpdate();
        entityManager.createNativeQuery("INSERT INTO children (node_id, father_id, sequence, master_id) "
                + "VALUES (:id, :parent, :sequence, 0)")
                .setParameter("id", id).setParameter("parent", parentId)
                .setParameter("sequence", sequence).executeUpdate();
        return id;
    }
}

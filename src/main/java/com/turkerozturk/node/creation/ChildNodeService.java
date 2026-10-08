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
        return createNode(parentId, false);
    }

    @Transactional
    public long createTopLevel() {
        return createNode(0, true);
    }

    @Transactional
    public long createSibling(long occurrenceId) {
        if (!properties.writable()) throw new AccessDeniedException("The selected CTB is read-only.");
        var rows = entityManager.createNativeQuery(
                "SELECT father_id, sequence FROM children WHERE node_id = :id")
                .setParameter("id", occurrenceId).getResultList();
        if (rows.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND);
        Object[] occurrence = (Object[]) rows.get(0);
        long parent = ((Number) occurrence[0]).longValue();
        long sequence = ((Number) occurrence[1]).longValue();
        long id = createNode(parent, parent == 0);
        entityManager.createNativeQuery("UPDATE children SET sequence = sequence + 1 "
                + "WHERE father_id = :parent AND sequence > :sequence AND node_id <> :id")
                .setParameter("parent", parent).setParameter("sequence", sequence)
                .setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("UPDATE children SET sequence = :sequence WHERE node_id = :id")
                .setParameter("sequence", sequence + 1).setParameter("id", id).executeUpdate();
        return id;
    }

    private long createNode(long parentId, boolean topLevel) {
        if (!properties.writable()) {
            throw new AccessDeniedException("The selected CTB is read-only.");
        }
        if (!topLevel) {
            // Parent is a tree occurrence; a shared occurrence can own its own children.
            long count = ((Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM children WHERE node_id = :id")
                    .setParameter("id", parentId).getSingleResult()).longValue();
            if (parentId <= 0 || count != 1) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND);
            }
        }
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

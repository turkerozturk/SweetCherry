package com.turkerozturk.node.contentediting;

import com.turkerozturk.helpers.BitOperation;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class NodeContentEditingService {
    private final NodePropertiesService properties;
    @PersistenceContext private EntityManager entityManager;

    public NodeContentEditingService(NodePropertiesService properties) {
        this.properties = properties;
    }

    public Node editableNode(long nodeId) {
        if (!properties.writable()) {
            throw new AccessDeniedException("The selected CTB is read-only.");
        }
        Node node = properties.realNode(nodeId);
        if (node.getSyntax() == null || "custom-colors".equals(node.getSyntax())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This node's content cannot be edited here.");
        }
        if (BitOperation.processSixteenBitData((int) node.getIsReadOnly16bit()).isReadOnly()) {
            throw new AccessDeniedException("The node content is read-only.");
        }
        return node;
    }

    @Transactional
    public void update(long nodeId, String text) {
        if (text == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text is required.");
        }
        editableNode(nodeId);
        // Recheck flags in the UPDATE in case CherryTree or another tab changed them after the SELECT.
        int updated = entityManager.createNativeQuery("UPDATE node SET txt = :text, ts_lastsave = :timestamp "
                        + "WHERE node_id = :id AND (is_ro & 1) = 0 AND syntax <> 'custom-colors'")
                .setParameter("text", text)
                .setParameter("timestamp", Instant.now().getEpochSecond())
                .setParameter("id", nodeId)
                .executeUpdate();
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Node changed before it could be saved.");
        }
    }
}

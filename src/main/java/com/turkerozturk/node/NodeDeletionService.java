/*
 * This file is part of the SweetCherry project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/SweetCherry
 *
 * Copyright (c) 2024 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.node;

import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NodeDeletionService {
    private static final Logger logger = LoggerFactory.getLogger(NodeDeletionService.class);

    private final CustomPropertiesHolder customPropertiesHolder;
    private final NodeRepository nodeRepository;
    private final ChildrenRepository childrenRepository;
    private final ChildrenService childrenService;

    @PersistenceContext
    private EntityManager entityManager;

    public NodeDeletionService(CustomPropertiesHolder customPropertiesHolder,
                               NodeRepository nodeRepository,
                               ChildrenRepository childrenRepository,
                               ChildrenService childrenService) {
        this.customPropertiesHolder = customPropertiesHolder;
        this.nodeRepository = nodeRepository;
        this.childrenRepository = childrenRepository;
        this.childrenService = childrenService;
    }

    public boolean isCurrentTenantWritable() {
        String tenant = TenantContext.getCurrentTenant();
        Map<String, String> properties = tenant == null ? Collections.emptyMap() :
                customPropertiesHolder.getCustomProperties(tenant);
        return properties != null && Boolean.parseBoolean(properties.get("custom.isWritable"));
    }

    /** A shared node has a children row but no node row. */
    @Transactional
    public void deleteNodeWithSubNodes(long nodeId) {
        if (!isCurrentTenantWritable()) {
            throw new AccessDeniedException("The selected CTB is read-only.");
        }
        Children selected = childrenRepository.findByNodeId(nodeId);
        if (selected == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found");
        }

        // A shared node is only a reference. Deleting it must never delete its master.
        if (isShared(selected)) {
            ensureNoChildren(selected.getNodeId());
            deleteRow("bookmark", selected.getNodeId());
            deleteRow("children", selected.getNodeId());
            logger.info("Deleted shared node {} (master {}).", nodeId, selected.getMasterId());
            return;
        }

        List<Children> subtree = childrenService.findAllSubChildren(nodeId);
        List<Long> realIds = new ArrayList<>();
        Set<Long> sharedIds = new HashSet<>();
        for (Children entry : subtree) {
            if (isShared(entry)) {
                sharedIds.add(entry.getNodeId());
            } else {
                realIds.add(entry.getNodeId());
            }
        }

        // References to deleted real nodes may live outside the selected subtree.
        // Validate first: a shared node with children needs an explicit migration policy.
        for (long realId : realIds) {
            for (Children alias : childrenRepository.findByMasterId(realId)) {
                sharedIds.add(alias.getNodeId());
            }
        }
        for (long sharedId : sharedIds) {
            ensureNoChildren(sharedId);
        }
        for (long realId : realIds) {
            if (!nodeRepository.existsById(realId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A real node is missing from the node table: " + realId);
            }
        }

        for (long sharedId : sharedIds) {
            deleteRow("bookmark", sharedId);
            deleteRow("children", sharedId);
        }
        // findAllSubChildren lists parents before descendants; delete in reverse order.
        for (int i = realIds.size() - 1; i >= 0; i--) {
            long realId = realIds.get(i);
            deleteRow("bookmark", realId);
            deleteRow("codebox", realId);
            deleteRow("image", realId);
            deleteRow("grid", realId);
            deleteRow("children", realId);
            deleteRow("node", realId);
        }
        logger.info("Deleted node {} and its subtree ({} real nodes, {} shared references).",
                nodeId, realIds.size(), sharedIds.size());
    }

    private static boolean isShared(Children entry) {
        return entry.getMasterId() != null && entry.getMasterId() != 0;
    }

    private void ensureNoChildren(long nodeId) {
        if (!childrenRepository.findByFatherId(nodeId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Shared node " + nodeId + " has child rows; deletion was cancelled to avoid orphan nodes.");
        }
    }

    // Native bulk deletes avoid keeping a managed Node with a now-deleted Bookmark reference.
    // The table name is supplied only by the fixed calls above; the node id is bound.
    private void deleteRow(String table, long nodeId) {
        entityManager.createNativeQuery("DELETE FROM " + table + " WHERE node_id = :nodeId")
                .setParameter("nodeId", nodeId)
                .executeUpdate();
    }
}

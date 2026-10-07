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

import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class NodeDeletionService {
    private static final Logger logger = LoggerFactory.getLogger(NodeDeletionService.class);

    private final CustomPropertiesHolder customPropertiesHolder;

    @PersistenceContext
    private EntityManager entityManager;

    public NodeDeletionService(CustomPropertiesHolder customPropertiesHolder,
                               NodeRepository nodeRepository,
                               ChildrenRepository childrenRepository,
                               ChildrenService childrenService) {
        this.customPropertiesHolder = customPropertiesHolder;
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
        entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> NodeDeletionSql.delete(connection, nodeId));
        entityManager.clear();
        logger.info("Deleted tree occurrence {} and its descendants; surviving shared groups preserved.", nodeId);
    }
}

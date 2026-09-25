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


import com.turkerozturk.bookmark.Bookmark;
import com.turkerozturk.bookmark.BookmarkRepository;
import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.codebox.CodeBox;
import com.turkerozturk.codebox.CodeBoxRepository;
import com.turkerozturk.grid.Grid;
import com.turkerozturk.grid.GridRepository;
import com.turkerozturk.image.Image;
import com.turkerozturk.image.ImageRepository;
import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import org.springframework.security.access.AccessDeniedException;

@Service
@Transactional
public class NodeDeletionService {

    private static final Logger logger = LoggerFactory.getLogger(NodeDeletionService.class);

    @Autowired
    private CustomPropertiesHolder customPropertiesHolder;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ChildrenService childrenService;

    @Autowired
    private ChildrenRepository childrenRepository;

    @Autowired
    private GridRepository gridRepository;

    @Autowired
    private CodeBoxRepository codeBoxRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    public boolean isCurrentTenantWritable() {
        String tenant = TenantContext.getCurrentTenant();
        Map<String, String> properties = tenant == null ? Collections.emptyMap() :
                customPropertiesHolder.getCustomProperties(tenant);
        return properties != null && Boolean.parseBoolean(properties.get("custom.isWritable"));
    }

    /**
     * TODO refactor this method with CASCADE
     * FOR ONE NODE, deletes all node related data from all tables
     * @param nodeId
     */
    private void deleteNode(long nodeId) {

        Node node = nodeRepository.findById(nodeId);

        if (node == null ) {

            // Eğer Node = null ise, o halde frontend'de ağaç yapısında görülen Node aslında shared Node'dir.
            // Dolayısıyla nodeId aslında children tablosundaki node_id'de yazılı olan shared nodeye ait olan kayıttır.
            Children children = childrenRepository.findByNodeId(nodeId);
            if (children != null) {
                childrenRepository.deleteByNodeId(nodeId);
            }
        } else {

            Bookmark bookmark = node.getBookmark();
            if(bookmark != null) {
                bookmarkRepository.deleteByNodeId(nodeId);
            }

            List<CodeBox> codeBoxes = codeBoxRepository.findByIdNodeId((int) nodeId);
            if(codeBoxes != null) {
                codeBoxRepository.deleteByIdNodeId(nodeId);
            }

            Set<Image> images = node.getImages();
            if (images != null) {
                imageRepository.deleteByNodeId(nodeId);
            }

            List<Grid> grids = gridRepository.findByIdNodeId((int) nodeId);
            if(grids != null) {
                gridRepository.deleteByIdNodeId(nodeId);
            }

            Children children = childrenRepository.findByNodeId(nodeId);
            if(children != null) {
                childrenRepository.deleteByNodeId(nodeId);
            }

            nodeRepository.deleteByNodeId(nodeId);


            logger.info(String.format("The node with id number %s is deleted.", nodeId));
        }




            // Shared Node denen şey aslında children tablosunda bir kayıt. Bağlı olduğu gerçek Node onun master_id'si.
            // Gerçek node silineceği zaman, orphan kayıt kalmaması için ona bağlı shared nodeler de silinmelidir.
            List<Children> sharedNodes = childrenRepository.findByMasterId(nodeId);
            if(sharedNodes != null) {
                for(Children sharedNode : sharedNodes) {
                    childrenRepository.deleteByNodeId(sharedNode.getNodeId());
                    logger.info(String.format("The shared node with id number %s is deleted.", sharedNode.getNodeId()));
                }

            }









    }


    /**
     * DELETES all data of a given nodeId and its tree of subNodes if they exist.
     * @param nodeId
     */
    public void deleteNodeWithSubNodes(long nodeId) {

        if (!isCurrentTenantWritable()) {
            throw new AccessDeniedException("The selected CTB is read-only. Enable custom.isWritable in its tenant definition to delete nodes.");
        }

        List<Children> children = childrenService.findAllSubChildren(nodeId);
        // Delete descendants before their parent to preserve the tree's references.
        for (int i = children.size() - 1; i >= 0; i--) {
            deleteNode(children.get(i).getNodeId());
        }
        logger.info("The node with id number % and all its subNodes are deleted.", nodeId);
    }


}

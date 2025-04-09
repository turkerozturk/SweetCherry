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

    /**
     * TODO refactor this method with CASCADE
     * FOR ONE NODE, deletes all node related data from all tables
     * @param nodeId
     */
    private void deleteNode(long nodeId) {

        Node node = nodeRepository.findById(nodeId);


        if (node != null ) {
            nodeRepository.deleteByNodeId(nodeId);
        }

        Children children = childrenRepository.findByNodeId(nodeId);
        if(children != null) {
            childrenRepository.deleteByNodeId(nodeId);
        }

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

        logger.info(String.format("The node with id number %s is deleted.", nodeId));


    }


    /**
     * DELETES all data of a given nodeId and its tree of subNodes if they exist.
     * @param nodeId
     */
    public void deleteNodeWithSubNodes(long nodeId) {

        //logger.info("getCurrentTenant: " + TenantContext.getCurrentTenant());

        Map<String, String> customProperties = customPropertiesHolder.getCustomProperties(TenantContext.getCurrentTenant());

        if(customProperties.containsKey("custom.isWritable")) {
            String isWritableAsString = customProperties.get("custom.isWritable");
            //logger.info("isWritable: " + isWritableAsString + " (" + tenantName + ")");
            boolean isWritable = isWritableAsString.equals("true");


            // bilgi CTB yi ayar dosyasinda writable olarak belirlemissek, delete islemi yapabiliriz.
            if(isWritable) {

                List<Children> childrens = childrenService.findAllSubChildren(nodeId);
                for(Children children: childrens) {

                    deleteNode(children.getNodeId());
                }
                logger.info(String.format("The node with id number %s and all its subNodes are deleted.", nodeId));

            }

        }


    }


}

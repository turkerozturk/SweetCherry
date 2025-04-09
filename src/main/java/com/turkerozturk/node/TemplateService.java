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

import com.turkerozturk.children.ChildrenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TemplateService {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private ChildrenService childrenService;

    public TemplateNode getTemplateNode(long templateNodeId) {
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        TemplateNode templateNode = new TemplateNode(nodeService.getById(templateNodeId));
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        List<Node> rawProjectNodes = nodeService.getNodesByTags(templateNode.getName());
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        List<ProjectNode> projectNodes = new ArrayList<>();
        for(Node rawProjectNode : rawProjectNodes) {
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            LinkedHashMap<Long, String> breadcrumbs = nodeService.addBreadcrumbs(rawProjectNode.getNodeId());
            rawProjectNode.setBreadcrumbs(breadcrumbs);
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            List<Node> childrenNodes = childrenService.getNodesByFatherId(rawProjectNode.getNodeId());
            Map<String, DataNode> dataNodesMap = new HashMap<>();
            for(Node childrenNode : childrenNodes) {
                for(String dataLabel : templateNode.getDataLabels().keySet()) {
                    if(childrenNode.getName().equals(dataLabel)) {
                        String humanFriendlyLabel = templateNode.getDataLabels().get(dataLabel);
                        DataNode dataNode = new DataNode(childrenNode, humanFriendlyLabel);
                        dataNodesMap.put(dataLabel, dataNode);
                    }
                }
            }

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            ProjectNode projectNode = new ProjectNode(rawProjectNode, nodeService.doBreadcrumbsToString(breadcrumbs));
            projectNode.setDataLabels(templateNode.getDataLabels());
            projectNode.setDataNodes(dataNodesMap);
            projectNode.setMissingDataLabels(findMissingLabels(templateNode.getDataLabels().keySet(), dataNodesMap));
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            projectNodes.add(projectNode);
        }
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // projectNodes listesini projenin adina gore alfabetik olarak sirala
        Collections.sort(projectNodes, Comparator.comparing(ProjectNode::getName));
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        templateNode.setProjectNodes(projectNodes);
        return templateNode;
    }

    /**
     * If the user not used some dataLabes whose defined in template node, this method will collect those dataLabels
     * to show them to the user for information.
     * @param dataLabels
     * @param dataNodesMap
     * @return
     */
    public static Set<String> findMissingLabels(Set<String> dataLabels, Map<String, DataNode> dataNodesMap) {
        Set<String> missingLabels = new TreeSet<>();

        for (String label : dataLabels) {
            if (!dataNodesMap.containsKey(label)) {
                missingLabels.add(label);
            }
        }

        return missingLabels;
    }

}

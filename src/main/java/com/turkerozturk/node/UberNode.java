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

import java.util.List;





public class UberNode {

        private long nodeId;
        private String name;
        //private String[] templateNodeNames;

        private List<Node> templateNodes;

    //https://www.baeldung.com/java-records-custom-constructor
    //this kelimesi constructor metodu icindeki ilk kelime olmak sartiyla, record icerisine constructor eklenebiliyor.

    public UberNode(Node node, List<Node> templateNodes) {

       this.nodeId = node.getNodeId();
       this.name = node.getName();
       this.templateNodes = templateNodes;
       //this.templateNodeNames = v(node);
    }



/*
    private String[] v(Node node) {
        String[] templateNodeNames = node.getTxt().split("\\r?\\n");
        templateNodes = new ArrayList<>();
        for(int i = 0; i < templateNodeNames.length; i++) {
            Node templateNode = nodeService.getTemplateNode();
            templateNodes.add(templateNode);
        }


        return templateNodeNames;
    }
*/

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Node> getTemplateNodes() {
        return templateNodes;
    }

    public void setTemplateNodes(List<Node> templateNodes) {
        this.templateNodes = templateNodes;
    }

    /*
    public String[] getTemplateNodeNames() {
        return templateNodeNames;
    }

    public void setTemplateNodeNames(String[] templateNodeNames) {
        this.templateNodeNames = templateNodeNames;
    }
    */
}

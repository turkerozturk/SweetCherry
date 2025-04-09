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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ProjectNode {
        private long nodeId;
        private String name;
        private String templateName;
        private Map<String, String> dataLabels;
        private Map<String, DataNode> dataNodes;
        private LinkedHashMap<Long, String> breadcrumbs;
        private String breadcrumbsAsString;

    /**
     * if the project node has not a subnode who defined as data label in the template node,
     * it will added here to show to the user
     **/
    private Set<String> missingDataLabels;


    public ProjectNode(Node node) {
        this.nodeId = node.getNodeId();
        this.name = node.getName();
        this.templateName = node.getTags();
        this.breadcrumbs = node.getBreadcrumbs();
    }

    public ProjectNode(Node node, String breadcrumbsAsString) {
        this.nodeId = node.getNodeId();
        this.name = node.getName();
        this.templateName = node.getTags();
        this.breadcrumbsAsString = breadcrumbsAsString;
    }

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

    public Map<String, DataNode> getDataNodes() {
        return dataNodes;
    }

    public void setDataNodes(Map<String, DataNode> dataNodes) {
        this.dataNodes = dataNodes;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Map<String, String> getDataLabels() {
        return dataLabels;
    }

    public void setDataLabels(Map<String, String> dataLabels) {
        this.dataLabels = dataLabels;
    }

    public Set<String> getMissingDataLabels() {
        return missingDataLabels;
    }

    public void setMissingDataLabels(Set<String> missingDataLabels) {
        this.missingDataLabels = missingDataLabels;
    }

    public LinkedHashMap<Long, String> getBreadcrumbs() {
        return breadcrumbs;
    }

    public void setBreadcrumbs(LinkedHashMap<Long, String> breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }

    public String getBreadcrumbsAsString() {
        return breadcrumbsAsString;
    }

    public void setBreadcrumbsAsString(String breadcrumbsAsString) {
        this.breadcrumbsAsString = breadcrumbsAsString;
    }
}

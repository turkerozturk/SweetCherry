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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateNode {
        private long nodeId;
        private String name;
        private Map<String, String> dataLabels;
        private List<ProjectNode> projectNodes;


    public TemplateNode(Node node) {
        this.nodeId = node.getNodeId();
        this.name = node.getName();
        this.dataLabels = setDataLabels(node.getTxt());
    }

    private java.util.List<String> definitionIssues = java.util.List.of();
    public java.util.List<String> getDefinitionIssues(){return definitionIssues;}

    /** Retain definition order; a leading ASCII space remains a legacy comment marker. */
    public Map<String, String> setDataLabels(String raw) {
        var labels = new java.util.LinkedHashMap<String,String>();
        var issues = new java.util.ArrayList<String>();
        var repeated = new java.util.HashSet<String>();
        int line = 0;
        for(String original : java.util.Objects.toString(raw, "").split("\\R", -1)) {
            line++;
            String clean = NodeNames.clean(original);
            if(clean.isEmpty() || clean.startsWith("#") || original.startsWith(" ")) continue;
            String[] parts = original.split("\\t", 2);
            String key = NodeNames.clean(parts[0]);
            if(key.isEmpty()) {issues.add("["+line+"] ∅");continue;}
            String label = parts.length == 1 ? key : NodeNames.clean(parts[1]);
            if(label.isEmpty())label=key;
            if(labels.containsKey(key) || repeated.contains(key)) {
                labels.remove(key);repeated.add(key);issues.add("["+line+"] "+key);continue;
            }
            labels.put(key,label);
        }
        definitionIssues = java.util.List.copyOf(issues);
        return labels;
    }

    /*
    public TemplateNode(Node node, List<ProjectNode> projectNodes) {

       this.nodeId = node.getNodeId();
       this.name = node.getName();
       this.dataLabels = node.getTxt().split("\\r?\\n");
       this.projectNodes = projectNodes;
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

    public Map<String, String> getDataLabels() {
        return dataLabels;
    }

    public void setDataLabels(Map<String, String> dataLabels) {
        this.dataLabels = dataLabels;
    }

    public List<ProjectNode> getProjectNodes() {
        return projectNodes;
    }

    public void setProjectNodes(List<ProjectNode> projectNodes) {
        this.projectNodes = projectNodes;
    }
}

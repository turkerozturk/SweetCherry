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

    @Autowired private NodeService nodeService;
    @Autowired private com.turkerozturk.children.ChildrenRepository children;
    @Autowired private com.turkerozturk.richtext.experimental.RichTextRenderingService richText;
    @Autowired private com.turkerozturk.exportmindmap.MindMapTextContent textContent;

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public TemplateNode getTemplateNode(long id) {return getTemplateNode(id, "");}

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public TemplateNode getTemplateNode(long id,String tenantView) {
        Node source=nodeService.findDisplayNode(id);
        if(source==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        var template=new TemplateNode(source);template.setNodeId(id);
        var projects=new ArrayList<ProjectNode>();
        // Preserve legacy exact whole-field tag membership. Do not expand into tag token matching.
        for(Node record:nodeService.getNodesByTags(template.getName())) {
            if(!Objects.equals(record.getTags(),template.getName()))continue;
            var matches=new LinkedHashMap<String,List<DataNode>>();
            for(var occurrence:children.findByFatherIdOrderBySequenceAsc(record.getNodeId())) {
                Node node=nodeService.findDisplayNode(occurrence.getNodeId());
                if(node==null || !template.getDataLabels().containsKey(node.getName()))continue;
                var data=new DataNode(node,template.getDataLabels().get(node.getName()));
                String raw=Objects.toString(node.getTxt(), "");
                String plain=textContent.text(node.getNodeId(),node.getSyntax(),raw);
                String html="custom-colors".equals(node.getSyntax()) && !raw.isBlank()?richText.render(node, tenantView):
                    com.turkerozturk.markdown.NodeMarkup.supports(node.getSyntax())?com.turkerozturk.markdown.NodeMarkup.render(node.getSyntax(),raw):DataNode.plainHtml(raw);
                data.prepare(occurrence.getNodeId(),html,plain);
                matches.computeIfAbsent(node.getName(),ignored->new ArrayList<>()).add(data);
            }
            var values=new LinkedHashMap<String,DataNode>();var duplicates=new LinkedHashMap<String,List<DataNode>>();
            for(String key:template.getDataLabels().keySet()){
                var found=matches.getOrDefault(key,List.of());
                if(found.size()==1)values.put(key,found.get(0));
                else if(found.size()>1)duplicates.put(key,List.copyOf(found));
            }
            var project=new ProjectNode(record,nodeService.doBreadcrumbsToString(nodeService.addBreadcrumbs(record.getNodeId())));
            project.setDataLabels(template.getDataLabels());project.setDataNodes(values);project.setDuplicateDataNodes(duplicates);
            project.setMissingDataLabels(findMissingLabels(template.getDataLabels().keySet(),matches));projects.add(project);
        }
        projects.sort(Comparator.comparing(ProjectNode::getName,Comparator.nullsFirst(Comparator.naturalOrder())).thenComparingLong(ProjectNode::getNodeId));
        template.setProjectNodes(projects);return template;
    }

    public static Set<String> findMissingLabels(Set<String> labels,Map<String,?> values) {
        var missing=new LinkedHashSet<>(labels);missing.removeAll(values.keySet());return missing;
    }
}

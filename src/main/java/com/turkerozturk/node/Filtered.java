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

import com.turkerozturk.helpers.DateTimeHelper;
import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.helpers.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;

import static com.turkerozturk.node.NodeController.DEFAULT_PAGE_SIZE;

//https://spring.io/guides/gs/validating-form-input
@Service
public class Filtered {

    private NodeService nodeService;

    //---------------------------------------
    private Integer pageNumber;
    private Integer pageSize;
    private String oldPageSizeAsString;
    private Long nodeIcons;
    private String tagsFilter;
    private boolean nameStartsWith;
    private String nameFilter;
    private String textFilter;
    private String tsCreationStartFilter;
    private String tsCreationEndFilter;
    private String tsModificationStartFilter;
    private String tsModificationEndFilter;
    private String nodeIdStartFilter;
    private String nodeIdEndFilter;
    private String syntaxFilter;

    // --------------------------------------------------

    private Integer oldPageSize;
    private int[] pages;
    private NodeIcon[] sortedIcons;

    Page<Node> nodes;


    @Autowired
    public Filtered(NodeService nodeService) {
        this.nodeService = nodeService;
    }


    public void process(Integer pageNumber,
                        Integer pageSize,
                        String oldPageSizeAsString,
                        Long nodeIcons,
                        String tagsFilter,
                        boolean nameStartsWith,
                        String nameFilter,
                        String textFilter,
                        String tsCreationStartFilter,
                        String tsCreationEndFilter,
                        String tsModificationStartFilter,
                        String tsModificationEndFilter,
                        String nodeIdStartFilter,
                        String nodeIdEndFilter,
                        String syntaxFilter) {



        // NODE ID

        Long nodeIdStart = 1L;
        if (nodeIdStartFilter != null && !nodeIdStartFilter.isEmpty() && StringHelper.isPositiveInteger(nodeIdStartFilter)) {
            nodeIdStart = Long.parseLong(nodeIdStartFilter);
        }

        Long nodeIdEnd = Long.MAX_VALUE;
        if (nodeIdEndFilter != null && !nodeIdEndFilter.isEmpty() && StringHelper.isPositiveInteger(nodeIdEndFilter)) {
            nodeIdEnd = Long.parseLong(nodeIdEndFilter);
        }

        if (nodeIdStart > nodeIdEnd) {
            nodeIdEnd = nodeIdStart;
        }

        this.nodeIdStartFilter = nodeIdStartFilter;
        this.nodeIdEndFilter = nodeIdEndFilter;

        // PAGE NUMBER
        this.pageNumber = pageNumber;

        // PAGE SIZE
        this.pageSize = pageSize;

        oldPageSize = null;
        if (pageSize == null) {
            this.pageSize = DEFAULT_PAGE_SIZE;
            oldPageSize = DEFAULT_PAGE_SIZE;
        }

        if (pageNumber == null) {
            this.pageNumber = 0;
        }


        if (oldPageSizeAsString != null && pageSize != Integer.parseInt(oldPageSizeAsString)) {
            this.pageNumber = 0;
            this.oldPageSize = pageSize;
        }

        // NAME

        String escNameFilter = "";
        if (nameFilter != null && !nameFilter.isEmpty()) {
            if (nameStartsWith) {
            } else {
            }
            escNameFilter = nameFilter;
        }
        this.nameFilter = escNameFilter;
        // TAGS

        String escTagsFilter = "";
        if (tagsFilter != null && !tagsFilter.isEmpty()) {
            escTagsFilter = tagsFilter;
        }
        this.tagsFilter = escTagsFilter;

        // TEXT

        String escTextFilter = "";
        if (textFilter != null && !textFilter.isEmpty()) {
            escTextFilter = textFilter;
        }
        this.textFilter = escTextFilter;

        // SYNTAX

        String escSyntaxFilter = "";
        if (syntaxFilter != null && !syntaxFilter.isEmpty()) {
            escSyntaxFilter = syntaxFilter;
        }
        this.syntaxFilter = escSyntaxFilter;

        // DEFAULT DATE TIME

        String nowAsString = DateTimeHelper.formatDate(LocalDate.now(), "yyyy-MM-dd");
        String oneWeekAgoAsString = DateTimeHelper.formatDate(LocalDate.now().minusDays(7), "yyyy-MM-dd");

        if (tsCreationStartFilter == null || tsCreationStartFilter.isEmpty()) {
            this.tsCreationStartFilter = "1970-01-01";
        }
        if (tsCreationEndFilter == null || tsCreationEndFilter.isEmpty()) {
            this.tsCreationEndFilter = nowAsString;//"2038-01-19";
        }
        long tsCreationStart = DateTimeHelper.convertToUnixTimestampAtDayBegin(this.tsCreationStartFilter + " 00:00:00");
        long tsCreationEnd = DateTimeHelper.convertToUnixTimestampAtDayEnd(this.tsCreationEndFilter + " 23:59:59");

        if (tsModificationStartFilter == null || tsModificationStartFilter.isEmpty()) {
            this.tsModificationStartFilter = oneWeekAgoAsString;
        }
        if (tsModificationEndFilter == null || tsModificationEndFilter.isEmpty()) {
            this.tsModificationEndFilter = nowAsString;//"2038-01-19";
        }
        long tsModificationStart = DateTimeHelper.convertToUnixTimestampAtDayBegin(this.tsModificationStartFilter + " 00:00:00");
        long tsModificationEnd = DateTimeHelper.convertToUnixTimestampAtDayEnd(this.tsModificationEndFilter + " 23:59:59");

        Pageable pageable = PageRequest.of(this.pageNumber, this.pageSize, Sort.by("ts_lastsave").descending());

        if (nodeIcons == null) {
            //nodeIcons = 0L;
            nodes = nodeService.getNodesWithCustomQuery(escNameFilter, escTextFilter, escTagsFilter,
                    tsCreationStart, tsCreationEnd,
                    tsModificationStart, tsModificationEnd,
                    nodeIdStart, nodeIdEnd,
                    escSyntaxFilter,
                    pageable);
        } else {
            nodes = nodeService.getNodesWithCustomQuery(nodeIcons, escNameFilter, escTextFilter, escTagsFilter,
                    tsCreationStart, tsCreationEnd,
                    tsModificationStart, tsModificationEnd,
                    nodeIdStart, nodeIdEnd,
                    escSyntaxFilter,
                    pageable);
        }


        this.pages = new int[nodes.getTotalPages()];
        for (int i = 0; i < pages.length; i++) {
            pages[i] = i;
        }


        /* ARADA SIRADA AKLIN ASAGIDAKI METODA GIDIYORSA GITMESIN DIYE COMMENTLI OLARAK BIRAKIYORUM.
        Set<NodeIcon> nodeIcons = new HashSet<>();
        for(Node node : nodes) {
            nodeIcons.add(node.getNodeIcon());
        }
        model.addAttribute("nodeIcons", nodeIcons);
        */

        //     Collections.sort(projectNodes, Comparator.comparing(ProjectNode::getName));

        sortedIcons = NodeIcon.values();
        Arrays.sort(sortedIcons, Comparator.comparing(NodeIcon::getIconNameForHuman));

        this.nodeIcons = nodeIcons;

        //this.tsCreationStartFilter = tsCreationStartFilter;
        //this.tsCreationEndFilter = tsCreationEndFilter;
       // this.tsModificationStartFilter = tsModificationStartFilter;
       // this.tsModificationEndFilter = tsModificationEndFilter;


        System.out.println("page:      " + this.pageNumber);
        System.out.println("page:      " + this.pageSize);


    }




    // getters setters


    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getOldPageSizeAsString() {
        return oldPageSizeAsString;
    }

    public void setOldPageSizeAsString(String oldPageSizeAsString) {
        this.oldPageSizeAsString = oldPageSizeAsString;
    }

    public Long getNodeIcons() {
        return nodeIcons;
    }

    public void setNodeIcons(Long nodeIcons) {
        this.nodeIcons = nodeIcons;
    }

    public String getTagsFilter() {
        return tagsFilter;
    }

    public void setTagsFilter(String tagsFilter) {
        this.tagsFilter = tagsFilter;
    }

    public boolean isNameStartsWith() {
        return nameStartsWith;
    }

    public void setNameStartsWith(boolean nameStartsWith) {
        this.nameStartsWith = nameStartsWith;
    }

    public String getNameFilter() {
        return nameFilter;
    }

    public void setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
    }

    public String getTextFilter() {
        return textFilter;
    }

    public void setTextFilter(String textFilter) {
        this.textFilter = textFilter;
    }

    public String getTsCreationStartFilter() {
        return tsCreationStartFilter;
    }

    public void setTsCreationStartFilter(String tsCreationStartFilter) {
        this.tsCreationStartFilter = tsCreationStartFilter;
    }

    public String getTsCreationEndFilter() {
        return tsCreationEndFilter;
    }

    public void setTsCreationEndFilter(String tsCreationEndFilter) {
        this.tsCreationEndFilter = tsCreationEndFilter;
    }

    public String getTsModificationStartFilter() {
        return tsModificationStartFilter;
    }

    public void setTsModificationStartFilter(String tsModificationStartFilter) {
        this.tsModificationStartFilter = tsModificationStartFilter;
    }

    public String getTsModificationEndFilter() {
        return tsModificationEndFilter;
    }

    public void setTsModificationEndFilter(String tsModificationEndFilter) {
        this.tsModificationEndFilter = tsModificationEndFilter;
    }

    public String getNodeIdStartFilter() {
        return nodeIdStartFilter;
    }

    public void setNodeIdStartFilter(String nodeIdStartFilter) {
        this.nodeIdStartFilter = nodeIdStartFilter;
    }

    public String getNodeIdEndFilter() {
        return nodeIdEndFilter;
    }

    public void setNodeIdEndFilter(String nodeIdEndFilter) {
        this.nodeIdEndFilter = nodeIdEndFilter;
    }

    public String getSyntaxFilter() {
        return syntaxFilter;
    }

    public void setSyntaxFilter(String syntaxFilter) {
        this.syntaxFilter = syntaxFilter;
    }

    //--------------------------------


    public NodeService getNodeService() {
        return nodeService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public Page<Node> getNodes() {
        return nodes;
    }

    public void setNodes(Page<Node> nodes) {
        this.nodes = nodes;
    }

    public Integer getOldPageSize() {
        return oldPageSize;
    }

    public void setOldPageSize(Integer oldPageSize) {
        this.oldPageSize = oldPageSize;
    }

    public int[] getPages() {
        return pages;
    }

    public void setPages(int[] pages) {
        this.pages = pages;
    }

    public NodeIcon[] getSortedIcons() {
        return sortedIcons;
    }

    public void setSortedIcons(NodeIcon[] sortedIcons) {
        this.sortedIcons = sortedIcons;
    }


    @Override
    public String toString() {
        return "Filtered{" +
                "nodeService=" + nodeService +
                ", pageNumber=" + pageNumber +
                ", pageSize=" + pageSize +
                ", oldPageSizeAsString='" + oldPageSizeAsString + '\'' +
                ", nodeIcons=" + nodeIcons +
                ", tagsFilter='" + tagsFilter + '\'' +
                ", nameStartsWith=" + nameStartsWith +
                ", nameFilter='" + nameFilter + '\'' +
                ", textFilter='" + textFilter + '\'' +
                ", tsCreationStartFilter='" + tsCreationStartFilter + '\'' +
                ", tsCreationEndFilter='" + tsCreationEndFilter + '\'' +
                ", tsModificationStartFilter='" + tsModificationStartFilter + '\'' +
                ", tsModificationEndFilter='" + tsModificationEndFilter + '\'' +
                ", nodeIdStartFilter='" + nodeIdStartFilter + '\'' +
                ", nodeIdEndFilter='" + nodeIdEndFilter + '\'' +
                ", syntaxFilter='" + syntaxFilter + '\'' +
                ", oldPageSize=" + oldPageSize +
                ", pages=" + Arrays.toString(pages) +
                //   ", sortedIcons=" + Arrays.toString(sortedIcons) +
                ", nodes=" + nodes +
                '}';
    }
}

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
package com.turkerozturk.node.filter;

import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.node.Node;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.*;

import static com.turkerozturk.node.NodeController.DEFAULT_PAGE_SIZE;

/*
 // Formats output date when this DTO is passed through JSON
    @JsonFormat(pattern = "dd/MM/yyyy")

// Allows dd/MM/yyyy date to be passed into GET request in JSON
    @DateTimeFormat(pattern = "dd/MM/yyyy")
 */

//https://spring.io/guides/gs/validating-form-input




/**
 *  asagidaki class ismi ustu custom validation annotationlarini ben olusturdum. Degiskenin degil sinifin uzerinde cunku
 *  ikiser degiskeni ilgilendiren validasyonlar.
 *  Her biri basinda @ isareti olan bir interface ve onla baglantili bir class dan olusuyor. Birbirlerinin isimleri
 *  kodlarinda geciyor. class ustu annotation ise ElementType.TYPE, degisken ustu ise ElementType.FIELD oluyor.
 *  Zaman zaman Kendi yazdigim validasyon dosyalarina bakarak kendime hatirlatma yapmak iyi olur.
 */


@ValidNodeIdRange() // buraya da message = "" eklenebiliyor parantez icine, ama calisiyor mu anlayamadim.
@ValidCreationDateRange()
@ValidModificationDateRange
public class FormSearch {

    private final int[] PAGE_SIZE_OPTIONS = {1,5, 15, 10,30,50,100,500,1000,10000,100000};

    public static final int DEFAULT_PAGE_SIZE = 15;

    Page<Node> nodes;

    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer pageNumber;

    public Integer getPageNumber() {
        if (pageNumber == null) {
            this.pageNumber = 0;
        }
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }



    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer pageSize;



    /*
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private String oldPageSizeAsString;

    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer oldPageSize;
    private int[] pages;
    */

    //-----------

    private LinkedHashSet<NodeIcon> allIcons;

    public LinkedHashSet<NodeIcon> getAllIcons() {
        NodeIcon[] sortedIcons = NodeIcon.values();
        Arrays.sort(sortedIcons, Comparator.comparing(NodeIcon::getIconNameForHuman));
        LinkedHashSet<NodeIcon> nodeIcons = new LinkedHashSet<>();
        for(NodeIcon nodeIcon : sortedIcons) {
            nodeIcons.add(nodeIcon);
        }
        return nodeIcons;
    }

    public void setAllIcons(LinkedHashSet<NodeIcon> allIcons) {
        this.allIcons = allIcons;
    }

    @ValidNodeIcons
    private LinkedHashSet<NodeIcon> nodeIcons;

    public LinkedHashSet<NodeIcon> getNodeIcons() {
        return nodeIcons;
    }
    public void setNodeIcons(LinkedHashSet<NodeIcon> nodeIcons) {
        this.nodeIcons = nodeIcons;
    }


    private static final long MIN_UNIX_EPOCH = -62167219200L; // Minimum Unix epoch (0001-01-01)

    @Positive
    private Long nodeIdStartFilter;
    @Positive
    private Long nodeIdEndFilter;


    private String tagsFilter;


    private final static String YYYY_MM_DD = "yyyy-MM-dd";
    @PastOrPresent
    @DateTimeFormat(pattern = YYYY_MM_DD)
    private Date tsCreationStartFilter;

    @PastOrPresent
    @DateTimeFormat(pattern = YYYY_MM_DD)
    private Date tsCreationEndFilter;

    @PastOrPresent
    @DateTimeFormat(pattern = YYYY_MM_DD)
    private Date tsModificationStartFilter;

    @PastOrPresent
    @DateTimeFormat(pattern = YYYY_MM_DD)
    private Date tsModificationEndFilter;

    private String nameFilter;
    private boolean nameStartsWith;
    private String textFilter;
    private String syntaxFilter;

    // --------------------------------------------------






    // getters setters


    public Long getNodeIdStartFilter() {
        if(nodeIdStartFilter == null) {
            nodeIdStartFilter = 1L;
        }
        return nodeIdStartFilter;
    }

    public void setNodeIdStartFilter(Long nodeIdStartFilter) {
        this.nodeIdStartFilter = nodeIdStartFilter;
    }

    public Long getNodeIdEndFilter() {
        if(nodeIdEndFilter == null) {
            nodeIdEndFilter = Long.MAX_VALUE;
        }
        return nodeIdEndFilter;
    }

    public void setNodeIdEndFilter(Long nodeIdEndFilter) {
        this.nodeIdEndFilter = nodeIdEndFilter;
    }



    public Integer getPageSize() {
        if (pageSize == null) {
            this.pageSize = DEFAULT_PAGE_SIZE;
        }
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
/*
    public String getOldPageSizeAsString() {
        return oldPageSizeAsString;
    }

    public void setOldPageSizeAsString(String oldPageSizeAsString) {
        this.oldPageSizeAsString = oldPageSizeAsString;
    }
*/


    public String getTagsFilter() {
        if (tagsFilter == null) {
            tagsFilter = "";
        }
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
        if (nameFilter == null) {
            nameFilter = "";
        }
        return nameFilter;
    }

    public void setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
    }

    public String getTextFilter() {
        if (textFilter == null) {
            textFilter = "";
        }
        return textFilter;
    }

    public void setTextFilter(String textFilter) {
        this.textFilter = textFilter;
    }

    public Date getTsCreationStartFilter() {
        if (tsCreationStartFilter == null) {
            tsCreationStartFilter = new Date(MIN_UNIX_EPOCH * 1000);
        }
        return tsCreationStartFilter;
    }

    public void setTsCreationStartFilter(Date tsCreationStartFilter) {
        this.tsCreationStartFilter = tsCreationStartFilter;
    }

    public Date getTsCreationEndFilter() {
        if (tsCreationEndFilter == null) {
            tsCreationEndFilter = java.sql.Date.valueOf(LocalDate.now());
        }
        return tsCreationEndFilter;
    }

    public void setTsCreationEndFilter(Date tsCreationEndFilter) {
        this.tsCreationEndFilter = tsCreationEndFilter;
    }

    public Date getTsModificationStartFilter() {
        /*if (tsModificationStartFilter == null) {
            tsModificationStartFilter = java.sql.Date.valueOf(LocalDate.now().minusDays(7));
        }*/
        if (tsModificationStartFilter == null) {
            tsModificationStartFilter = new Date(MIN_UNIX_EPOCH * 1000);
        }
        return tsModificationStartFilter;
    }

    public void setTsModificationStartFilter(Date tsModificationStartFilter) {
        this.tsModificationStartFilter = tsModificationStartFilter;
    }

    public Date getTsModificationEndFilter() {
        if (tsModificationEndFilter == null) {
            tsModificationEndFilter = java.sql.Date.valueOf(LocalDate.now());
        }
        return tsModificationEndFilter;
    }

    public void setTsModificationEndFilter(Date tsModificationEndFilter) {
        this.tsModificationEndFilter = tsModificationEndFilter;
    }

    public String getSyntaxFilter() {
        if (syntaxFilter == null) {
            syntaxFilter = "";
        }
        return syntaxFilter;
    }

    public void setSyntaxFilter(String syntaxFilter) {
        this.syntaxFilter = syntaxFilter;
    }
/*
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

    public Page<Node> getNodes() {
        return nodes;
    }

    public void setNodes(Page<Node> nodes) {
        this.nodes = nodes;
    }
*/
    public int[] getPAGE_SIZE_OPTIONS() {
        return PAGE_SIZE_OPTIONS;
    }




}

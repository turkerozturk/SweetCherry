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
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.*;

/** POST search state. Empty ranges impose no bound, including future calendar entries. */
@ValidNodeIdRange @ValidCreationDateRange @ValidModificationDateRange
public class FormSearch {
 public static final int DEFAULT_PAGE_SIZE=25;
 private final int[] PAGE_SIZE_OPTIONS={1,5,10,15,25,30,50,100,250,500,1000,10000,100000};
 @Min(0) @Max(1000000) private Integer pageNumber=0;
 @Min(1) @Max(100000) private Integer pageSize=DEFAULT_PAGE_SIZE;
 @ValidNodeIcons private LinkedHashSet<NodeIcon> nodeIcons;
 @Positive private Long nodeIdStartFilter,nodeIdEndFilter;
 @DateTimeFormat(pattern="yyyy-MM-dd") private Date tsCreationStartFilter,tsCreationEndFilter,tsModificationStartFilter,tsModificationEndFilter;
 @Size(max=2000) private String query="",nameFilter="",textFilter="";
 @Size(max=2000) private String tagsFilter="";
 @Size(max=100) private String syntaxFilter="";
 private boolean nameStartsWith;
 @NotNull @Pattern(regexp="all|any|phrase") private String matchMode="all";
 @NotNull @Pattern(regexp="all|any|contains") private String tagMode="all";
 @NotNull @Pattern(regexp="id|name|parent|created|modified|tags|syntax") private String sortBy="modified";
 @NotNull @Pattern(regexp="asc|desc") private String sortDirection="desc";
 public int[] getPAGE_SIZE_OPTIONS(){return PAGE_SIZE_OPTIONS;}
 public LinkedHashSet<NodeIcon> getAllIcons(){var icons=new ArrayList<>(List.of(NodeIcon.values()));icons.sort(Comparator.comparing((NodeIcon icon)->icon!=NodeIcon.OTHER).thenComparing(NodeIcon::getIconNameForHuman,String.CASE_INSENSITIVE_ORDER));return new LinkedHashSet<>(icons);}
 public Integer getPageNumber(){return pageNumber==null?0:pageNumber;}
 public void setPageNumber(Integer value){pageNumber=value;}
 public Integer getPageSize(){return pageSize==null?DEFAULT_PAGE_SIZE:pageSize;}
 public void setPageSize(Integer value){pageSize=value;}
 public LinkedHashSet<NodeIcon> getNodeIcons(){return nodeIcons;}
 public void setNodeIcons(LinkedHashSet<NodeIcon> value){nodeIcons=value;}
 public Long getNodeIdStartFilter(){return nodeIdStartFilter;}
 public void setNodeIdStartFilter(Long value){nodeIdStartFilter=value;}
 public Long getNodeIdEndFilter(){return nodeIdEndFilter;}
 public void setNodeIdEndFilter(Long value){nodeIdEndFilter=value;}
 public Date getTsCreationStartFilter(){return tsCreationStartFilter;}
 public void setTsCreationStartFilter(Date value){tsCreationStartFilter=value;}
 public Date getTsCreationEndFilter(){return tsCreationEndFilter;}
 public void setTsCreationEndFilter(Date value){tsCreationEndFilter=value;}
 public Date getTsModificationStartFilter(){return tsModificationStartFilter;}
 public void setTsModificationStartFilter(Date value){tsModificationStartFilter=value;}
 public Date getTsModificationEndFilter(){return tsModificationEndFilter;}
 public void setTsModificationEndFilter(Date value){tsModificationEndFilter=value;}
 public String getQuery(){return Objects.requireNonNullElse(query,"");}
 public void setQuery(String value){query=value;}
 public String getNameFilter(){return Objects.requireNonNullElse(nameFilter,"");}
 public void setNameFilter(String value){nameFilter=value;}
 public String getTextFilter(){return Objects.requireNonNullElse(textFilter,"");}
 public void setTextFilter(String value){textFilter=value;}
 public String getTagsFilter(){return Objects.requireNonNullElse(tagsFilter,"");}
 public void setTagsFilter(String value){tagsFilter=value;}
 public String getSyntaxFilter(){return Objects.requireNonNullElse(syntaxFilter,"");}
 public void setSyntaxFilter(String value){syntaxFilter=value;}
 public String getMatchMode(){return matchMode;}
 public void setMatchMode(String value){matchMode=value;}
 public String getTagMode(){return tagMode;}
 public void setTagMode(String value){tagMode=value;}
 public String getSortBy(){return sortBy;}
 public void setSortBy(String value){sortBy=value;}
 public String getSortDirection(){return sortDirection;}
 public void setSortDirection(String value){sortDirection=value;}
 public boolean isNameStartsWith(){return nameStartsWith;}
 public void setNameStartsWith(boolean value){nameStartsWith=value;}
}

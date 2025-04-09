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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByNodeIdIn(List<Long> nodeIds);

    List<Node> findByTagsContaining(String filter);

    //@Query("SELECT n FROM Node n WHERE n.name = :name")
    //List<Node> findByName(@Param("name") String name);

    List<Node> findByName(String name);
    List<Node> findByNameOrderByNodeIdAsc(String name);

    List<Node> findByTags(String tags);

    @Query("SELECT n FROM Node n ORDER BY n.creationTimestamp DESC")
    List<Node> findRecentlyCreatedNodes(Pageable pageable);

    @Query("SELECT n FROM Node n ORDER BY n.lastSaveTimestamp DESC")
    List<Node> findRecentlyModifiedNodes(Pageable pageable);

    @Query(value = "SELECT * FROM node n WHERE n.name LIKE %:name% AND n.txt LIKE %:txt% AND n.tags LIKE %:tags% ;", nativeQuery = true)
    List<Node> findByCustomQuery(@Param("name") String name, @Param("txt") String txt, @Param("tags") String tags);

    @Query(value = "SELECT * FROM node n WHERE n.name LIKE %:name% AND n.txt LIKE %:txt% AND n.tags LIKE %:tags% ;", nativeQuery = true)
    Page<Node> findByCustomQuery(@Param("name") String name, @Param("txt") String txt, @Param("tags") String tags, Pageable pageable);

    @Query(value = "SELECT * FROM node n WHERE n.name LIKE %:name% AND n.txt LIKE %:txt% AND n.tags LIKE %:tags% " +
            "AND n.ts_creation BETWEEN :tsCreationStart AND :tsCreationEnd " +
            "AND n.ts_lastsave BETWEEN :tsModificationStart AND :tsModificationEnd "
            // "ORDER BY ?#{#pageable}"
            , nativeQuery = true)
    Page<Node> findByCustomQuery(@Param("name") String name,
                                 @Param("txt") String txt,
                                 @Param("tags") String tags,
                                 @Param("tsCreationStart") Long tsCreationStart,
                                 @Param("tsCreationEnd") Long tsCreationEnd,
                                 @Param("tsModificationStart") Long tsModificationStart,
                                 @Param("tsModificationEnd") Long tsModificationEnd,
                                 Pageable pageable);

    @Query(value = "SELECT * FROM node n WHERE n.name LIKE %:name% AND n.txt LIKE %:txt% AND n.tags LIKE %:tags% " +
            "AND n.ts_creation BETWEEN :tsCreationStart AND :tsCreationEnd " +
            "AND n.ts_lastsave BETWEEN :tsModificationStart AND :tsModificationEnd " +
            "AND n.is_ro IN ( :nodeIcons ) " +
            "ORDER BY ?#{#pageable}"
            , nativeQuery = true)
    Page<Node> findByCustomQuery(@Param("nodeIcons") Long nodeIcons,
                                 @Param("name") String name,
                                 @Param("txt") String txt,
                                 @Param("tags") String tags,
                                 @Param("tsCreationStart") Long tsCreationStart,
                                 @Param("tsCreationEnd") Long tsCreationEnd,
                                 @Param("tsModificationStart") Long tsModificationStart,
                                 @Param("tsModificationEnd") Long tsModificationEnd,
                                 Pageable pageable);

    @Query(value = "SELECT * FROM node n WHERE n.name LIKE %:name% AND n.txt LIKE %:txt% AND n.tags LIKE %:tags% " +
            "AND n.ts_creation BETWEEN :tsCreationStart AND :tsCreationEnd " +
            "AND n.ts_lastsave BETWEEN :tsModificationStart AND :tsModificationEnd " +
            "AND n.node_id BETWEEN :nodeIdStart AND :nodeIdEnd "
            // "ORDER BY ?#{#pageable}"
            , nativeQuery = true)
    Page<Node> findByCustomQuery(@Param("name") String name,
                                 @Param("txt") String txt,
                                 @Param("tags") String tags,
                                 @Param("tsCreationStart") Long tsCreationStart,
                                 @Param("tsCreationEnd") Long tsCreationEnd,
                                 @Param("tsModificationStart") Long tsModificationStart,
                                 @Param("tsModificationEnd") Long tsModificationEnd,
                                 @Param("nodeIdStart") Long nodeIdStart,
                                 @Param("nodeIdEnd") Long nodeIdEnd,
                                 Pageable pageable);

    @Query(value = "SELECT * FROM node n WHERE n.name LIKE %:name% AND n.txt LIKE %:txt% AND n.tags LIKE %:tags% " +
            "AND n.ts_creation BETWEEN :tsCreationStart AND :tsCreationEnd " +
            "AND n.ts_lastsave BETWEEN :tsModificationStart AND :tsModificationEnd " +
            "AND n.node_id BETWEEN :nodeIdStart AND :nodeIdEnd " +
            "AND n.is_ro IN ( :nodeIcons ) " +
            "ORDER BY ?#{#pageable}"
            , nativeQuery = true)
    Page<Node> findByCustomQuery(@Param("nodeIcons") Long nodeIcons,
                                 @Param("name") String name,
                                 @Param("txt") String txt,
                                 @Param("tags") String tags,
                                 @Param("tsCreationStart") Long tsCreationStart,
                                 @Param("tsCreationEnd") Long tsCreationEnd,
                                 @Param("tsModificationStart") Long tsModificationStart,
                                 @Param("tsModificationEnd") Long tsModificationEnd,
                                 @Param("nodeIdStart") Long nodeIdStart,
                                 @Param("nodeIdEnd") Long nodeIdEnd,
                                 Pageable pageable);

    // ASAGISI

    @Query(value = "SELECT * FROM node n WHERE n.name LIKE %:name% " +
            "AND n.txt LIKE %:txt% " +
            "AND n.tags LIKE %:tags% " +
            "AND n.ts_creation BETWEEN :tsCreationStart AND :tsCreationEnd " +
            "AND n.ts_lastsave BETWEEN :tsModificationStart AND :tsModificationEnd " +
            "AND n.node_id BETWEEN :nodeIdStart AND :nodeIdEnd " +
            "AND n.syntax LIKE %:syntax% "
            // "ORDER BY ?#{#pageable}"
            , nativeQuery = true)
    Page<Node> findByCustomQuery(@Param("name") String name,
                                 @Param("txt") String txt,
                                 @Param("tags") String tags,
                                 @Param("tsCreationStart") Long tsCreationStart,
                                 @Param("tsCreationEnd") Long tsCreationEnd,
                                 @Param("tsModificationStart") Long tsModificationStart,
                                 @Param("tsModificationEnd") Long tsModificationEnd,
                                 @Param("nodeIdStart") Long nodeIdStart,
                                 @Param("nodeIdEnd") Long nodeIdEnd,
                                 @Param("syntax") String syntax,
                                 Pageable pageable);

    @Query(value = "SELECT * FROM node n WHERE n.name LIKE %:name% " +
            "AND n.txt LIKE %:txt% " +
            "AND n.tags LIKE %:tags% " +
            "AND n.ts_creation BETWEEN :tsCreationStart AND :tsCreationEnd " +
            "AND n.ts_lastsave BETWEEN :tsModificationStart AND :tsModificationEnd " +
            "AND n.node_id BETWEEN :nodeIdStart AND :nodeIdEnd " +
            "AND n.is_ro IN ( :nodeIcons ) " +
            "AND n.syntax LIKE %:syntax% " +
            "ORDER BY ?#{#pageable}"
            , nativeQuery = true)
    Page<Node> findByCustomQuery(@Param("nodeIcons") Long nodeIcons,
                                 @Param("name") String name,
                                 @Param("txt") String txt,
                                 @Param("tags") String tags,
                                 @Param("tsCreationStart") Long tsCreationStart,
                                 @Param("tsCreationEnd") Long tsCreationEnd,
                                 @Param("tsModificationStart") Long tsModificationStart,
                                 @Param("tsModificationEnd") Long tsModificationEnd,
                                 @Param("nodeIdStart") Long nodeIdStart,
                                 @Param("nodeIdEnd") Long nodeIdEnd,
                                 @Param("syntax") String syntax,
                                 Pageable pageable);

    @Query(value = "SELECT * FROM node n WHERE n.name LIKE %:name% " +
            "AND n.txt LIKE %:txt% " +
            "AND n.tags LIKE %:tags% " +
            "AND n.ts_creation BETWEEN :tsCreationStart AND :tsCreationEnd " +
            "AND n.ts_lastsave BETWEEN :tsModificationStart AND :tsModificationEnd " +
            "AND n.node_id BETWEEN :nodeIdStart AND :nodeIdEnd " +
            "AND n.is_ro IN :nodeIcons " +
            "AND n.syntax LIKE %:syntax% " +
            "ORDER BY ?#{#pageable}"
            , nativeQuery = true)
    Page<Node> findByCustomQuery(@Param("nodeIcons") List<Integer> nodeIcons,
                                 @Param("name") String name,
                                 @Param("txt") String txt,
                                 @Param("tags") String tags,
                                 @Param("tsCreationStart") Long tsCreationStart,
                                 @Param("tsCreationEnd") Long tsCreationEnd,
                                 @Param("tsModificationStart") Long tsModificationStart,
                                 @Param("tsModificationEnd") Long tsModificationEnd,
                                 @Param("nodeIdStart") Long nodeIdStart,
                                 @Param("nodeIdEnd") Long nodeIdEnd,
                                 @Param("syntax") String syntax,
                                 Pageable pageable);

    void deleteByNodeId(Long nodeId);

    Node findById(long nodeId);

}

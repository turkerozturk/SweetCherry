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
package com.turkerozturk.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findAllByNodeId(Long nodeId);

    @Query("SELECT i FROM Image i WHERE i.nodeId = :nodeId AND i.png IS NOT NULL AND i.fileName = ''")
    List<Image> findByNodeIdAndPngIsNotNullAndFileNameIsEmpty(Long nodeId);
    @Query("SELECT i FROM Image i WHERE i.nodeId = :nodeId AND i.fileName IS NOT NULL AND i.fileName <> ''")
    List<Image> findAllByNonEmptyFileName(Long nodeId);

    @Query("SELECT i FROM Image i WHERE i.fileName IS NOT NULL AND i.fileName <> '' ORDER BY time DESC")
    List<Image> findAllByNonEmptyFileName();

    Optional<Image> findByNodeIdAndOffset(Long nodeId, Integer offset);


    // boyle olmaz cunku hakiki idsi veya combined idsi yok o yuzden. onun yerine deleteAll kullandim hazirmis zaten.
    //void deleteByNodeId(Long nodeId);

    // bunu da kullanabilirdim chatgpt onerdi denemedim cunku zaten deleteAll kullandim ve buna gerek kalmadi:
    @Modifying
    @Query("DELETE FROM Image i WHERE i.nodeId = :nodeId")
    void deleteByNodeId(Long nodeId);


}

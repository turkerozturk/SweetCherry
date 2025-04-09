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
package com.turkerozturk.codebox;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CombinedId implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(CombinedId.class);


    @Column(name = "node_id")
    private Integer nodeId;

    @Column(name = "\"offset\"")  // Escape the offset column name for postgresql
    private Integer offset;

    // equals ve hashCode metodlarını eklemek önemlidir
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CombinedId that = (CombinedId) o;
        return Objects.equals(nodeId, that.nodeId) && Objects.equals(offset, that.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, offset);
    }

    // Getter ve Setter metodları
    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }
}

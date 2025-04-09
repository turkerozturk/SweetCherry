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
package com.turkerozturk.grid;


import com.turkerozturk.codebox.CombinedId;
import com.turkerozturk.node.Node;
import jakarta.persistence.*;

@Entity
@Table(name = "grid")
public class Grid{

    @EmbeddedId
    private CombinedId id;

    @Column(name = "justification")
    private String justification;

    @Column(name = "txt")
    private String txt;

    @Column(name = "col_min")
    private Integer colMin;

    @Column(name = "col_max")
    private Integer colMax;

    @ManyToOne
    @JoinColumn(name = "node_id", insertable = false, updatable = false)
    private Node node;

    // Getter ve setter metotları


    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getTxt() {
        return txt;
    }

    public void setTxt(String txt) {
        this.txt = txt;
    }

    public Integer getColMin() {
        return colMin;
    }

    public void setColMin(Integer colMin) {
        this.colMin = colMin;
    }

    public Integer getColMax() {
        return colMax;
    }

    public void setColMax(Integer colMax) {
        this.colMax = colMax;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public CombinedId getId() {
        return id;
    }

    public void setId(CombinedId id) {
        this.id = id;
    }
}

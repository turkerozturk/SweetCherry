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
package com.turkerozturk.children;

import com.turkerozturk.node.Node;
import jakarta.persistence.*;

@Entity
@Table(name = "children")
public class Children {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    private long nodeId;
    @Column(name = "father_id")
    private long fatherId;
    @Column(name = "master_id")
    private Long masterId;
    private long sequence;

    @Transient
    @OneToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", referencedColumnName = "node_id", nullable = true)
    private Node node;


    public long getNodeId() {
        return nodeId;
    }

    public Children setNodeId(long nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public long getFatherId() {
        return fatherId;
    }

    public Children setFatherId(long fatherId) {
        this.fatherId = fatherId;
        return this;
    }
    public Long getMasterId() {
        return masterId;
    }

    public void setMasterId(Long masterId) {
        this.masterId = masterId;
    }

    public long getSequence() {
        return sequence;
    }

    public Children setSequence(long sequence) {
        this.sequence = sequence;
        return this;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}

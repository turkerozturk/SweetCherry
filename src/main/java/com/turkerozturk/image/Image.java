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

import com.turkerozturk.helpers.StringHelper;
import com.turkerozturk.node.Node;
import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@IdClass(Image.class)     // <--this is the extra annotation to add
@Table(name = "image")
public class Image implements Serializable {

    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    private Long nodeId;

    @Id
    @Column(name = "offset")
    private Integer offset;

    @Column(name = "justification")
    private String justification;

    @Column(name = "anchor")
    private String anchor;

    //@Lob
    @Column(name = "png", columnDefinition="BLOB")
    private byte[] png;

    @Column(name = "filename")
    private String fileName;

    @Column(name = "link")
    private String link;

    @Column(name = "time")
    private Long time;

    @ManyToOne
    @JoinColumn(name = "node_id", referencedColumnName = "node_id", insertable = false, updatable = false)
    private Node node;

    @Transient
    private Integer fileSize;

    @Transient
    private String fileExtension;

    @PostLoad
    private void doAfterInitialization() {
        fileSize = png.length;
        fileExtension = StringHelper.getFileExtension(fileName);
    }

    // Getter ve setter metotları
    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getAnchor() {
        return anchor;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public byte[] getPng() {
        return png;
    }

    public void setPng(byte[] png) {
        this.png = png;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String filename) {
        this.fileName = filename;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}

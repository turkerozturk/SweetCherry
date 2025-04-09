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

import com.turkerozturk.IconIdAndIsReadOnly;
import com.turkerozturk.bookmark.Bookmark;
import com.turkerozturk.children.Children;
import com.turkerozturk.codebox.CodeBox;
import com.turkerozturk.grid.Grid;
import com.turkerozturk.helpers.*;
import com.turkerozturk.image.Image;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Set;

@Entity
@Table(name = "node")
public class Node {

    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    private long nodeId;
    private String name;
    private String txt;
    private String syntax;
    private String tags;
    @Column(name = "is_ro")
    private long isReadOnly16bit;
    @Column(name = "is_richtxt")
    private long isRichText;
    @Column(name = "has_codebox")
    private boolean hasCodeBox;
    @Column(name = "has_table")
    private boolean hasTable;
    @Column(name = "has_image")
    private boolean hasImage;
    private long level;
    @Column(name = "ts_creation")
    private long creationTimestamp;
    @Column(name = "ts_lastsave")
    private long lastSaveTimestamp;

    //@Transient
    @OneToOne
    @JoinColumn(name = "node_id", referencedColumnName = "node_id")
    private Bookmark bookmark;

    @OneToOne
    @JoinColumn(name = "node_id", referencedColumnName = "master_id")
    private Children children;

    @OneToOne
    @JoinColumn(name = "node_id", referencedColumnName = "node_id")
    private Children nodeIdInChildrenTable;



    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY)
    private Set<CodeBox> codeBoxes;

    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY)
    private Set<Grid> grids;

    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY)
    private Set<Image> images;

    @Transient
    private String txtAsHtml;

    @Transient
    private String txtAsTransformed;

    @Transient
    private LinkedHashMap<Long, String> breadcrumbs;

    @Transient
    private NodeIcon nodeIcon;

    @Transient
    private boolean isReadOnly;

    @Transient
    private long titleColor;

    @Transient
    private boolean boldnessBit;

    @Transient
    private boolean isRichTextBit;

    @Transient
    private String titleColorAsHtmlHex;
    //AndBoldnessAndTextType

    @Transient
    private boolean hasChildren;

    /**
     * Shared node ile ilgili.
     */
    @Transient
    private boolean isMasterNode;

    @Transient
    private Node fatherNode;

    /**
     * dikkat: eger txtAsHtml nin parsellenmesi isteniyorsa diger boolean constructur true olarak cagrilmali veya bu
     * constructor ile node olusturulduktan sonra processTxtToHtml metodu cagrilmalidir.
     */
    public Node() {
    }

    /**
     * Su anda bu metodu kullanmak icin bir sebep yok cunku repositoryden veya servisten boyle gelmiyor.
     * Fakat ikinci bir islem gerektigi gozden kacmasin vurgulansin diye burada bulunmasini uygun gordum.
     * @param runProcessTxtToHtml
     */
    public Node(boolean runProcessTxtToHtml) {
        if(runProcessTxtToHtml) {
            processTxtToHtml();
        }
    }

    /**
     * Node metni her zaman gerekli olmayabilir. Sistem kaynaklarini bosa harcamamak icin bu metod sadece gerektiginde
     * cagrilarak metin icerigi parselleyecek sekilde hazirlandi. Yani ya bos constructor ile node olusturduktan sonra
     * bu metod cagrilmali, veya node boolean constructor true olarak olusturulursa kendiliginden calisacak ve
     * node nin txt icerigini parselleyip txtAsHtml degiskenine atacaktir. Bir hata durumunda veya kosullar
     * gerceklesmediginde node nin txt icerigi ile txtAsHtml yi birebir ayni yapacaktir.
     */
    public void processTxtToHtml()  {
            this.txtAsHtml = this.txt; // TODO
    }

    /**
     * PostLoad in yaptigi is, entity verileri aldiktan sonra, kendiliginden icine yazdigimiz metodlari cagirmasi.
     * Niye kullandik cunku bazi verilerin veritabanindan geldikten sonra islenmesi ve degiskenlere ayri ayri atanmasi
     * gerekiyor.
     *
     * Kullanmadigimiz metod var mi var, kaynaklari tuketmemesi icin ornegin icerigi htmlye donusturen metodu bunda
     * degil, sonradan gerektiginde calisitiriyoruz.
     */
    @PostLoad
    private void doAfterInitialization() {
        parseNodeIconAndReadOnlyFlag();
        parseNodeTitleColorAndBoldnessAndTextType();
        parseIsMasterNode();

    }





    public void parseNodeIconAndReadOnlyFlag() {
        int dbField16bits = (int) this.getIsReadOnly16bit();

        IconIdAndIsReadOnly iconIdAndIsReadOnly = BitOperation.processSixteenBitData(dbField16bits);
        this.setReadOnly(iconIdAndIsReadOnly.isReadOnly());

        // ustteki helper metod ile hem readonly degiskenden ayirdik hem de donusum yaptik raw icon id ye.
        int iconIdIn16bit = iconIdAndIsReadOnly.iconId();
        if (iconIdIn16bit != 0) {
            // hesaplanmis icon id ile NodeIcon u elde ediyoruz:
            // Sifir veya null olanlar var, sebebi CherryTree programinin zaman icerisinde birkac metodla iconlari
            // farkli yerlerden elde etmesi.
            NodeIcon nodeIcon = NodeIcon.getNodeIconByIconId(iconIdIn16bit);
            // TODO asagidaki system out printler hersey cozuldukten sonra kaldirilacak.
            if (nodeIcon != null) {
                //System.out.println("dbField16bits: " + dbField16bits + "\tnodeIcon: " + nodeIcon.toString());
                this.setNodeIcon(nodeIcon);
            } else {
                //System.out.println("nodeIcon is null");
                this.setNodeIcon(NodeIcon.OTHER);
            }
        } else {
            // 0 means we need to search in COdeIcon too.
            String syntax = this.getSyntax();
            if(!syntax.equals("plain-text") && !syntax.equals("custom-colors")) {
                NodeIcon nodeIcon = CodeIcon.getCodeIconBySyntax(syntax).getNodeIcon();

                this.setNodeIcon(nodeIcon);
            } else {
                //System.out.println("nodeIcon is null");
                this.setNodeIcon(NodeIcon.OTHER);
            }
        }
        //System.out.println("-------------------------------------------------------------------------------------");
    }

    private void parseIsMasterNode() {

        Long masterId = this.getChildren().getMasterId();
        if(masterId == null || masterId == 0) {
            this.setMasterNode(true);
        } else {
            logger.info(this.getNodeId() + " is a shared node. Its MASTER is = " + masterId + ", name: " + this.getName());
            this.setMasterNode(false);
        }



    }

    public void parseNodeTitleColorAndBoldnessAndTextType() {

        this.titleColor = (isRichText >> 2) & 0xFFFFFF; // Foreground rengini al
        this.titleColorAsHtmlHex = HtmlColorUtil.longToHtmlHex(this.titleColor);
        this.boldnessBit = ((isRichText >> 1) & 0x01) == 1; // Bold bilgisini al
        this.isRichTextBit = (isRichText & 0x01) == 1; // Rich text bilgisini al
        /*
        System.out.println("isRichText: " + isRichText);
        System.out.println("titleColor: " + titleColor);
        System.out.println("titleColorAsHtmlHex: " + titleColorAsHtmlHex);
        System.out.println("boldnessBit: " + boldnessBit);
        System.out.println("isRichTextBit: " + isRichTextBit);
        */
    }

    public String getTxtAsHtml() {
        return txtAsHtml;
    }

    public void setTxtAsHtml(String txtAsHtml) {
        this.txtAsHtml = txtAsHtml;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTxt() {
        return txt;
    }

    public void setTxt(String txt) {
        this.txt = txt;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public long getIsReadOnly16bit() {
        return isReadOnly16bit;
    }

    public void setIsReadOnly16bit(long isReadOnly16bit) {
        this.isReadOnly16bit = isReadOnly16bit;
    }

    public long getIsRichText() {
        return isRichText;
    }

    public void setIsRichText(long isRichText) {
        this.isRichText = isRichText;
    }

    public boolean getHasCodeBox() {
        return hasCodeBox;
    }

    public void setHasCodeBox(boolean hasCodeBox) {
        this.hasCodeBox = hasCodeBox;
    }

    public boolean getHasTable() {
        return hasTable;
    }

    public void setHasTable(boolean hasTable) {
        this.hasTable = hasTable;
    }

    public boolean getHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    public long getLevel() {
        return level;
    }

    public void setLevel(long level) {
        this.level = level;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public long getLastSaveTimestamp() {
        return lastSaveTimestamp;
    }

    public void setLastSaveTimestamp(long lastSaveTimestamp) {
        this.lastSaveTimestamp = lastSaveTimestamp;
    }

    public LinkedHashMap<Long, String> getBreadcrumbs() {
        return breadcrumbs;
    }

    public void setBreadcrumbs(LinkedHashMap<Long, String> breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }

    public NodeIcon getNodeIcon() {
        return nodeIcon;
    }

    public void setNodeIcon(NodeIcon nodeIcon) {
        this.nodeIcon = nodeIcon;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly(boolean readOnly) {
        isReadOnly = readOnly;
    }

    public String getTxtAsTransformed() {
        return txtAsTransformed;
    }

    public void setTxtAsTransformed(String txtAsTransformed) {
        this.txtAsTransformed = txtAsTransformed;
    }

    public long getTitleColor() {
        return titleColor;
    }

    public void setTitleColor(long titleColor) {
        this.titleColor = titleColor;
    }

    public boolean isBoldnessBit() {
        return boldnessBit;
    }

    public void setBoldnessBit(boolean boldnessBit) {
        this.boldnessBit = boldnessBit;
    }

    public boolean isRichTextBit() {
        return isRichTextBit;
    }

    public void setRichTextBit(boolean richTextBit) {
        isRichTextBit = richTextBit;
    }

    public String getTitleColorAsHtmlHex() {
        return titleColorAsHtmlHex;
    }

    public void setTitleColorAsHtmlHex(String titleColorAsHtmlHex) {
        this.titleColorAsHtmlHex = titleColorAsHtmlHex;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }


    public boolean isMasterNode() {
        return isMasterNode;
    }

    public void setMasterNode(boolean masterNode) {
        isMasterNode = masterNode;
    }

    public Children getChildren() {
        return children;
    }

    public void setChildren(Children children) {
        this.children = children;
    }


    public Set<Grid> getGrids() {
        return grids;
    }

    public void setGrids(Set<Grid> grids) {
        this.grids = grids;
    }

    public Set<Image> getImages() {
        return images;
    }

    public void setImages(Set<Image> images) {
        this.images = images;
    }


    public Node getFatherNode() {
        return fatherNode;
    }

    public void setFatherNode(Node fatherNode) {
        this.fatherNode = fatherNode;
    }

    public Children getNodeIdInChildrenTable() {
        return nodeIdInChildrenTable;
    }

    public void setNodeIdInChildrenTable(Children nodeIdInChildrenTable) {
        this.nodeIdInChildrenTable = nodeIdInChildrenTable;
    }

    public Bookmark getBookmark() {
        return bookmark;
    }

    public void setBookmark(Bookmark bookmark) {
        this.bookmark = bookmark;
    }

    public boolean isHasCodeBox() {
        return hasCodeBox;
    }

    public Set<CodeBox> getCodeBoxes() {
        return codeBoxes;
    }

    public void setCodeBoxes(Set<CodeBox> codeBoxes) {
        this.codeBoxes = codeBoxes;
    }
}

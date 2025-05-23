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
package com.turkerozturk.export;

import com.turkerozturk.bookmark.Bookmark;
import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.codebox.CodeBox;
import com.turkerozturk.grid.Grid;
import com.turkerozturk.helpers.ApplicationUtils;
import com.turkerozturk.helpers.TargetFilePathHelper;
import com.turkerozturk.image.Image;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class ExportManipulatedRecursiveController {

    private static final Logger logger = LoggerFactory.getLogger(ExportManipulatedRecursiveController.class);

    @Autowired
    NodeService nodeService;

    @Autowired
    ChildrenService childrenService;

    private long newRootNodeId;

    private boolean isMainNode = true;



    private void createTablesIfNotExist(Connection conn) throws IOException, SQLException {

        Statement stmt = conn.createStatement();

        // bilgi hem jar hem de jar degilken schema dosyasini problemsiz bulmasi icin logic ve kod:
        String schema = null;
        if (ApplicationUtils.isRunningFromJar()) {
            logger.info("Running from JAR");
            schema = loadFileContent("schema-sqlite.sql");
        } else {
            logger.info("Running from IDE or exploded JAR");
            String dbSchemaFilePath = TargetFilePathHelper
                    .getTargetPathWirhoutStatic("schemas" + File.separator + "schema-sqlite.sql");
            logger.info("SCHEMA " + dbSchemaFilePath);
            schema = readFileToString(dbSchemaFilePath);
        }

        stmt.executeUpdate(schema);
    }

    private long setNewRootId(Connection conn) throws SQLException {


        // Benzersiz node_id belirleme
        String getNodeIdQuery = "SELECT MAX(node_id) FROM children";
        long newRootNodeId = 1;
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(getNodeIdQuery);
        // try (Statement stmt = conn.createStatement();
        //      ResultSet rs = stmt.executeQuery(getNodeIdQuery)) {
        if (rs.next()) {
            int maxNodeId = rs.getInt(1);
            newRootNodeId = maxNodeId + 1;
            logger.info(String.format("old: %s, new: %s", maxNodeId, newRootNodeId));

        }
        //  }

        return newRootNodeId;

    }

    long newRootSrquence;

    private long setNewRootSrquence(Connection conn) throws SQLException {


        // Benzersiz sequence belirlemek, eger father_id 0 ise. father_id yi de biz 0 yapiyoruz zaten,
        // her baska node tree ekledigimizde koke yerlessin diye.
        String getNodeIdQuery = "SELECT MAX(sequence) FROM children WHERE father_id = 0";

        long newRootSequence = 1;
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(getNodeIdQuery);
        // try (Statement stmt = conn.createStatement();
        //      ResultSet rs = stmt.executeQuery(getNodeIdQuery)) {
        if (rs.next()) {
            int maxNodeId = rs.getInt(1);
            newRootSequence = maxNodeId + 1;
            logger.info(String.format("old sequence: %s, new sequence : %s", maxNodeId, newRootSequence));

        }
        //  }

        return newRootSequence;

    }

    public long getNewRootSrquence() {
        return newRootSrquence;
    }




    private void insertOneNodeRelatedData(int nodeId, Connection conn, boolean isRegularNode) throws SQLException {

       // long newNodeId = getNewNodeId(nodeId);
     //   long newSequenceId = newNodeId;

        Children children;

        if(isRegularNode) {
            Node node = nodeService.getById(nodeId);
            children = node.getChildren();//childrenService.findById(nodeId);
            Bookmark bookmark = node.getBookmark();
            Set<Grid> grids = node.getGrids();
            Set<CodeBox> codeBoxes = node.getCodeBoxes();
            Set<Image> images = node.getImages();

            // Insert Node data
            String nodeInsert = "INSERT INTO node (node_id, name, txt, syntax, tags, " +
                    "is_ro, is_richtxt, has_codebox, has_table, has_image, " +
                    "level, ts_creation, ts_lastsave) VALUES " +
                    "(:node_id, :name, :txt, :syntax, :tags, " +
                    ":is_ro, :is_richtxt, :has_codebox, :has_table, :has_image, " +
                    ":level, :ts_creation, :ts_lastsave)";
            try (PreparedStatement pstmt = conn.prepareStatement(nodeInsert)) {
                //pstmt.setLong(1, node.getNodeId());
                pstmt.setLong(1, newRootNodeId + nodeId);
                pstmt.setString(2, node.getName());
                pstmt.setString(3, node.getTxt());
                pstmt.setString(4, node.getSyntax());
                pstmt.setString(5, node.getTags());
                pstmt.setLong(6, node.getIsReadOnly16bit());
                pstmt.setLong(7, node.getIsRichText());
                pstmt.setBoolean(8, node.getHasCodeBox());
                pstmt.setBoolean(9, node.getHasTable());
                pstmt.setBoolean(10, node.getHasImage());
                pstmt.setLong(11, node.getLevel());
                pstmt.setLong(12, node.getCreationTimestamp());
                pstmt.setLong(13, node.getLastSaveTimestamp());
                pstmt.executeUpdate();
            }



            // Insert Bookmark data
            if (bookmark != null) {
                String bookmarkInsert = "INSERT INTO bookmark (node_id, sequence) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(bookmarkInsert)) {
                    //pstmt.setLong(1, bookmark.getNodeId());
                    pstmt.setLong(1, newRootNodeId + nodeId);
                    pstmt.setLong(2, bookmark.getSequence());
                    //pstmt.setLong(2, newSequenceId);
                    pstmt.executeUpdate();
                }
            }

            // Insert Grid data
            if (grids != null) {
                String gridInsert = "INSERT INTO grid (node_id, offset, justification, txt, col_min, col_max) VALUES (?, ?, ?, ?, ?, ?)";
                for (Grid grid : grids) {
                    try (PreparedStatement pstmt = conn.prepareStatement(gridInsert)) {
                        //pstmt.setLong(1, grid.getId().getNodeId());
                        pstmt.setLong(1, newRootNodeId + nodeId);
                        pstmt.setInt(2, grid.getId().getOffset());
                        pstmt.setString(3, grid.getJustification());
                        pstmt.setString(4, grid.getTxt());
                        pstmt.setInt(5, grid.getColMin());
                        pstmt.setInt(6, grid.getColMax());
                        pstmt.executeUpdate();
                    }
                }
            }

            // Insert CodeBox data
            if (codeBoxes != null) {
                String codeBoxInsert = "INSERT INTO codebox (node_id, offset, justification, txt, syntax, width, height, is_width_pix, do_highl_bra, do_show_linenum) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                for (CodeBox codeBox : codeBoxes) {
                    try (PreparedStatement pstmt = conn.prepareStatement(codeBoxInsert)) {
                        //pstmt.setLong(1, codeBox.getId().getNodeId());
                        pstmt.setLong(1, newRootNodeId + nodeId);
                        pstmt.setInt(2, codeBox.getId().getOffset());
                        pstmt.setString(3, codeBox.getJustification());
                        pstmt.setString(4, codeBox.getTxt());
                        pstmt.setString(5, codeBox.getSyntax());
                        pstmt.setInt(6, codeBox.getWidth());
                        pstmt.setInt(7, codeBox.getHeight());
                        pstmt.setInt(8, codeBox.getIsWidthPix());
                        pstmt.setInt(9, codeBox.getDoHighlBra());
                        pstmt.setInt(10, codeBox.getDoShowLinenum());
                        pstmt.executeUpdate();
                    }
                }
            }

            // Insert Image data
            if (images != null) {
                String imageInsert = "INSERT INTO image (node_id, offset, justification, anchor, png, filename, link, time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                for (Image image : images) {
                    try (PreparedStatement pstmt = conn.prepareStatement(imageInsert)) {
                        //pstmt.setLong(1, image.getNodeId());
                        pstmt.setLong(1, newRootNodeId + nodeId);
                        pstmt.setInt(2, image.getOffset());
                        pstmt.setString(3, image.getJustification());
                        pstmt.setString(4, image.getAnchor());
                        pstmt.setBytes(5, image.getPng());
                        pstmt.setString(6, image.getFileName());
                        pstmt.setString(7, image.getLink());
                        pstmt.setLong(8, image.getTime());
                        pstmt.executeUpdate();
                    }
                }
            }


        } else {
            // Shared Node oldugundan id ve verisi sadece children tablosunda tek satir kayit olarak var.
            children = childrenService.findById(nodeId);

        }





        // Insert Children data
        String childrenInsert = "INSERT INTO children (node_id, father_id, sequence, master_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(childrenInsert)) {
            //pstmt.setLong(1, children.getNodeId());
            pstmt.setLong(1, newRootNodeId + nodeId);
            //pstmt.setLong(2, newRootNodeId + children.getFatherId());
            if(!isMainNode) {
                pstmt.setLong(2, newRootNodeId + children.getFatherId());
                pstmt.setLong(3, children.getSequence());
                if(isRegularNode) {
                    pstmt.setLong(4, children.getMasterId()); // 0 or null
                } else {
                    pstmt.setLong(4, newRootNodeId + children.getMasterId());
                }

            } else {
                // it is main node
                pstmt.setLong(2, 0);

                pstmt.setLong(3, setNewRootSrquence(conn));

                pstmt.setLong(4, 0); // 0 or null


                isMainNode = false;
            }
            logger.info(String.format("%s, %s, %s", isMainNode, nodeId, newRootNodeId));
            //pstmt.setLong(2, 0);
            //pstmt.setLong(3, children.getSequence());
            //pstmt.setLong(3, newSequenceId);



            //pstmt.setLong(4, newRootNodeId + children.getMasterId());
            pstmt.executeUpdate();
        }






    }


    //https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-sql.html
    @GetMapping("/expo/{node_id}")
    public String export1(@PathVariable("node_id") Integer nodeId, Model model) {

        logger.info("export contoroller " + nodeId);


        // bilgi basla
        String dbFilePath = "exportedFiles" + File.separator + "exportednodes.ctb";
        if (Files.exists(Path.of(dbFilePath))) {
            logger.info("db file exits: " + dbFilePath);
        } else {
            logger.info("db file not exits: " + dbFilePath);
        }

        String sqliteUrl = "jdbc:sqlite:" + dbFilePath;
        try (Connection conn = DriverManager.getConnection(sqliteUrl)) {
            if (conn != null) {


                createTablesIfNotExist(conn);

                newRootNodeId = setNewRootId(conn);

                setMainNode(true); // bilgi "it is necessary" to set it.

                List<Children> childrens = childrenService.findAllSubChildren(nodeId);
                for (Children children : childrens) {
                    boolean isRegulardNode = children.getMasterId() == null || children.getMasterId() == 0;
                    if(isRegulardNode) {
                        insertOneNodeRelatedData((int) children.getNodeId(), conn, true);
                    } else {
                        insertOneNodeRelatedData((int) children.getNodeId(), conn, false);
                    }


                }









                model.addAttribute("contentText", "Export successful. Exported node count: " + childrens.size());
            }
        } catch (SQLException e) {
            logger.error("Error during export", e);
            model.addAttribute("contentText", "Export failed.");
        } catch (IOException e) {
            logger.error("Error during export", e);
            model.addAttribute("contentText", "Export failed.");
            throw new RuntimeException(e);
        }


        // bilgi bitti

        //model.addAttribute("contentText", "export başarılı.");

        return "expo";
    }




    public static String readFileToString(String filePath) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public String loadFileContent(String filePath) throws IOException {
        Resource resource = new ClassPathResource(filePath);
        logger.info("is resource present: " + resource.exists());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    public boolean isMainNode() {
        return isMainNode;
    }

    public void setMainNode(boolean mainNode) {
        isMainNode = mainNode;
    }
}

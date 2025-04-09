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
package com.turkerozturk.xlsx;

import com.turkerozturk.node.DataNode;
import com.turkerozturk.node.ProjectNode;
import com.turkerozturk.node.TemplateNode;
import com.turkerozturk.node.TemplateService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
public class XlsxController {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private MessageSource messageSource;

    public static final int DATA_START_COLUMN = 1 + 1;

    @GetMapping("/cherrytemplatenode/xlsx/{templateNodeId}")
    public ResponseEntity<byte[]> downloadXlsx(@PathVariable long templateNodeId, Locale locale) throws IOException {
        TemplateNode templateNode = templateService.getTemplateNode(templateNodeId);

        // Create a new Workbook and Sheet
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Template Data");

        int rowCounter = 0;
        int columnHeaderCounter = DATA_START_COLUMN;
        Row headerRow = sheet.createRow(rowCounter);

        Cell nodeIdCellLabel = headerRow.createCell(0);
        String translationOfNodeIdLabel = messageSource.getMessage("node.nodeId", null, locale);
        nodeIdCellLabel.setCellValue(translationOfNodeIdLabel);

        Cell nodeNameCellLabel = headerRow.createCell(1);
        String translationOfNodeNameLabel = messageSource.getMessage("node.name", null, locale);
        nodeNameCellLabel.setCellValue(translationOfNodeNameLabel);

        for (String key : templateNode.getDataLabels().keySet()) {

            Cell headerCell = headerRow.createCell(columnHeaderCounter);
            headerCell.setCellValue(key);
            columnHeaderCounter++;
        }


        List<ProjectNode> projectNodes = templateNode.getProjectNodes();
        for (ProjectNode projectNode : projectNodes) {
            rowCounter++;
            Row dataRow = sheet.createRow(rowCounter);

            Map<String, DataNode> dataNodesMap = projectNode.getDataNodes();

            Cell porjectNodeIdCell = dataRow.createCell(0); // see DATA_START_COLUMN
            porjectNodeIdCell.setCellValue(projectNode.getNodeId());

            Cell porjectNodeNameCell = dataRow.createCell(1); // see DATA_START_COLUMN
            porjectNodeNameCell.setCellValue(projectNode.getName());



            int columnDataCounter = DATA_START_COLUMN;
            for (String key : templateNode.getDataLabels().keySet()) {

                Cell dataCell = dataRow.createCell(columnDataCounter);

                DataNode dataNode = dataNodesMap.get(key);
                String value;
                if (dataNode != null) {
                    if (!dataNode.getSyntax().equals("custom-colors")) {
                        value = dataNode.getData();
                    } else {
                        try {
                            String xmlContent = dataNode.getData();
                            ByteArrayInputStream input = new ByteArrayInputStream(xmlContent.getBytes("UTF-8"));
                            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                            Document doc = null;
                            doc = dBuilder.parse(input);
                            doc.getDocumentElement().normalize();
                            value = getTextContent(doc.getDocumentElement());

                        } catch (SAXException e) {
                            throw new RuntimeException(e);
                        } catch (ParserConfigurationException e) {
                            throw new RuntimeException(e);
                        }




                    }
                } else {
                    value = null;
                }

                dataCell.setCellValue(value);
                columnDataCounter++;
            }

        }


        // Write the workbook to a ByteArrayOutputStream
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        byte[] xlsxBytes = bos.toByteArray();
        workbook.close();

        // Prepare headers for the response
        String targetXlsxFileName = templateNode.getName() + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", targetXlsxFileName);
        headers.setContentLength(xlsxBytes.length);

        // Return the ResponseEntity with the XLSX data
        return new ResponseEntity<>(xlsxBytes, headers, HttpStatus.OK);
    }

    public static String getTextContent(Node node) {
        StringBuilder sb = new StringBuilder();
        NodeList childNodes = node.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                sb.append(child.getNodeValue().trim());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                sb.append(getTextContent(child));
            }
        }
        return sb.toString().trim();
    }


}

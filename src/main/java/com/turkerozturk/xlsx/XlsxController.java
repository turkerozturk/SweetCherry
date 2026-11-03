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

    public static final int DATA_START_COLUMN = 2;

    @com.turkerozturk.multipledatabases.RequiresTenant
    @GetMapping("/cherrytemplatenode/xlsx/{templateNodeId}")
    public ResponseEntity<byte[]> downloadXlsx(@PathVariable long templateNodeId, Locale locale) throws IOException {
        var template=templateService.getTemplateNode(templateNodeId);
        byte[] bytes=new TemplateXlsxDocument().render(template,messageSource,locale);
        var headers=new HttpHeaders();headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename(template.getName()+".xlsx",java.nio.charset.StandardCharsets.UTF_8).build());
        headers.setContentLength(bytes.length);headers.setCacheControl("no-store");return new ResponseEntity<>(bytes,headers,HttpStatus.OK);
    }
}

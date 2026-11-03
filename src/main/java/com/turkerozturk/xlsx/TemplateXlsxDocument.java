package com.turkerozturk.xlsx;

import com.turkerozturk.node.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.context.MessageSource;
import java.util.Locale;
import java.io.*;

/** Typed string cells preserve IDs and never interpret note content as spreadsheet formulas. */
public final class TemplateXlsxDocument {
    public byte[] render(TemplateNode template,MessageSource messages,Locale locale)throws IOException {
        if(template.getDataLabels().size()>16382 || template.getProjectNodes().size()>1048575)throw new IllegalArgumentException("Too many report rows/columns");
        try(var book=new XSSFWorkbook();var output=new ByteArrayOutputStream()) {
            var sheet=book.createSheet("Data");var header=sheet.createRow(0);
            var bold=book.createFont();bold.setBold(true);var heading=book.createCellStyle();heading.setFont(bold);
            var wrap=book.createCellStyle();wrap.setWrapText(true);wrap.setVerticalAlignment(VerticalAlignment.TOP);
            var warning=book.createCellStyle();warning.cloneStyleFrom(wrap);warning.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());warning.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.createCell(0).setCellValue(messages.getMessage("node.nodeId",null,locale));header.createCell(1).setCellValue(messages.getMessage("node.name",null,locale));
            int column=2;for(var label:template.getDataLabels().entrySet())header.createCell(column++).setCellValue(label.getValue());
            for(var cell:header)cell.setCellStyle(heading);
            int number=0;for(var project:template.getProjectNodes()) {
                var row=sheet.createRow(++number);row.createCell(0).setCellValue(Long.toString(project.getNodeId()));row.createCell(1).setCellValue(project.getName());column=2;
                for(var field:project.getFields()) {
                    var cell=row.createCell(column++);cell.setCellStyle(field.status().equals("value")?wrap:warning);
                    if(field.status().equals("value"))cell.setCellValue(field.data().getPlainText());
                    else cell.setCellValue(messages.getMessage("report.state."+field.status(),null,locale)+(field.status().equals("ambiguous")?" "+field.matches().stream().map(d->"["+d.getNodeId()+"]").collect(java.util.stream.Collectors.joining(", ")): ""));
                }
            }
            sheet.createFreezePane(2,1);sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,number,0,column-1));
            sheet.setColumnWidth(0,14*256);sheet.setColumnWidth(1,30*256);for(int i=2;i<header.getLastCellNum();i++)sheet.setColumnWidth(i,40*256);
            if(!template.getDefinitionIssues().isEmpty()) {
                var issues=book.createSheet("Definitions");issues.createRow(0).createCell(0).setCellValue(messages.getMessage("report.definitions",null,locale));
                int i=0;for(String issue:template.getDefinitionIssues())issues.createRow(++i).createCell(0).setCellValue(issue);issues.setColumnWidth(0,80*256);
            }
            book.write(output);return output.toByteArray();
        }
    }
}

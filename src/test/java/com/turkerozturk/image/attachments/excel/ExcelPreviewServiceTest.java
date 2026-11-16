package com.turkerozturk.image.attachments.excel;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;

class ExcelPreviewServiceTest {
    static ExcelPreviewSettings settings() { return new ExcelPreviewSettings(5242880,16777216,2,3,20); }
    static byte[] fixture(boolean xlsx) throws Exception {
        try (Workbook wb = xlsx ? new XSSFWorkbook() : new HSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet("<Budget & plan>"); var row = sheet.createRow(0);
            row.createCell(0).setCellValue("<script>alert(1)</script>");
            row.createCell(1).setCellValue(12.5);
            var style=wb.createCellStyle(); style.setDataFormat(wb.createDataFormat().getFormat("0.00"));
            var font=wb.createFont(); font.setBold(true); font.setItalic(true); style.setFont(font); style.setAlignment(HorizontalAlignment.RIGHT);
            row.getCell(1).setCellStyle(style); row.createCell(2).setCellFormula("B1*2");
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
            row.getCell(1).setCellValue(20); // deliberately stale cached formula value must remain 25
            row.createCell(4).setCellValue("outside column cap");
            sheet.createRow(3).createCell(0).setCellValue("last row");
            sheet.getRow(3).createCell(2).setCellFormula("B1+1");
            wb.createSheet("Hidden").createRow(0).createCell(0).setCellValue(true); wb.setSheetHidden(1,true);
            wb.createSheet("Empty"); wb.write(out); return out.toByteArray();
        }
    }
    @Test void xlsAndXlsxKeepCoordinatesHiddenSheetsFormattingAndCachedFormulas() throws Exception {
        for (boolean xlsx : List.of(false,true)) {
            byte[] bytes=fixture(xlsx), before=bytes.clone(); var service=new ExcelPreviewService(settings());
            var view=service.parse(bytes,xlsx?"BUDGET.XLSX":"budget.xls","node",0,1,Locale.US);
            assertThat(view.columns()).containsExactly("A","B","C"); assertThat(view.totalRows()).isEqualTo(4);
            assertThat(view.pages()).isEqualTo(2); assertThat(view.rows()).hasSize(2);
            assertThat(view.rows().get(0).cells().get(0).text()).isEqualTo("<script>alert(1)</sc…");
            var number=view.rows().get(0).cells().get(1); assertThat(number.text()).isEqualTo("20.00");
            assertThat(number.bold()).isTrue(); assertThat(number.italic()).isTrue(); assertThat(number.alignment()).isEqualTo("right");
            var formula=view.rows().get(0).cells().get(2); assertThat(formula.text()).isEqualTo("25"); assertThat(formula.formula()).isEqualTo("=B1*2");
            assertThat(view.clippedColumns()).isTrue(); assertThat(view.clippedText()).isTrue(); assertThat(view.sheets().get(1).hidden()).isTrue();
            var page2=service.parse(bytes,"book.xls","",0,2,Locale.US); assertThat(page2.rows().get(0).number()).isEqualTo(3); assertThat(page2.rows().get(1).cells().get(0).text()).isEqualTo("last row");
            assertThat(service.parse(bytes,"book.xls","",2,1,Locale.US).totalRows()).isZero();
            if (xlsx) assertThat(page2.rows().get(1).cells().get(2).text()).isEqualTo("—");
            assertThat(bytes).isEqualTo(before);
        }
    }
    @Test void databaseReadDoesNotChangeBlobOrMetadataAndExcludesNonAttachments() throws Exception {
        try (var c=DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try(var q=c.createStatement()) {
                q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,ts_lastsave INTEGER)");
                q.execute("CREATE TABLE image(node_id INTEGER,offset INTEGER,anchor TEXT,png BLOB,filename TEXT,time INTEGER)");
                q.execute("INSERT INTO node VALUES(1,'Budget',123)");
            }
            byte[] bytes=fixture(true);
            try(var q=c.prepareStatement("INSERT INTO image VALUES(1,0,'',?,'book.xlsx',456)")){q.setBytes(1,bytes);q.executeUpdate();}
            try(var q=c.createStatement()){q.execute("PRAGMA query_only=ON");}
            var service=new ExcelPreviewService(settings()); assertThat(service.load(c,1,0,0,1,Locale.ENGLISH).nodeName()).isEqualTo("Budget");
            try(var q=c.createStatement();var rs=q.executeQuery("SELECT png,time,ts_lastsave FROM image JOIN node USING(node_id)")) {
                assertThat(rs.next()).isTrue();assertThat(rs.getBytes(1)).isEqualTo(bytes);assertThat(rs.getLong(2)).isEqualTo(456);assertThat(rs.getLong(3)).isEqualTo(123);
            }
            assertThatThrownBy(()->service.load(c,1,3,0,1,Locale.US)).hasMessage("missing");
            assertThatThrownBy(()->new ExcelPreviewService(new ExcelPreviewSettings(1024,1024,1,1,1)).load(c,1,0,0,1,Locale.US)).hasMessage("large");
        }
    }
    @Test void encryptedWorkbookIsReportedWithoutChangingIt() throws Exception {
        byte[] bytes=fixture(true);
        try(var fs=new org.apache.poi.poifs.filesystem.POIFSFileSystem();var out=new ByteArrayOutputStream()) {
            var info=new org.apache.poi.poifs.crypt.EncryptionInfo(org.apache.poi.poifs.crypt.EncryptionMode.agile);
            var encryptor=info.getEncryptor();encryptor.confirmPassword("private");
            try(var encrypted=encryptor.getDataStream(fs)){encrypted.write(bytes);}
            fs.writeFilesystem(out);
            assertThatThrownBy(()->new ExcelPreviewService(settings()).parse(out.toByteArray(),"locked.xlsx","",0,1,Locale.US)).hasMessage("encrypted");
        }
    }
    @Test void rejectsCorruptionInvalidCoordinatesUnsupportedAndExpandedZip() throws Exception {
        var service=new ExcelPreviewService(settings()); byte[] bytes=fixture(true);
        assertThatThrownBy(()->service.parse(bytes,"book.xlsm","",0,1,Locale.US)).hasMessage("unsupported");
        assertThatThrownBy(()->service.parse(new byte[100],"book.xls","",0,1,Locale.US)).hasMessage("invalid");
        assertThatThrownBy(()->service.parse(bytes,"book.xlsx","",99,1,Locale.US)).hasMessage("request");
        assertThatThrownBy(()->service.parse(bytes,"book.xlsx","",0,3,Locale.US)).hasMessage("request");
        var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){zip.putNextEntry(new ZipEntry("huge.xml"));zip.write(new byte[2048]);zip.closeEntry();}
        var tiny=new ExcelPreviewService(new ExcelPreviewSettings(1024,1024,1,1,1));
        assertThatThrownBy(()->tiny.parse(out.toByteArray(),"book.xlsx","",0,1,Locale.US)).hasMessage("large");
        assertThatThrownBy(()->new ExcelPreviewSettings(0,1024,1,1,1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(ExcelPreviewService.supports("a.XLSX")).isTrue(); assertThat(ExcelPreviewService.supports("a.xlsx.exe")).isFalse();
    }
}

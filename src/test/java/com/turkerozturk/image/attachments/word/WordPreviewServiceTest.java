package com.turkerozturk.image.attachments.word;

import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;

class WordPreviewServiceTest {
    static WordPreviewSettings settings() { return new WordPreviewSettings(5242880,16777216,200000,1000,100,20); }
    static byte[] fixture() throws Exception {
        try(var doc=new XWPFDocument();var out=new ByteArrayOutputStream()) {
            var heading=doc.createParagraph(); heading.setStyle("Heading1");heading.setAlignment(ParagraphAlignment.CENTER);
            heading.createRun().setText("<Title & heading>");
            var paragraph=doc.createParagraph();paragraph.createRun().setText("Before ");
            var run=paragraph.createRun();run.setBold(true);run.setItalic(true);run.setUnderline(UnderlinePatterns.SINGLE);run.setText("bold italic underlined");
            var link=paragraph.createHyperlinkRun("http://127.0.0.1:1/private");link.setText(" link text");
            var table=doc.createTable(2,2);table.getRow(0).getCell(0).setText("<script>literal()</script>");table.getRow(0).getCell(1).setText("Second column");
            table.getRow(1).getCell(0).setText("Second row");table.getRow(1).getCell(1).setText("Last cell");
            var last=doc.createParagraph();last.setStyle("ListBullet");last.createRun().setText("After table 😀");
            doc.write(out);return out.toByteArray();
        }
    }
    @Test void docxPreservesBodyOrderBasicFormattingAndTablesWithoutFollowingLinks() throws Exception {
        byte[] bytes=fixture(),original=bytes.clone();var view=new WordPreviewService(settings()).parse(bytes,"report.DOCX","<node>");
        assertThat(view.blocks()).extracting(WordPreviewService.Block::kind).containsExactly("heading","paragraph","table","paragraph");
        assertThat(view.blocks().get(0).alignment()).isEqualTo("center");
        assertThat(view.blocks().get(0).runs().get(0).text()).isEqualTo("<Title & heading>");
        var formatted=view.blocks().get(1).runs().get(1);assertThat(formatted.bold()).isTrue();assertThat(formatted.italic()).isTrue();assertThat(formatted.underline()).isTrue();
        assertThat(view.blocks().get(1).runs()).extracting(WordPreviewService.Run::text).contains(" link text");
        assertThat(view.blocks().get(2).rows().get(0)).containsExactly("<script>literal()</script>","Second column");
        assertThat(view.blocks().get(3).runs()).extracting(WordPreviewService.Run::text).containsExactly("• ","After table 😀");
        assertThat(view.clipped()).isFalse();assertThat(view.legacy()).isFalse();assertThat(bytes).isEqualTo(original);
    }
    @Test void legacyWordDocumentExtractsTextWithoutMutatingBytes() throws Exception {
        byte[] bytes;try(var in=getClass().getResourceAsStream("/word-preview/simple.doc")){assertThat(in).isNotNull();bytes=in.readAllBytes();}
        byte[] original=bytes.clone();var view=new WordPreviewService(settings()).parse(bytes,"legacy.doc","node");
        assertThat(view.legacy()).isTrue();assertThat(view.blocks()).isNotEmpty();
        assertThat(view.blocks().stream().flatMap(b->b.runs().stream()).map(WordPreviewService.Run::text).reduce("",String::concat)).isNotBlank();
        assertThat(bytes).isEqualTo(original);
        assertThatThrownBy(()->new WordPreviewService(settings()).parse(bytes,"renamed.docx","node")).hasMessage("invalid");
        try(var in=getClass().getResourceAsStream("/word-preview/simple-table.doc")) {
            assertThat(in).isNotNull(); var table=new WordPreviewService(settings()).parse(in.readAllBytes(),"table.doc","node");
            assertThat(table.blocks()).anySatisfy(b->{assertThat(b.kind()).isEqualTo("table");assertThat(b.rows()).isNotEmpty();});
        }
    }
    @Test void displayAndZipLimitsRejectOrTruncateWithoutBreakingUnicode() throws Exception {
        byte[] bytes=fixture();var limited=new WordPreviewService(new WordPreviewSettings(5242880,16777216,200000,1000,1,1)).parse(bytes,"a.docx","n");
        assertThat(limited.clipped()).isTrue();assertThat(limited.blocks().get(2).rows()).hasSize(1);assertThat(limited.blocks().get(2).rows().get(0)).hasSize(1);
        var capped=new WordPreviewService(new WordPreviewSettings(5242880,16777216,4,1000,10,10)).parse(bytes,"a.docx","n");
        assertThat(capped.clipped()).isTrue();assertThat(capped.blocks()).hasSize(1);assertThat(capped.blocks().get(0).runs().get(0).text()).isEqualTo("<Tit…");
        var blocks=new WordPreviewService(new WordPreviewSettings(5242880,16777216,200000,1,10,10)).parse(bytes,"a.docx","n");assertThat(blocks.blocks()).hasSize(1);assertThat(blocks.clipped()).isTrue();
        var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){zip.putNextEntry(new ZipEntry("word/document.xml"));zip.write(new byte[2048]);zip.closeEntry();}
        var tiny=new WordPreviewService(new WordPreviewSettings(1024,1024,1,1,1,1));
        assertThatThrownBy(()->tiny.parse(out.toByteArray(),"bomb.docx","n")).hasMessage("large");
        assertThatThrownBy(()->tiny.parse(bytes,"large.docx","n")).hasMessage("large");
    }
    @Test void databaseReadsAreAllowedInQueryOnlyModeAndPreserveBlobAndDates() throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try(var q=c.createStatement()){q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,ts_lastsave INTEGER)");q.execute("CREATE TABLE image(node_id INTEGER,offset INTEGER,anchor TEXT,png BLOB,filename TEXT,time INTEGER)");q.execute("INSERT INTO node VALUES(1,'<node>',123)");}
            byte[] bytes=fixture();try(var q=c.prepareStatement("INSERT INTO image VALUES(1,0,'',?,'report.docx',456)")){q.setBytes(1,bytes);q.executeUpdate();}
            try(var q=c.createStatement()){q.execute("PRAGMA query_only=ON");}
            var service=new WordPreviewService(settings());assertThat(service.load(c,1,0).nodeName()).isEqualTo("<node>");
            try(var q=c.createStatement();var rs=q.executeQuery("SELECT png,time,ts_lastsave FROM image JOIN node USING(node_id)")){assertThat(rs.next()).isTrue();assertThat(rs.getBytes(1)).isEqualTo(bytes);assertThat(rs.getInt(2)).isEqualTo(456);assertThat(rs.getInt(3)).isEqualTo(123);}
            assertThatThrownBy(()->service.load(c,1,99)).hasMessage("missing");
        }
    }
    @Test void encryptedCorruptUnsupportedAndEmptyDocumentsHavePredictableResults() throws Exception {
        var service=new WordPreviewService(settings());byte[] bytes=fixture();
        try(var fs=new org.apache.poi.poifs.filesystem.POIFSFileSystem();var out=new ByteArrayOutputStream()) {
            var info=new org.apache.poi.poifs.crypt.EncryptionInfo(org.apache.poi.poifs.crypt.EncryptionMode.agile);var encryptor=info.getEncryptor();encryptor.confirmPassword("private");
            try(var stream=encryptor.getDataStream(fs)){stream.write(bytes);}fs.writeFilesystem(out);
            assertThatThrownBy(()->service.parse(out.toByteArray(),"locked.docx","n")).hasMessage("encrypted");
        }
        assertThatThrownBy(()->service.parse(new byte[100],"bad.docx","n")).hasMessage("invalid");
        assertThatThrownBy(()->service.parse(bytes,"a.docm","n")).hasMessage("unsupported");
        try(var doc=new XWPFDocument();var out=new ByteArrayOutputStream()){doc.write(out);assertThat(service.parse(out.toByteArray(),"empty.docx","n").blocks()).isEmpty();}
        assertThat(WordPreviewService.supports("a.DOC")).isTrue();assertThat(WordPreviewService.supports("a.doc.exe")).isFalse();
        assertThatThrownBy(()->new WordPreviewSettings(0,1024,1,1,1,1)).isInstanceOf(IllegalArgumentException.class);
    }
}

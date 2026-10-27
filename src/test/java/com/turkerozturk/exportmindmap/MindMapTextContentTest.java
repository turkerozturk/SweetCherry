package com.turkerozturk.exportmindmap;

import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MindMapTextContentTest {
    @Test void combinesFormattedLinkRunsAndPreservesUnicodeLinesAndAllTargets() throws Exception {
        try(var c=database()) {
            String xml="<node><rich_text>Türkçe 😀\n</rich_text><rich_text link='webs https://example.org/?a=1&amp;b=2'>Visit </rich_text>"
                +"<rich_text link='webs https://example.org/?a=1&amp;b=2' weight='heavy'>site</rich_text>"
                +"<rich_text>\n</rich_text><rich_text link='node 11 an anchor'>Shared</rich_text>"
                +"<rich_text link='file L3RtcC9hLnR4dA=='>File</rich_text></node>";
            assertThat(MindMapTextContent.text(c,2,xml)).isEqualTo("Türkçe 😀\nVisit site (https://example.org/?a=1&b=2)\nShared (/nodes/11#an anchor)File (/tmp/a.txt)");
        }
    }
    @Test void ordersObjectsAtUnicodeBufferOffsetsWithoutReadingAnyBinaryColumn() throws Exception {
        try(var c=database();var s=c.createStatement()) {
            // Deliberately no png column: selecting an image entity/BLOB would fail this test.
            s.execute("INSERT INTO image VALUES(2,1,'','', 'webs https://image.example/picture',NULL)");
            s.execute("INSERT INTO image VALUES(2,3,'','report.pdf','',123)");
            s.execute("INSERT INTO image VALUES(2,4,'bookmark','','',NULL)");
            s.execute("INSERT INTO codebox VALUES(2,5,'if (x < 2) {\\n  x++;\\n}','java')");
            s.execute("INSERT INTO grid VALUES(2,6,'<table><row><cell>A &amp; B</cell><cell>1</cell></row><row><cell>Title</cell><cell>Value</cell></row></table>')");
            s.execute("PRAGMA query_only=ON");
            String result=MindMapTextContent.text(c,2,"<node><rich_text>😀AB</rich_text></node>");
            assertThat(result).startsWith("😀[Image (node: 2, offset: 1)] (https://image.example/picture)A[Attachment: report.pdf (node: 2, offset: 3, time: 123)]");
            assertThat(result).contains("[Anchor: bookmark", "[Code box: java]", "if (x < 2)", "[Table]\nTitle\tValue\nA & B\t1\n").endsWith("B");
        }
    }
    @Test void plainAndCodeTextNeverNeedsAnEntityManagerOrInterpretsMarkup() {
        var service=new MindMapTextContent();
        assertThat(service.text(2,"plain-text","<node>literal & text</node>\n")).isEqualTo("<node>literal & text</node>\n");
        assertThat(service.text(2,"python","print('<tag>')")).isEqualTo("print('<tag>')");
    }
    @Test void invalidXmlAndOutOfBoundsObjectsFailWithSourceIdInsteadOfSilentlyDroppingData() throws Exception {
        try(var c=database();var s=c.createStatement()) {
            assertThatThrownBy(()->MindMapTextContent.text(c,2,"<!DOCTYPE node [<!ENTITY x SYSTEM 'file:///secret'>]><node><rich_text>&x;</rich_text></node>"))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("source node 2");
            s.execute("INSERT INTO image VALUES(2,900,'','','',NULL)");
            assertThatThrownBy(()->MindMapTextContent.text(c,2,"<node><rich_text>short</rich_text></node>"))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("source node 2");
        }
    }
    private Connection database() throws Exception {
        var c=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var s=c.createStatement()) {
            s.execute("CREATE TABLE image(node_id INTEGER,offset INTEGER,anchor TEXT,filename TEXT,link TEXT,time INTEGER)");
            s.execute("CREATE TABLE codebox(node_id INTEGER,offset INTEGER,txt TEXT,syntax TEXT)");
            s.execute("CREATE TABLE grid(node_id INTEGER,offset INTEGER,txt TEXT)");
        }
        return c;
    }
}

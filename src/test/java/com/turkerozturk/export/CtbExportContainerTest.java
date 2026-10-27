package com.turkerozturk.export;

import com.turkerozturk.richtext.experimental.RichTextXmlReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CtbExportContainerTest {
    private static final String XML = "<node><rich_text link=\"node 12 anchor\" weight=\"heavy\">Inside</rich_text>"
            + "<rich_text link='node 3 missing-anchor'>Outside &amp; </rich_text>"
            + "<rich_text link='node 3 missing-anchor' style='italic'>more</rich_text>"
            + "<rich_text link='node 10'>Shared target</rich_text>"
            + "<rich_text link='node 999'>Gone &lt;target&gt;</rich_text>"
            + "<rich_text link='webs https://example.org/?a=1&amp;b=2'>Web</rich_text></node>";

    @Test void allFourModesCreateBoldRootReportWithCurrentDatesAndKeepExportedDates() throws Exception {
        for (boolean descendants : new boolean[]{false,true}) for (boolean collection : new boolean[]{false,true}) {
            try (var source=fixture(); var target=target()) {
                rich(source,2,XML);
                long before=Instant.now().getEpochSecond();
                int count=CtbExportSql.copy(source,target,11,descendants,collection,"Türkçe / Notes & Work");
                long container=number(target,"SELECT node_id FROM children WHERE father_id=0");
                assertThat(count).isEqualTo(descendants ? 7 : 1);
                assertThat(number(target,"SELECT COUNT(*) FROM children")).isEqualTo(count+1);
                assertThat(text(target,"SELECT name FROM node WHERE node_id="+container))
                        .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} from:Türkçe / Notes & Work \\[node:11\\]");
                assertThat(number(target,"SELECT is_richtxt FROM node WHERE node_id="+container)).isEqualTo(3);
                assertThat(number(target,"SELECT ts_creation FROM node WHERE node_id="+container))
                        .isBetween(before,Instant.now().getEpochSecond());
                assertThat(number(target,"SELECT ts_lastsave-ts_creation FROM node WHERE node_id="+container)).isZero();
                assertThat(number(target,"SELECT COUNT(*) FROM children WHERE father_id="+container)).isEqualTo(1);
                String report=plain(text(target,"SELECT txt FROM node WHERE node_id="+container));
                assertThat(report).contains("node | 3 | Node 3 | Outside & more",
                        "shared node | 10 | Node 1 | Shared target", "orphan | 999 | — | Gone <target>");
                assertThat(report.contains("node | 12 | Node 12 | Inside")).isEqualTo(!descendants);
                assertThat(text(source,"SELECT txt FROM node WHERE node_id=2")).isEqualTo(XML);
                assertThat(number(target,"SELECT COUNT(*) FROM node WHERE node_id<>"+container
                        +" AND (ts_creation<>100 OR ts_lastsave<>200)")).isZero();
            }
        }
    }

    @Test void mappedLinksKeepAnchorsAndFormattingButExcludedTargetsGoToContainerWithoutAnchor() throws Exception {
        try(var source=fixture();var target=target()) {
            rich(source,2,XML);
            CtbExportSql.copy(source,target,11,true,true,"Source");
            long container=number(target,"SELECT node_id FROM children WHERE father_id=0");
            var document=new RichTextXmlReader().read(text(target,"SELECT txt FROM node WHERE name='Node 2'"));
            assertThat(document.runs().get(0).attributes()).containsEntry("link","node 2 anchor").containsEntry("weight","heavy");
            assertThat(document.runs().get(2).attributes()).containsEntry("style","italic");
            for(int i=1;i<=4;i++) assertThat(document.runs().get(i).attributes()).containsEntry("link","node "+container);
            assertThat(document.runs().get(5).attributes()).containsEntry("link","webs https://example.org/?a=1&b=2");
            assertThat(document.runs().stream().map(r->r.text()).toList())
                    .isEqualTo(new RichTextXmlReader().read(XML).runs().stream().map(r->r.text()).toList());
        }
    }

    @Test void sharedOccurrenceLinksKeepOccurrenceIdentityAndOutsideMasterIsNotMistakenForIncludedAlias() throws Exception {
        try(var source=fixture();var target=target()) {
            rich(source,2,"<node><rich_text link='node 18'>Alias inside</rich_text>"
                    +"<rich_text link='node 1'>Master outside</rich_text></node>");
            CtbExportSql.copy(source,target,11,true,true,"Source");
            long container=number(target,"SELECT node_id FROM children WHERE father_id=0");
            var runs=new RichTextXmlReader().read(text(target,"SELECT txt FROM node WHERE name='Node 2'")).runs();
            assertThat(runs.get(0).attributes().get("link")).isEqualTo("node 6");
            assertThat(runs.get(1).attributes().get("link")).isEqualTo("node "+container);
            assertThat(plain(text(target,"SELECT txt FROM node WHERE node_id="+container)))
                    .contains("node | 1 | Node 1 | Master outside").doesNotContain("shared node | 18 |");
        }
    }

    @Test void linkedImagesAreRewrittenWithoutChangingObjectsOrOffsets() throws Exception {
        try(var source=fixture();var target=target();var st=source.createStatement()) {
            st.execute("INSERT INTO image VALUES (2,4,'left',NULL,X'00FF10','photo.png','node 10 anchor',123)");
            st.execute("INSERT INTO image VALUES (2,8,'right',NULL,X'1122',NULL,'node 12 anchor',124)");
            st.execute("INSERT INTO image VALUES (2,9,'right',NULL,X'22',NULL,'webs https://example.org',125)");
            CtbExportSql.copy(source,target,11,true,true,"Source");
            long container=number(target,"SELECT node_id FROM children WHERE father_id=0");
            assertThat(text(target,"SELECT link FROM image WHERE offset=4")).isEqualTo("node "+container);
            assertThat(text(target,"SELECT link FROM image WHERE offset=8")).isEqualTo("node 2 anchor");
            assertThat(text(target,"SELECT link FROM image WHERE offset=9")).isEqualTo("webs https://example.org");
            assertThat(text(target,"SELECT hex(png) FROM image WHERE offset=4")).isEqualTo("00FF10");
            assertThat(number(target,"SELECT time FROM image WHERE offset=4")).isEqualTo(123);
            assertThat(text(source,"SELECT link FROM image WHERE offset=4")).isEqualTo("node 10 anchor");
            assertThat(plain(text(target,"SELECT txt FROM node WHERE node_id="+container)))
                    .contains("shared node | 10 | Node 1 | [image object at offset 4: photo.png]");
        }
    }

    @Test void inPlaceRepeatedExportsNeverLinkToUnrelatedExistingTargetAndDoNotModifySource() throws Exception {
        try(var source=fixture()) {
            rich(source,2,XML);
            CtbExportSql.copy(source,source,11,true,true,"Same file");
            CtbExportSql.copy(source,source,11,true,true,"Same file");
            assertThat(number(source,"SELECT COUNT(*) FROM children")).isEqualTo(35);
            assertThat(text(source,"SELECT txt FROM node WHERE node_id=2")).isEqualTo(XML);
            try(var st=source.createStatement();var reports=st.executeQuery("SELECT node_id,txt FROM node WHERE name LIKE '% from:Same file %'")) {
                int count=0;
                while(reports.next()) {
                    count++;
                    assertThat(plain(reports.getString(2))).contains("node | 3 | Node 3 | Outside & more");
                    long id=reports.getLong(1);
                    long root=number(source,"SELECT node_id FROM children WHERE father_id="+id);
                    var runs=new RichTextXmlReader().read(text(source,"SELECT txt FROM node WHERE node_id="+root)).runs();
                    assertThat(runs.get(1).attributes().get("link")).isEqualTo("node "+id);
                }
                assertThat(count).isEqualTo(2);
            }
        }
    }

    @Test void plainTextAndExternalLinksRemainUnchangedAndEmptyReportStillExists() throws Exception {
        try(var source=fixture();var target=target();var st=source.createStatement()) {
            st.execute("UPDATE node SET txt='literal node 999 <rich_text link=\"node 10\">' WHERE node_id=2");
            CtbExportSql.copy(source,target,2,false,false,"Source");
            assertThat(text(target,"SELECT txt FROM node WHERE node_id=2"))
                    .isEqualTo(text(source,"SELECT txt FROM node WHERE node_id=2"));
            assertThat(plain(text(target,"SELECT txt FROM node WHERE node_id<>2")))
                    .contains("No missing or excluded internal-link targets.");
        }
    }

    @Test void invalidRichXmlFailsAndInPlaceSavepointRemovesPartialWrites() throws Exception {
        try(var source=fixture()) {
            rich(source,2,"<!DOCTYPE node [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><node><rich_text>&x;</rich_text></node>");
            assertThatThrownBy(()->CtbExportService.appendInPlace(source,11,true))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .hasMessageContaining("source node 2");
            assertThat(number(source,"SELECT COUNT(*) FROM children")).isEqualTo(19);
            assertThat(number(source,"SELECT COUNT(*) FROM node")).isEqualTo(14);
        }
    }

    @Test void oversizedNumericTargetIsReportedAsOrphanWithoutOverflow() throws Exception {
        try(var source=fixture();var target=target()) {
            rich(source,2,"<node><rich_text link='node 9999999999999999999999999'>Gone</rich_text></node>");
            CtbExportSql.copy(source,target,11,false,false,"Source");
            assertThat(plain(text(target,"SELECT txt FROM node WHERE node_id<>11")))
                    .contains("orphan | 9999999999999999999999999 | — | Gone");
        }
    }

    private static void rich(Connection source,long id,String xml)throws Exception {
        try(var st=source.prepareStatement("UPDATE node SET txt=?,syntax='custom-colors',is_richtxt=1 WHERE node_id=?")) {
            st.setString(1,xml);st.setLong(2,id);st.executeUpdate();
        }
    }
    private static Connection fixture()throws Exception {
        var c=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var in=CtbExportContainerTest.class.getResourceAsStream("/fixtures/shared-node-tree.sql");var st=c.createStatement()) {
            for(String sql:new String(in.readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$","").split(";"))
                if(!sql.isBlank())st.execute(sql);
            st.execute("UPDATE node SET ts_creation=100,ts_lastsave=200");
        }
        return c;
    }
    private static Connection target()throws Exception {
        var c=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var st=c.createStatement()){for(String sql:SqliteSchemaLoader.load().split(";"))if(!sql.isBlank())st.execute(sql);}
        return c;
    }
    private static String plain(String xml){return new RichTextXmlReader().read(xml).runs().stream().map(r->r.text()).collect(java.util.stream.Collectors.joining());}
    private static long number(Connection c,String sql)throws Exception {try(var st=c.createStatement();var rs=st.executeQuery(sql)){assertThat(rs.next()).isTrue();return rs.getLong(1);}}
    private static String text(Connection c,String sql)throws Exception {try(var st=c.createStatement();var rs=st.executeQuery(sql)){assertThat(rs.next()).isTrue();return rs.getString(1);}}
}

package com.turkerozturk.node.links;

import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
class InternalLinkReportServiceTest {
    private Connection connection;
    @BeforeEach void setup() throws Exception {
        connection=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var sql=connection.createStatement()){
            sql.execute("CREATE TABLE node(node_id INTEGER,name TEXT,is_ro INTEGER,is_richtxt INTEGER,syntax TEXT,txt TEXT)");
            sql.execute("CREATE TABLE children(node_id INTEGER,father_id INTEGER,master_id INTEGER)");
            sql.execute("CREATE TABLE image(node_id INTEGER,offset INTEGER,filename TEXT,anchor TEXT,link TEXT,png BLOB)");
            sql.execute("INSERT INTO node VALUES(1,'<Source>',0,3,'custom-colors',''),(2,'Türkçe Target',0,0,'plain-text','node 999'),(4,'Parent',0,0,'plain-text',''),(5,'Other',0,0,'plain-text','')");
            sql.execute("INSERT INTO children VALUES(1,0,0),(2,4,0),(4,0,0),(5,0,0),(10,4,1),(11,5,2)");
            sql.execute("INSERT INTO image VALUES(2,0,'','çapa & bölüm','',NULL),(1,8,'<file>.png','','node 11',X'010203')");
        }
        setText("<node><rich_text link='node 2'>go </rich_text><rich_text link='node 2' weight='heavy'>Target</rich_text><rich_text> gap </rich_text>"
                +"<rich_text link='node 11 çapa &amp; bölüm'>Anchor</rich_text><rich_text link='node 999'>lost</rich_text>"
                +"<rich_text link='node 2 missing'>no anchor</rich_text><rich_text link='node oops'>bad</rich_text><rich_text link='webs https://example.org'>external</rich_text></node>");
    }
    @AfterEach void close() throws Exception {connection.close();}
    private void setText(String text)throws Exception{try(var query=connection.prepareStatement("UPDATE node SET txt=? WHERE node_id=1")){query.setString(1,text);query.executeUpdate();}}
    private InternalLinkReportService.Report report(String q,String status,int limit,int page)throws Exception {
        return InternalLinkReportService.inspect(connection,new InternalLinkReportService.Options(q,status,limit,page));
    }
    @Test void backlinksDistinguishExactOccurrenceAndSharedContent()throws Exception {
        var exact=InternalLinkReportService.inspect(connection,new InternalLinkReportService.Options("","all",20,1,11L,false));
        assertThat(exact.rows()).hasSize(4).allMatch(row->row.targetId().equals("11"));
        var group=InternalLinkReportService.inspect(connection,new InternalLinkReportService.Options("","all",20,1,11L,true));
        assertThat(group.rows()).hasSize(8);assertThat(group.rows()).anyMatch(row->row.targetId().equals("2"));
        assertThat(InternalLinkReportService.inspect(connection,new InternalLinkReportService.Options("","all",20,1,999L,false))).isNull();
    }
    @Test void listsSharedOccurrencesAndObjectsWithExactAnchorStatusAndMergedText()throws Exception {
        var report=report("","all",50,1);
        assertThat(report.links()).isEqualTo(12);assertThat(report.sourceContents()).isEqualTo(1);assertThat(report.sourcePlaces()).isEqualTo(2);
        assertThat(report.missingNodes()).isEqualTo(2);assertThat(report.missingAnchors()).isEqualTo(2);assertThat(report.invalidLinks()).isEqualTo(2);
        assertThat(report.rows()).hasSize(12);
        assertThat(report.rows().get(0).label()).isEqualTo("go Target");
        assertThat(report.rows().get(1).source().id()).isEqualTo(10);assertThat(report.rows().get(1).source().shared()).isTrue();
        assertThat(report.rows().get(1).source().breadcrumb()).contains("Parent [4]","<Source> [10]");
        var anchor=report.rows().stream().filter(row->row.anchor().equals("çapa & bölüm")).findFirst().orElseThrow();
        assertThat(anchor.target().id()).isEqualTo(11);assertThat(anchor.target().shared()).isTrue();assertThat(anchor.status()).isEqualTo("ok");
        assertThat(anchor.target().breadcrumb()).contains("Other [5]","Türkçe Target [11]");assertThat(anchor.fragment()).contains("%C3%A7apa%20%26%20b%C3%B6l%C3%BCm");
        assertThat(report.rows().stream().filter(row->row.kind().equals("object"))).hasSize(2);
    }
    @Test void filtersAndPaginatesWithoutChangingGlobalCounts()throws Exception {
        var broken=report("","broken",3,2);assertThat(broken.matched()).isEqualTo(6);assertThat(broken.links()).isEqualTo(12);
        assertThat(broken.rows()).hasSize(3);assertThat(broken.first()).isEqualTo(4);assertThat(broken.last()).isEqualTo(6);assertThat(broken.pages()).isEqualTo(2);
        assertThat(report("11","all",50,1).matched()).isEqualTo(4);
        assertThat(report("turkce","ok",50,1).matched()).isEqualTo(6);
        assertThat(report("not found","all",50,1).rows()).isEmpty();
    }
    @Test void malformedAndOversizeXmlAreReportedButObjectLinksStillWork()throws Exception {
        setText("<!DOCTYPE node [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><node><rich_text>&x;</rich_text></node>");
        var invalid=report("","all",50,1);assertThat(invalid.unreadable()).isEqualTo(1);assertThat(invalid.issues().get(0).reason()).isEqualTo("invalidXml");
        assertThat(invalid.rows()).hasSize(2).allMatch(row->row.kind().equals("object"));
        setText("x".repeat(InternalLinkReportService.MAX_XML_CHARACTERS+1));
        assertThat(report("","all",50,1).issues().get(0).reason()).isEqualTo("tooLarge");
    }
    @Test void missingMasterOrOrphanSourceAndCyclesDoNotCrashOrCreateLinks()throws Exception {
        try(var sql=connection.createStatement()){
            sql.execute("UPDATE children SET master_id=99 WHERE node_id=11");
            sql.execute("UPDATE children SET father_id=5 WHERE node_id=4");sql.execute("UPDATE children SET father_id=4 WHERE node_id=5");
            sql.execute("DELETE FROM children WHERE node_id IN(1,10)");
        }
        var report=report("","all",50,1);
        assertThat(report.rows()).anyMatch(row->!row.source().available());
        assertThat(report.rows()).anyMatch(row->row.target()!=null&&!row.target().available()&&row.status().equals("missingNode"));
        assertThat(report.rows()).anyMatch(row->row.target()!=null&&row.target().breadcrumb().contains("↻"));
    }
    @Test void doesNotSelectPngAndLeavesStoredDataUntouched()throws Exception {
        var inspected=(Connection)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
            if((method.getName().equals("prepareStatement")) && args!=null&&args[0] instanceof String sql)assertThat(sql.toLowerCase(Locale.ROOT)).doesNotContain("png","update ","delete ","insert ");
            try{return method.invoke(connection,args);}catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause();}
        });
        InternalLinkReportService.inspect(inspected,new InternalLinkReportService.Options("","all",50,1));
        try(var query=connection.createStatement();var rows=query.executeQuery("SELECT hex(png) FROM image WHERE node_id=1")){assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("010203");}
    }
    @Test void overflowAndMalformedInternalValuesAreVisibleRatherThanLinked()throws Exception {
        setText("<node><rich_text link='node 9223372036854775808'>overflow</rich_text><rich_text link='node 0'>zero</rich_text></node>");
        var report=report("","all",50,1);assertThat(report.invalidLinks()).isEqualTo(4);
        assertThat(report.rows().stream().filter(row->row.status().equals("invalid"))).allMatch(row->row.target()==null);
    }
}

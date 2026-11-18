package com.turkerozturk.node.fragments;

import java.sql.*;
import org.junit.jupiter.api.Test;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import jakarta.persistence.EntityManager;
import com.turkerozturk.node.*;
import com.turkerozturk.richtext.experimental.RichTextRenderingService;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeFragmentServiceTest {
    static NodeFragmentSettings settings(){return new NodeFragmentSettings(3,20,250000,1048576,16777216,200);}
    static Connection database()throws Exception{
        var c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var q=c.createStatement()){
            q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,syntax TEXT,tags TEXT,txt TEXT)");
            q.execute("CREATE TABLE children(node_id INTEGER PRIMARY KEY,father_id INTEGER,master_id INTEGER)");
            q.execute("CREATE TABLE image(node_id INTEGER,png BLOB)");q.execute("CREATE TABLE codebox(node_id INTEGER,txt TEXT)");q.execute("CREATE TABLE grid(node_id INTEGER,txt TEXT)");
            q.execute("INSERT INTO node VALUES(1,'Host','custom-colors','','host'),(2,'<Source>','custom-colors','','source'),(10,'Trash','plain-text','__sweetcherry_trash_v1__','metadata'),(11,'Hidden','plain-text','','hidden'),(12,'Cycle','plain-text','','x'),(13,'Cycle parent','plain-text','','x'),(15,'Orphan','plain-text','','x')");
            q.execute("INSERT INTO children VALUES(1,0,0),(2,0,0),(3,1,2),(10,0,0),(11,10,0),(12,13,0),(13,12,0),(14,0,999)");
        }return c;
    }
    @Test void metadataResolvesSharedContentAndRejectsHiddenMissingAndCyclicPaths()throws Exception{
        try(var c=database()){
            var source=NodeFragmentSource.read(c,3,settings());assertThat(source.contentId()).isEqualTo(2);assertThat(source.name()).isEqualTo("<Source>");
            for(long id:new long[]{0,12,13,14,15,999})assertThatThrownBy(()->NodeFragmentSource.read(c,id,settings())).hasMessage("missing");
            for(long id:new long[]{10,11})assertThatThrownBy(()->NodeFragmentSource.read(c,id,settings())).hasMessage("managed");
        }
    }
    @Test void textAndObjectLimitsAreCheckedWithoutLoadingBodiesOrBlobs()throws Exception{
        try(var c=database()){
            var small=new NodeFragmentSettings(1,1,3,1024,1024,1);
            assertThatThrownBy(()->NodeFragmentSource.read(c,2,small)).hasMessage("large");
            try(var q=c.createStatement()){q.execute("UPDATE node SET txt='x' WHERE node_id=2");q.execute("INSERT INTO image VALUES(2,zeroblob(1025))");}
            assertThatThrownBy(()->NodeFragmentSource.read(c,2,small)).hasMessage("large");
            try(var q=c.createStatement()){q.execute("DELETE FROM image");q.execute("INSERT INTO codebox VALUES(2,'xx'),(2,'xx')");}
            assertThatThrownBy(()->NodeFragmentSource.read(c,2,small)).hasMessage("large");
        }
    }
    static NodeFragmentService service(Connection c,NodeService nodes,RichTextRenderingService rich,NodeFragmentSettings settings){
        var service=new NodeFragmentService(nodes,rich,settings);var manager=mock(EntityManager.class);var session=mock(Session.class);
        when(manager.unwrap(Session.class)).thenReturn(session);doAnswer(call->((ReturningWork<?>)call.getArgument(0)).execute(c)).when(session).doReturningWork(any());
        ReflectionTestUtils.setField(service,"manager",manager);return service;
    }
    @Test void sharedFragmentRendersLiveObjectsWithCurrentTokenWithoutChangingStoredContent()throws Exception{
        try(var c=database()){
            try(var q=c.createStatement()){q.execute("PRAGMA query_only=ON");}
            var nodes=mock(NodeService.class);var rich=mock(RichTextRenderingService.class);var node=new Node();node.setNodeId(2L);node.setSyntax("custom-colors");node.setTxt("source");
            when(nodes.findById(2)).thenReturn(node);when(rich.renderLive(node,"current")).thenReturn("<p>Text<img src=\"/images/2/0?_tenantView=current\"><code>code</code><table><tr><td>cell</td></tr></table></p>");
            var fragment=service(c,nodes,rich,settings()).fragment(3,"current");assertThat(fragment.contentId()).isEqualTo("2");assertThat(fragment.name()).isEqualTo("<Source>");assertThat(fragment.html()).contains("/images/2/0?_tenantView=current","<code>","<table>");assertThat(node.getTxt()).isEqualTo("source");
            try(var q=c.createStatement();var rs=q.executeQuery("SELECT txt FROM node WHERE node_id=2")){assertThat(rs.next()).isTrue();assertThat(rs.getString(1)).isEqualTo("source");}
        }
    }
    @Test void helperGateRunsBeforeBodyLookupAndPlainTextStaysEscaped()throws Exception{
        try(var c=database()){
            var nodes=mock(NodeService.class);var rich=mock(RichTextRenderingService.class);var service=service(c,nodes,rich,settings());
            assertThatThrownBy(()->service.fragment(11,"t")).hasMessage("managed");verifyNoInteractions(nodes,rich);
            try(var q=c.createStatement()){q.execute("UPDATE node SET syntax='plain-text' WHERE node_id=2");}
            var node=new Node();node.setTxt("<script>literal</script>\nTürkçe 😀");when(nodes.findById(2)).thenReturn(node);
            assertThat(service.fragment(2,"t").html()).contains("&lt;script&gt;","Türkçe 😀").doesNotContain("<script>");verifyNoInteractions(rich);
        }
    }
    @Test void reusedMarkupRemovesActiveControlsHandlersAndRemoteImages(){
        var html=NodeFragmentService.readingHtml("<p id='a' onclick='bad()'>Text</p><script>bad()</script><iframe src='https://example.test'></iframe><form>form<input></form><img src='https://example.test/x' onerror='bad()'><img src='/images/1/0?_tenantView=t'><a href='javascript:bad()'>bad</a>");
        var doc=org.jsoup.Jsoup.parseBodyFragment(html);assertThat(doc.select("script,iframe,form,input,[onclick],[onerror],a[href^=javascript],img[src^=https]")).isEmpty();assertThat(doc.select("img[src^=/images/]")).hasSize(1);assertThat(doc.selectFirst("p").id()).isEqualTo("a");
        assertThatThrownBy(()->new NodeFragmentSettings(0,20,10,1024,1024,1)).isInstanceOf(IllegalArgumentException.class);
    }
}

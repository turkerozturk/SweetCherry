package com.turkerozturk.importing.mindmap;

import com.turkerozturk.children.*;
import com.turkerozturk.exportmindmap.*;
import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.node.*;
import com.turkerozturk.node.properties.NodePropertiesService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MindMapImportTest {
    @TempDir Path directory;
    static MindMapImportData read(String xml){return MindMapImportData.read(xml.getBytes(StandardCharsets.UTF_8));}
    Connection target()throws Exception {
        var c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve(UUID.randomUUID()+".ctb"));
        try(var s=c.createStatement();var in=getClass().getResourceAsStream("/schemas/schema-sqlite.sql")){
            for(String sql:new String(in.readAllBytes(),StandardCharsets.UTF_8).split(";"))if(!sql.isBlank())s.execute(sql);
            s.execute("INSERT INTO node VALUES(80,'Existing','unchanged','plain-text','',0,0,0,0,0,0,1,2)");
            s.execute("INSERT INTO children VALUES(80,0,7,0)");
            s.execute("INSERT INTO children VALUES(100,80,1,80)");
        }
        c.setAutoCommit(false);return c;
    }
    long number(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    String text(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getString(1);}}
    @Test void suppliedFreeplaneMapKeepsAll69NodesAndNeverTreatsStylesAsChildren()throws Exception {
        byte[] bytes;try(var in=getClass().getResourceAsStream("/mindmap/freeplaneFunctions.mm")){bytes=in.readAllBytes();}
        var data=MindMapImportData.read(bytes);
        assertThat(data.count()).isEqualTo(69);
        assertThat(data.rootName()).isEqualTo("Basic Freeplane\nFunctions");
        assertThat(data.nodes().get(0).text()).contains("Hover with the cursor","Note panel");
        assertThat(data.nodes()).anySatisfy(e->{assertThat(e.name()).isEqualTo("Spell check & Academic Writing suite");assertThat(e.text()).contains("Spelling dictionaries","freeplaneUserGuide.mm#ID_");});
        assertThat(data.nodes()).anySatisfy(e->{assertThat(e.name()).isEqualTo("node c");assertThat(e.text()).contains("This text only","tool tip");});
        assertThat(data.warnings()).containsKeys("unsupported","icons","encrypted","titles");
        assertThat(data.nodes().stream().filter(e->e.parent()==-1)).hasSize(1);
        try(var target=target()) {
            assertThat(data.insert(target)).isEqualTo(69);target.commit();
            assertThat(number(target,"SELECT COUNT(*) FROM node")).isEqualTo(70);
            assertThat(number(target,"SELECT sequence FROM children WHERE node_id=101")).isEqualTo(8);
            assertThat(text(target,"SELECT txt FROM node WHERE node_id=80")).isEqualTo("unchanged");
        }
    }
    @Test void appendsOneRootWithNewIdsSiblingOrderPropertiesAndEmptyBodies()throws Exception {
        var data=read("<map><node TEXT='Root' COLOR='#3584E4' CREATED='1700000000123' MODIFIED='1700000100999'><font BOLD='true'/><node TEXT='First'/><node TEXT='Second'><node TEXT='Grandchild'/></node></node></map>");
        try(var c=target()) {
            data.insert(c);c.commit();
            assertThat(number(c,"SELECT sequence FROM children WHERE node_id=101")).isEqualTo(8);
            assertThat(number(c,"SELECT father_id FROM children WHERE node_id=104")).isEqualTo(103);
            assertThat(number(c,"SELECT sequence FROM children WHERE node_id=103")).isEqualTo(2);
            assertThat(number(c,"SELECT is_richtxt FROM node WHERE node_id=101")).isEqualTo((0x3584E4L<<3)|2);
            assertThat(number(c,"SELECT ts_creation FROM node WHERE node_id=101")).isEqualTo(1700000000);
            assertThat(number(c,"SELECT ts_lastsave FROM node WHERE node_id=101")).isEqualTo(1700000100);
            assertThat(text(c,"SELECT txt FROM node WHERE node_id=102")).isEmpty();
            assertThat(text(c,"SELECT syntax FROM node WHERE node_id=101")).isEqualTo("plain-text");
        }
    }
    @Test void exportsAndReimportsNamesPlainContentPropertiesAndEverySweetCherryIconWithoutImageFiles()throws Exception {
        for(NodeIcon icon:NodeIcon.values()) {
            var children=mock(ChildrenRepository.class);var nodes=mock(NodeRepository.class);var content=mock(MindMapTextContent.class);
            var occurrence=new Children().setNodeId(7).setFatherId(0);
            var node=new Node();node.setNodeId(7);node.setName("Türkçe 😀 & <Root>");node.setSyntax("plain-text");node.setTxt("original");
            node.setIsReadOnly16bit(icon.getIconIdIn16bit());node.parseNodeIconAndReadOnlyFlag();
            node.setIsRichText((0x3584E4L<<3)|2);node.parseNodeTitleColorAndBoldnessAndTextType();node.setCreationTimestamp(1700000000);node.setLastSaveTimestamp(1700000010);
            when(children.findByNodeId(7L)).thenReturn(occurrence);when(nodes.findById(7L)).thenReturn(node);
            when(content.text(7,"plain-text","original")).thenReturn("  first\n\n<script>literal</script> & 😀\n");
            var exporter=new MindMapExportController(children,nodes);ReflectionTestUtils.setField(exporter,"textContent",content);
            var data=MindMapImportData.read(exporter.exportMindMap(7,null,"chat",true,true,"references",true).getBody());
            var actual=data.nodes().get(0);
            assertThat(actual.icon()).as(icon.getIconName()).isEqualTo(icon.getIconIdIn16bit());
            assertThat(actual.name()).isEqualTo(node.getName());
            assertThat(actual.text()).isEqualTo("  first\n\n<script>literal</script> & 😀\n");
            assertThat(actual.titleBits()).isEqualTo(node.getIsRichText());
            assertThat(actual.created()).isEqualTo(1700000000);
        }
    }
    @Test void legacyRootDatabaseEmblemIsNotMistakenForItsNodeIcon() {
        String prefix="<map><!--Exported by SweetCherry: https://github.com/turkerozturk/SweetCherry--><node TEXT='R'><hook NAME='ExternalObject' URI='ctbicons/cherry_red.png'/>";
        assertThat(read(prefix+"</node></map>").nodes().get(0).icon()).isZero();
        assertThat(read(prefix+"<hook NAME='ExternalObject' URI='ctbicons/ct_notes.png'/></node></map>").nodes().get(0).icon()).isEqualTo(NodeIcon.CT_NOTES.getIconIdIn16bit());
        assertThat(read("<map><node TEXT='R'><node TEXT='C'><hook NAME='ExternalObject' URI='ctbicons/ct_home.png'/></node></node></map>").nodes().get(1).icon()).isEqualTo(NodeIcon.CT_HOME.getIconIdIn16bit());
    }
    @Test void notesDetailsLinksAttributesImagesAndFormulasRemainInertReadableText() {
        var data=read("""
            <map><node TEXT='=3+4' LINK='#ID_2' ENCRYPTED_CONTENT='opaque'>
            <richcontent TYPE='DETAILS'><html><body><p>Line one</p><p>Line two</p></body></html></richcontent>
            <richcontent TYPE='NOTE'><html><body><p>Note <b>bold</b> and <a href='file:///not-read'>local link</a></p><img src='https://not-fetched/image.png'/><script>not-executed</script></body></html></richcontent>
            <attribute NAME='key' VALUE='value'/><icon BUILTIN='bee'/>
            </node></map>
            """);
        var node=data.nodes().get(0);
        assertThat(node.name()).isEqualTo("=3+4");
        assertThat(node.text()).contains("Details:","Line one\nLine two","Note:","Note bold and local link (file:///not-read)","image.png","[Link] #ID_2","key: value","Encrypted").doesNotContain("not-executed");
        assertThat(node.icon()).isZero();
    }
    @Test void malformedXmlDtdEntitiesAndNonMapOrMultipleRootsAreRejected() {
        for(String xml:List.of("<map><node></map>","<node TEXT='x'/>","<map/>","<map><node/><node/></map>","<!DOCTYPE map [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><map><node TEXT='&x;'/></map>"))
            assertThatThrownBy(()->read(xml)).isInstanceOf(IllegalArgumentException.class).hasMessage("xml");
    }
    @Test void bytesNodeCountDepthAndLongTitlesAreBounded() {
        assertThatThrownBy(()->MindMapImportData.read(new byte[MindMapImportData.MAX_BYTES+1])).hasMessage("limit");
        assertThatThrownBy(()->read("<map>"+"<node>".repeat(130)+"</node>".repeat(130)+"</map>")).hasMessage("limit");
        assertThatThrownBy(()->read("<map><node>"+"<node/>".repeat(10000)+"</node></map>")).hasMessage("limit");
        var title="😀".repeat(300);var data=read("<map><node TEXT='"+title+"'/></map>");
        assertThat(data.rootName().codePointCount(0,data.rootName().length())).isEqualTo(255);
        assertThat(data.nodes().get(0).text()).contains(title);
    }
    @Test void overflowAndNonTransactionalTargetsFailBeforeAnyInsert()throws Exception {
        try(var c=target()) {
            try(var s=c.createStatement()){s.execute("INSERT INTO children VALUES(9223372036854775807,0,99,80)");}
            assertThatThrownBy(()->read("<map><node TEXT='r'/></map>").insert(c)).isInstanceOf(ArithmeticException.class);
            assertThat(number(c,"SELECT COUNT(*) FROM node")).isEqualTo(1);
        }
        try(var c=target()){c.commit();c.setAutoCommit(true);assertThatThrownBy(()->read("<map><node/></map>").insert(c)).isInstanceOf(SQLException.class);}
    }
    @Test void serviceChecksWritableAgainBeforeConfirmation()throws Exception {
        var properties=mock(NodePropertiesService.class);var service=new MindMapImportService(properties);
        assertThatThrownBy(()->service.preview(new java.io.ByteArrayInputStream(new byte[0]))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        when(properties.writable()).thenReturn(true);
        var data=service.preview(new java.io.ByteArrayInputStream("<map><node/></map>".getBytes(StandardCharsets.UTF_8)));
        when(properties.writable()).thenReturn(false);
        assertThatThrownBy(()->service.confirm(data)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void failedInsertRollsBackTheWholeImportThroughTheSpringTransaction()throws Exception {
        try(var c=target()) {
            try(var s=c.createStatement()){s.execute("CREATE TRIGGER fail_child BEFORE INSERT ON children WHEN NEW.node_id>100 BEGIN SELECT RAISE(ABORT,'test failure'); END");}c.commit();
            var properties=mock(NodePropertiesService.class);when(properties.writable()).thenReturn(true);
            var raw=new MindMapImportService(properties);var em=mock(jakarta.persistence.EntityManager.class);var session=mock(org.hibernate.Session.class);
            when(em.unwrap(org.hibernate.Session.class)).thenReturn(session);ReflectionTestUtils.setField(raw,"entityManager",em);
            String url=c.getMetaData().getURL();var ds=new org.springframework.jdbc.datasource.DriverManagerDataSource(url);
            when(session.doReturningWork(any())).thenAnswer(call->{var tx=org.springframework.jdbc.datasource.DataSourceUtils.getConnection(ds);try{return ((org.hibernate.jdbc.ReturningWork<?>)call.getArgument(0)).execute(tx);}finally{org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection(tx,ds);}});
            var proxy=new org.springframework.aop.framework.ProxyFactory(raw);
            proxy.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
            var service=(MindMapImportService)proxy.getProxy();
            assertThatThrownBy(()->service.confirm(read("<map><node TEXT='r'><node TEXT='c'/></node></map>"))).hasRootCauseMessage("[SQLITE_CONSTRAINT_TRIGGER] A RAISE function within a trigger fired, causing the SQL statement to abort (test failure)");
            assertThat(number(c,"SELECT COUNT(*) FROM node")).isEqualTo(1);
        }
    }
}

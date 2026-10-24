package com.turkerozturk.importing.exportednodes;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import jakarta.persistence.EntityManager;
import com.turkerozturk.node.properties.NodePropertiesService;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CtbImportTest {
    @TempDir Path directory;
    Connection open(Path path) throws Exception { return DriverManager.getConnection("jdbc:sqlite:"+path); }
    void sql(Connection c,String sql)throws Exception {try(var s=c.createStatement()){s.execute(sql);}}
    long number(Connection c,String sql)throws Exception {try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    String text(Connection c,String sql)throws Exception {try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getString(1);}}
    void schema(Connection c)throws Exception {
        try(var in=getClass().getResourceAsStream("/schemas/schema-sqlite.sql")) {
            for(String statement:new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split(";")) if(!statement.isBlank())sql(c,statement);
        }
    }
    Path source()throws Exception {
        Path file=directory.resolve("exports/English Türkçe (1).ctb");Files.createDirectories(file.getParent());
        try(var c=open(file)) {
            schema(c);
            sql(c,"INSERT INTO node VALUES(2,'Master','<node><rich_text link=\"node 11 anchor\">Link</rich_text></node>','custom-colors','tags',32768,1,1,1,1,0,111,222)");
            sql(c,"INSERT INTO node VALUES(3,'Child','Plain','plain-text','',0,0,0,0,0,0,112,223)");
            sql(c,"INSERT INTO children VALUES(2,0,5,0)");
            sql(c,"INSERT INTO children VALUES(11,0,7,2)");
            sql(c,"INSERT INTO children VALUES(3,11,9,0)");
            sql(c,"INSERT INTO bookmark VALUES(11,1)");sql(c,"INSERT INTO bookmark VALUES(2,2)");
            sql(c,"INSERT INTO image VALUES(2,0,'left','',X'010203','file.bin','node 3',123)");
            sql(c,"INSERT INTO grid VALUES(2,1,'left','<table/>',1,2)");
            sql(c,"INSERT INTO codebox VALUES(2,2,'left','code','java',100,50,1,0,1)");
        }
        return file;
    }
    Connection target()throws Exception {
        var c=open(directory.resolve(UUID.randomUUID()+".ctb"));schema(c);
        sql(c,"INSERT INTO node VALUES(80,'Existing','unchanged','plain-text','',0,0,0,0,0,0,1,2)");
        sql(c,"INSERT INTO children VALUES(80,0,4,0)");
        sql(c,"INSERT INTO children VALUES(100,80,1,80)");
        sql(c,"INSERT INTO bookmark VALUES(80,3)");
        c.setAutoCommit(false);return c;
    }
    @Test void copiesEveryTableRekeysSharedGroupsAppendsRootsAndPreservesPayloads()throws Exception {
        Path path=source();byte[] original=Files.readAllBytes(path);
        var data=CtbImportData.read(path);
        assertThat(data.counts()).containsEntry("node",2).containsEntry("children",3).containsEntry("bookmark",2).containsEntry("image",1).containsEntry("grid",1).containsEntry("codebox",1);
        try(var c=target()) {
            assertThat(data.insert(c)).isEqualTo(3);c.commit();
            assertThat(number(c,"SELECT MIN(node_id) FROM children WHERE node_id>100")).isEqualTo(101);
            assertThat(number(c,"SELECT father_id FROM children WHERE node_id=102")).isEqualTo(103);
            assertThat(number(c,"SELECT master_id FROM children WHERE node_id=103")).isEqualTo(101);
            assertThat(number(c,"SELECT sequence FROM children WHERE node_id=101")).isEqualTo(5);
            assertThat(number(c,"SELECT sequence FROM children WHERE node_id=103")).isEqualTo(6);
            assertThat(number(c,"SELECT sequence FROM children WHERE node_id=102")).isEqualTo(9);
            assertThat(number(c,"SELECT node_id FROM bookmark WHERE sequence=4")).isEqualTo(103);
            assertThat(text(c,"SELECT txt FROM node WHERE node_id=101")).contains("node 103 anchor");
            assertThat(text(c,"SELECT link FROM image WHERE node_id=101")).isEqualTo("node 102");
            assertThat(number(c,"SELECT ts_creation FROM node WHERE node_id=101")).isEqualTo(111);
            assertThat(number(c,"SELECT ts_lastsave FROM node WHERE node_id=101")).isEqualTo(222);
            assertThat(text(c,"SELECT hex(png) FROM image WHERE node_id=101")).isEqualTo("010203");
            assertThat(text(c,"SELECT txt FROM node WHERE node_id=80")).isEqualTo("unchanged");
            assertThat(number(c,"SELECT COUNT(*) FROM grid WHERE node_id=101")).isEqualTo(1);
            assertThat(number(c,"SELECT COUNT(*) FROM codebox WHERE node_id=101")).isEqualTo(1);
        }
        assertThat(Files.readAllBytes(path)).isEqualTo(original);
    }
    @Test void legacyMasterColumnIsVirtualAndSourceRemainsUnchanged()throws Exception {
        var path=source();try(var c=open(path)){sql(c,"DELETE FROM bookmark WHERE node_id=11");sql(c,"DELETE FROM children WHERE node_id=11");sql(c,"UPDATE children SET father_id=2 WHERE node_id=3");sql(c,"UPDATE node SET txt='plain',syntax='plain-text' WHERE node_id=2");sql(c,"ALTER TABLE children DROP COLUMN master_id");}
        byte[] before=Files.readAllBytes(path);assertThat(CtbImportData.read(path).occurrences).hasSize(2);assertThat(Files.readAllBytes(path)).isEqualTo(before);
    }
    @Test void invalidReferencesAndCyclesAreRejectedBeforeWriting()throws Exception {
        var path=source();try(var c=open(path)){sql(c,"UPDATE children SET father_id=3 WHERE node_id=11");}
        assertThatThrownBy(()->CtbImportData.read(path)).isInstanceOf(IllegalArgumentException.class).hasMessage("hierarchy");
        try(var c=open(path)){sql(c,"UPDATE children SET father_id=999 WHERE node_id=11");}
        assertThatThrownBy(()->CtbImportData.read(path)).hasMessage("hierarchy");
    }
    @Test void missingMasterAndDanglingObjectAreRejected()throws Exception {
        var path=source();try(var c=open(path)){sql(c,"UPDATE children SET master_id=999 WHERE node_id=11");}
        assertThatThrownBy(()->CtbImportData.read(path)).hasMessage("hierarchy");
        try(var c=open(path)){sql(c,"UPDATE children SET master_id=2 WHERE node_id=11");sql(c,"UPDATE image SET node_id=999");}
        assertThatThrownBy(()->CtbImportData.read(path)).hasMessage("hierarchy");
    }
    @Test void danglingInternalLinkIsRejectedRatherThanPointingAtUnrelatedTargetNode()throws Exception {
        var path=source();try(var c=open(path)){sql(c,"UPDATE image SET link='node 80'");}
        assertThatThrownBy(()->CtbImportData.read(path)).hasMessage("links");
    }
    @Test void digestDetectsContentChangeAndIsStableOnRepeatedReads()throws Exception {
        var path=source();var first=CtbImportData.read(path).digest;assertThat(CtbImportData.read(path).digest).isEqualTo(first);
        try(var c=open(path)){sql(c,"UPDATE node SET name='Changed' WHERE node_id=2");}
        assertThat(CtbImportData.read(path).digest).isNotEqualTo(first);
    }
    @Test void checkedFailureRollsBackAllPreviouslyInsertedTablesViaSpringTransaction()throws Exception {
        var path=source();var properties=mock(NodePropertiesService.class);when(properties.writable()).thenReturn(true);
        var raw=new CtbImportService(properties);
        ReflectionTestUtils.setField(raw,"applicationPath",directory.toString());ReflectionTestUtils.setField(raw,"folder","exports");
        Path target=directory.resolve("rollback.ctb");try(var c=open(target)){schema(c);sql(c,"CREATE TRIGGER fail_image BEFORE INSERT ON image BEGIN SELECT RAISE(ABORT,'test failure'); END");}
        var ds=new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:sqlite:"+target);
        var em=mock(EntityManager.class);var session=mock(Session.class);when(em.unwrap(Session.class)).thenReturn(session);
        when(session.doReturningWork(any())).thenAnswer(call->{
            var c=org.springframework.jdbc.datasource.DataSourceUtils.getConnection(ds);
            try{return ((ReturningWork<?>)call.getArgument(0)).execute(c);}finally{org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection(c,ds);}
        });ReflectionTestUtils.setField(raw,"entityManager",em);
        var factory=new org.springframework.aop.framework.ProxyFactory(raw);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        var service=(CtbImportService)factory.getProxy();
        var preview=service.preview(path.getFileName().toString());
        assertThatThrownBy(()->service.confirm(preview)).isInstanceOf(SQLException.class);
        try(var c=open(target)){for(String table:CtbImportData.COLUMNS.keySet())assertThat(number(c,"SELECT COUNT(*) FROM "+table)).isZero();}
    }
    @Test void previewAndConfirmationRecheckWritableAndSourceDigest()throws Exception {
        var path=source();var properties=mock(NodePropertiesService.class);when(properties.writable()).thenReturn(true);
        var service=new CtbImportService(properties);ReflectionTestUtils.setField(service,"applicationPath",directory.toString());ReflectionTestUtils.setField(service,"folder","exports");
        var preview=service.preview(path.getFileName().toString());
        when(properties.writable()).thenReturn(false);
        assertThatThrownBy(()->service.confirm(preview)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        when(properties.writable()).thenReturn(true);try(var c=open(path)){sql(c,"UPDATE node SET name='Changed'");}
        assertThatThrownBy(()->service.confirm(preview)).hasMessage("changed");
        assertThatThrownBy(()->service.preview("../target.ctb")).hasMessage("file");
        assertThatThrownBy(()->service.preview("missing.ctb")).hasMessage("file");assertThat(Files.exists(directory.resolve("exports/missing.ctb"))).isFalse();
        assertThatThrownBy(()->service.preview("a.old")).hasMessage("file");
    }
    @Test void sameFileCanBeImportedAfterReadSnapshotCloses()throws Exception {
        var path=source();var data=CtbImportData.read(path);
        try(var c=open(path)){c.setAutoCommit(false);assertThat(data.insert(c)).isEqualTo(3);c.commit();assertThat(number(c,"SELECT COUNT(*) FROM children")).isEqualTo(6);}
    }
    @Test void emptySourceAndOverflowAreSafe()throws Exception {
        var path=directory.resolve("empty.ctb");try(var c=open(path)){schema(c);}
        try(var target=target()){assertThat(CtbImportData.read(path).insert(target)).isZero();}
        var data=CtbImportData.read(source());try(var target=target()){sql(target,"INSERT INTO children VALUES(9223372036854775807,0,99,80)");assertThatThrownBy(()->data.insert(target)).isInstanceOf(ArithmeticException.class);assertThat(number(target,"SELECT COUNT(*) FROM node")).isEqualTo(1);}
    }
}

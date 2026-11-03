package com.turkerozturk.node.tags;

import com.turkerozturk.node.properties.NodePropertiesService;
import java.nio.file.*;
import java.sql.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TagManagementServiceTest {
    @TempDir Path dir;
    private final NodePropertiesService properties=mock(NodePropertiesService.class);
    private final TagManagementService service=new TagManagementService(properties);
    private final TagManagementService.Operation rename=new TagManagementService.Operation(TagManagementService.Action.RENAME,"old","new");
    private Connection database()throws Exception{
        var c=DriverManager.getConnection("jdbc:sqlite::memory:");seed(c);return c;
    }
    private void seed(Connection c)throws Exception{try(var s=c.createStatement()){
        s.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,tags TEXT,ts_creation INTEGER,ts_lastsave INTEGER)");
        s.execute("INSERT INTO node VALUES(2,'Master','  old new old C++  ',100,101),(3,'Second','old other',200,201),(4,'Unchanged','Old older C++',300,301)");
    }}
    @Test void previewIsReadOnlyAndRenameIsLiteralDeduplicatedAndUpdatesOnlyModified()throws Exception{
        try(var c=database()){
            var preview=service.preview(c,rename);assertThat(preview.count()).isEqualTo(2);assertThat(preview.rows().get(0).after()).isEqualTo("new C++");
            try(var q=c.createStatement();var r=q.executeQuery("SELECT tags FROM node WHERE node_id=2")){r.next();assertThat(r.getString(1)).isEqualTo("  old new old C++  ");}
            c.setAutoCommit(false);var result=service.apply(c,rename,preview.revision());c.commit();assertThat(result.count()).isEqualTo(2);
            try(var q=c.createStatement();var r=q.executeQuery("SELECT tags,ts_creation,ts_lastsave FROM node WHERE node_id=2")){r.next();assertThat(r.getString(1)).isEqualTo("new C++");assertThat(r.getLong(2)).isEqualTo(100);assertThat(r.getLong(3)).isGreaterThan(101);}
            try(var q=c.createStatement();var r=q.executeQuery("SELECT tags,ts_lastsave FROM node WHERE node_id=4")){r.next();assertThat(r.getString(1)).isEqualTo("Old older C++");assertThat(r.getLong(2)).isEqualTo(301);}
        }
    }
    @Test void removalAndCaseOnlyRenamePreserveOtherTagsAndMayEmptyTheField()throws Exception{
        try(var c=database();var s=c.createStatement()){
            s.execute("UPDATE node SET tags='old' WHERE node_id=2");var remove=new TagManagementService.Operation(TagManagementService.Action.REMOVE,"old",null);
            c.setAutoCommit(false);service.apply(c,remove,service.preview(c,remove).revision());c.commit();
            try(var r=s.executeQuery("SELECT tags FROM node ORDER BY node_id")){r.next();assertThat(r.getString(1)).isEmpty();r.next();assertThat(r.getString(1)).isEqualTo("other");}
            var change=new TagManagementService.Operation(TagManagementService.Action.RENAME,"Old","OLD");service.apply(c,change,service.preview(c,change).revision());c.commit();
            try(var r=s.executeQuery("SELECT tags FROM node WHERE node_id=4")){r.next();assertThat(r.getString(1)).isEqualTo("OLD older C++");}
        }
    }
    @Test void rejectsChangedRowsNamesAndNewMatchesWithoutWriting()throws Exception{
        for(String sql:new String[]{"UPDATE node SET tags='old extra' WHERE node_id=2","UPDATE node SET name='Changed' WHERE node_id=2","INSERT INTO node VALUES(9,'New','old',1,1)"})try(var c=database();var s=c.createStatement()){
            var preview=service.preview(c,rename);s.execute(sql);
            assertThatThrownBy(()->service.apply(c,rename,preview.revision())).isInstanceOf(TagManagementProblem.class).hasMessage("changed");
            try(var r=s.executeQuery("SELECT ts_lastsave FROM node WHERE node_id=3")){r.next();assertThat(r.getLong(1)).isEqualTo(201);}
        }
    }
    @Test void validatesNamesAndReadonlyBeforeAccessingEntityManager(){
        assertThatThrownBy(()->new TagManagementService.Operation(TagManagementService.Action.RENAME,"old","two tags")).isInstanceOf(TagManagementProblem.class);
        assertThatThrownBy(()->new TagManagementService.Operation(TagManagementService.Action.RENAME,"old","old")).isInstanceOf(TagManagementProblem.class);
        assertThatThrownBy(()->new TagManagementService.Operation(TagManagementService.Action.RENAME,"old","line\nfeed")).isInstanceOf(TagManagementProblem.class);
        assertThatThrownBy(()->service.preview(rename)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->service.apply(rename,"x")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(TagManagementService.safeLog("name\nforged\tline\r")).isEqualTo("name forged line ");
    }
    @Test void realTransactionRollsBackAllRowsAndLogsOnlyAfterSuccessfulCommit()throws Exception{
        var source=new DriverManagerDataSource("jdbc:sqlite:"+dir.resolve("tags.ctb"));try(var c=source.getConnection()){seed(c);}
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(TagManagementService.class);
        var logs=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();logs.start();logger.addAppender(logs);
        try{
            String revision;try(var c=source.getConnection()){revision=service.preview(c,rename).revision();try(var s=c.createStatement()){s.execute("CREATE TRIGGER fail_tag BEFORE UPDATE ON node WHEN OLD.node_id=3 BEGIN SELECT RAISE(ABORT,'reject'); END");}}
            assertThatThrownBy(()->tx.execute(status->{var c=DataSourceUtils.getConnection(source);try{return service.apply(c,rename,revision);}catch(SQLException e){throw new IllegalStateException(e);}finally{DataSourceUtils.releaseConnection(c,source);}})).isInstanceOf(IllegalStateException.class);
            assertThat(logs.list).isEmpty();try(var c=source.getConnection();var s=c.createStatement()){try(var r=s.executeQuery("SELECT tags,ts_lastsave FROM node WHERE node_id=2")){r.next();assertThat(r.getString(1)).isEqualTo("  old new old C++  ");assertThat(r.getLong(2)).isEqualTo(101);}s.execute("DROP TRIGGER fail_tag");}
            tx.execute(status->{var c=DataSourceUtils.getConnection(source);try{var result=service.apply(c,rename,revision);assertThat(logs.list).isEmpty();return result;}catch(SQLException e){throw new IllegalStateException(e);}finally{DataSourceUtils.releaseConnection(c,source);}});
            assertThat(logs.list).hasSize(3);assertThat(logs.list.get(0).getFormattedMessage()).contains("nodeId=2","nodeName=\"Master\"","action=RENAME","from=\"old\"","to=\"new\"");
        }finally{logger.detachAppender(logs);logs.stop();}
    }
}

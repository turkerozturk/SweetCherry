package com.turkerozturk.node.title;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.EntityManager;
import java.sql.*;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeTitleServiceTest {
    Connection database()throws Exception{
        var c=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var q=c.createStatement()){
            q.execute("CREATE TABLE node(node_id INTEGER,name TEXT,tags TEXT,ts_lastsave INTEGER,txt TEXT,is_ro INTEGER,is_richtxt INTEGER)");
            q.execute("CREATE TABLE children(node_id INTEGER,master_id INTEGER)");
            q.execute("INSERT INTO node VALUES(1,'Old title','tags',10,'<node>keep content</node>',88,12345)");
            q.execute("INSERT INTO children VALUES(1,0),(9,1)");
        }return c;
    }
    NodeTitleService service(Connection c,NodePropertiesService properties){
        var value=new NodeTitleService(properties);var manager=mock(EntityManager.class);var session=mock(Session.class);
        when(manager.unwrap(Session.class)).thenReturn(session);
        doAnswer(call->((ReturningWork<?>)call.getArgument(0)).execute(c)).when(session).doReturningWork(any());
        ReflectionTestUtils.setField(value,"manager",manager);return value;
    }
    NodePropertiesService properties(){var properties=mock(NodePropertiesService.class);when(properties.writable()).thenReturn(true);var node=new Node();node.setTags("tags");when(properties.realNode(anyLong())).thenReturn(node);return properties;}
    @Test void renameChangesOnlyNameAndTimestampAndReturnsNewRevision()throws Exception{
        try(var c=database()){
            var service=service(c,properties());var before=service.open(1);
            var after=service.save(1,"  New <title> & Türkçe  ",before.revision());
            assertThat(after.name()).isEqualTo("New <title> & Türkçe");assertThat(after.revision()).isNotEqualTo(before.revision());
            try(var q=c.createStatement();var row=q.executeQuery("SELECT txt,tags,is_ro,is_richtxt,ts_lastsave FROM node")){
                assertThat(row.next()).isTrue();assertThat(row.getString(1)).isEqualTo("<node>keep content</node>");assertThat(row.getString(2)).isEqualTo("tags");
                assertThat(row.getInt(3)).isEqualTo(88);assertThat(row.getInt(4)).isEqualTo(12345);assertThat(row.getLong(5)).isGreaterThan(10);
            }
        }
    }
    @Test void sameTitleIsANoOpAndStaleNamesOrModifiedDatesAreRejected()throws Exception{
        try(var c=database()){
            var service=service(c,properties());var before=service.open(1);assertThat(service.save(1,"Old title",before.revision())).isEqualTo(before);
            try(var q=c.createStatement()){q.execute("UPDATE node SET name='Concurrent title'");}
            assertThatThrownBy(()->service.save(1,"Overwrite",before.revision())).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(409));
            var current=service.open(1);try(var q=c.createStatement()){q.execute("UPDATE node SET ts_lastsave=11");}
            assertThatThrownBy(()->service.save(1,"Overwrite",current.revision())).isInstanceOf(ResponseStatusException.class);
        }
    }
    @Test void invalidNamesAliasesAndReadOnlyConnectionsAreRejected()throws Exception{
        try(var c=database()){
            var properties=properties();var service=service(c,properties);
            for(var name:java.util.Arrays.asList(null," ","x".repeat(256)))assertThatThrownBy(()->service.save(1,name,"r")).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(400));
            assertThatThrownBy(()->service.open(9)).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(404));
            when(properties.writable()).thenReturn(false);assertThat(properties.writable()).isFalse();assertThatThrownBy(()->service.open(1)).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(403));
        }
    }
    @Test void propertiesProtectionAndConcurrentTagChangesCannotBeBypassed()throws Exception{
        try(var c=database()){
            var properties=properties();var service=service(c,properties);var before=service.open(1);
            try(var q=c.createStatement()){q.execute("UPDATE node SET tags='changed'");}
            assertThatThrownBy(()->service.save(1,"Overwrite",before.revision())).isInstanceOf(ResponseStatusException.class);
            when(properties.realNode(1L)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"reminder.protected"));
            assertThatThrownBy(()->service.open(1)).hasMessageContaining("reminder.protected");
        }
    }
}

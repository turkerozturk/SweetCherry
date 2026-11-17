package com.turkerozturk.node.todo;

import com.turkerozturk.export.SqliteSchemaLoader;
import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.node.TrashSql;
import com.turkerozturk.node.audit.AuditSql;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoServiceTest {
    Connection c; TodoService service; NodePropertiesService properties;
    @BeforeEach void setup() throws Exception {
        c = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (var sql = c.createStatement()) {
            for (String ddl : SqliteSchemaLoader.load().split(";")) if (!ddl.isBlank()) sql.execute(ddl);
            sql.execute("INSERT INTO node(node_id,name,tags,ts_lastsave,txt,syntax,is_ro,is_richtxt) VALUES(1,'<First>','todo project',100,'Original text','plain-text',0,3),(2,'Second','todo done project',200,'Second text','plain-text',0,0),(3,'Ordinary','done',300,'','plain-text',0,0),(4,'Case','TODO',400,'','plain-text',0,0),(5,'Prefix','todo-later',500,'','plain-text',0,0),(6,'Orphan','todo',600,'','plain-text',0,0)");
            sql.execute("INSERT INTO children(node_id,father_id,sequence,master_id) VALUES(1,0,1,0),(2,0,2,0),(3,0,3,0),(4,0,4,0),(5,0,5,0),(11,3,1,1),(12,0,6,1)");
            sql.execute("INSERT INTO image(node_id,png) VALUES(1,X'010203')");
        }
        properties = mock(NodePropertiesService.class); when(properties.writable()).thenReturn(true);
        service = new TodoService(properties);
        var em = mock(jakarta.persistence.EntityManager.class); var session = mock(org.hibernate.Session.class);
        when(em.unwrap(org.hibernate.Session.class)).thenReturn(session);
        when(session.doReturningWork(any())).thenAnswer(call -> ((org.hibernate.jdbc.ReturningWork<?>)call.getArgument(0)).execute(c));
        doAnswer(call -> { ((org.hibernate.jdbc.Work)call.getArgument(0)).execute(c); return null; }).when(session).doWork(any());
        ReflectionTestUtils.setField(service,"manager",em);
    }
    @AfterEach void close() throws Exception { c.close(); }
    TodoService.View view(String q, String status, int limit, int page) { return service.inspect(new TodoService.Options(q,status,limit,page)); }
    TodoService.Task task(long content) { return view("","all",50,1).rows().stream().filter(t -> t.contentId()==content).findFirst().orElseThrow(); }
    @Test void exactLowercaseTagsAndOneTaskPerContentExcludeOrphans() {
        var view = view("","all",50,1);
        assertThat(view.total()).isEqualTo(2); assertThat(view.open()).isEqualTo(1); assertThat(view.done()).isEqualTo(1);
        assertThat(view.rows()).extracting(TodoService.Task::contentId).containsExactly(1L,2L);
        assertThat(task(1).locations()).isEqualTo(3); assertThat(task(1).id()).isEqualTo(1);
        assertThat(task(1).breadcrumb()).contains("<First> [1]");
        assertThat(view("","open",50,1).rows()).extracting(TodoService.Task::contentId).containsExactly(1L);
        assertThat(view("","done",50,1).rows()).extracting(TodoService.Task::contentId).containsExactly(2L);
    }
    @Test void filtersAndPaginationKeepGlobalCountsAndStableOrder() {
        var page = view("project","all",1,2);
        assertThat(page.total()).isEqualTo(2); assertThat(page.matched()).isEqualTo(2); assertThat(page.pages()).isEqualTo(2);
        assertThat(page.rows()).extracting(TodoService.Task::contentId).containsExactly(2L); assertThat(page.first()).isEqualTo(2);
        assertThat(view("12","all",50,1).rows()).extracting(TodoService.Task::contentId).containsExactly(1L);
        assertThat(view("no match","all",50,1).rows()).isEmpty();
        assertThat(view("","all",1,99).rows()).isEmpty();
    }
    @Test void trashedAndAuditLocationsAreExcludedWhileLiveAliasesRemain() throws Exception {
        try (var sql = c.createStatement()) {
            sql.execute("INSERT INTO node(node_id,name,tags,syntax) VALUES(90,'Trash','"+TrashSql.TAG+"','plain-text'),(91,'History','"+AuditSql.TAG+"','plain-text')");
            sql.execute("INSERT INTO children(node_id,father_id,sequence,master_id) VALUES(90,0,7,0),(91,0,8,0)");
            sql.execute("UPDATE children SET father_id=90 WHERE node_id=1");
            sql.execute("UPDATE children SET father_id=91 WHERE node_id=2");
        }
        assertThat(task(1).id()).isEqualTo(11); assertThat(task(1).locations()).isEqualTo(2);
        assertThatThrownBy(() -> service.update(1,true,task(1).revision())).hasMessageContaining("changed");
        assertThat(view("","all",50,1).total()).isEqualTo(1);
        try (var sql = c.createStatement()) { sql.execute("UPDATE children SET father_id=90 WHERE node_id IN(11,12)"); }
        assertThat(view("","all",50,1).rows()).isEmpty();
    }
    @Test void cyclesAndMissingParentsDoNotLoopOrPublishBrokenTasks() throws Exception {
        try (var sql = c.createStatement()) { sql.execute("UPDATE children SET father_id=11 WHERE node_id=3"); sql.execute("UPDATE children SET father_id=999 WHERE node_id=2"); }
        assertThat(view("","all",50,1).total()).isEqualTo(1); assertThat(task(1).locations()).isEqualTo(2);
        try (var sql = c.createStatement()) { sql.execute("DELETE FROM children WHERE node_id=1"); }
        assertThat(view("","all",50,1).rows()).isEmpty();
    }
    @Test void completeAndReopenPreserveOtherTagsTextBlobsAndCreationDate() throws Exception {
        var original = task(1); service.update(original.id(),true,original.revision());
        assertThat(task(1).tags()).containsExactly("todo","project","done");
        var completed = task(1); service.update(completed.id(),false,completed.revision());
        assertThat(task(1).tags()).containsExactly("todo","project");
        try (var sql = c.createStatement(); var rs = sql.executeQuery("SELECT txt,is_richtxt,ts_creation,ts_lastsave,(SELECT hex(png) FROM image WHERE node_id=1) FROM node WHERE node_id=1")) {
            rs.next(); assertThat(rs.getString(1)).isEqualTo("Original text"); assertThat(rs.getLong(2)).isEqualTo(3);
            assertThat(rs.getObject(3)).isNull(); assertThat(rs.getLong(4)).isGreaterThan(100); assertThat(rs.getString(5)).isEqualTo("010203");
        }
    }
    @Test void activeSharedOccurrenceUpdatesMasterOnce() throws Exception {
        try (var sql = c.createStatement()) { sql.execute("UPDATE children SET father_id=999 WHERE node_id=1"); }
        var shared = task(1); assertThat(shared.id()).isEqualTo(11);
        service.update(shared.id(),true,shared.revision()); assertThat(task(1).done()).isTrue();
        try (var sql = c.createStatement(); var rs = sql.executeQuery("SELECT COUNT(*) FROM node WHERE node_id=11")) { rs.next(); assertThat(rs.getInt(1)).isZero(); }
    }
    @Test void sameSecondTagChangeAndWrongOccurrenceRejectStaleWrites() throws Exception {
        var original = task(1);
        assertThatThrownBy(() -> service.update(11,true,original.revision())).hasMessageContaining("changed");
        try (var sql = c.createStatement()) { sql.execute("UPDATE node SET tags='todo Project' WHERE node_id=1"); }
        assertThatThrownBy(() -> service.update(1,true,original.revision())).hasMessageContaining("changed");
        assertThat(task(1).tags()).containsExactly("todo","Project");
    }
    @Test void readonlySourcesCanListButCannotWriteAndRemovedTodoCannotBeCompleted() throws Exception {
        var original = task(1); when(properties.writable()).thenReturn(false);
        assertThat(view("","all",50,1).total()).isEqualTo(2);
        assertThatThrownBy(() -> service.update(1,true,original.revision())).hasMessageContaining("readonly");
        when(properties.writable()).thenReturn(true);
        try (var sql = c.createStatement()) { sql.execute("UPDATE node SET tags='project' WHERE node_id=1"); }
        assertThatThrownBy(() -> service.update(1,true,original.revision())).hasMessageContaining("changed");
    }
    @Test void nullTimestampAndNoOpAreHandledWithoutChangingUnrelatedData() throws Exception {
        try (var sql = c.createStatement()) { sql.execute("UPDATE node SET ts_lastsave=NULL WHERE node_id=1"); }
        var original = task(1); service.update(1,false,original.revision()); assertThat(task(1).revision()).isEqualTo(original.revision());
        service.update(1,true,original.revision()); assertThat(task(1).done()).isTrue();
    }
    @Test void rejectsInvalidOptionsAndEmptyDatabaseIsUsable() throws Exception {
        assertThatThrownBy(() -> new TodoService.Options("","unknown",50,1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TodoService.Options("","open",201,1)).isInstanceOf(IllegalArgumentException.class);
        try (var sql = c.createStatement()) { sql.execute("DELETE FROM children"); sql.execute("DELETE FROM node"); }
        var empty = view("","open",50,1); assertThat(empty.total()).isZero(); assertThat(empty.pages()).isEqualTo(1); assertThat(empty.first()).isZero();
    }
}

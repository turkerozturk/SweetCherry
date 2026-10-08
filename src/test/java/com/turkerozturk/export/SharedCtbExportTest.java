package com.turkerozturk.export;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import com.turkerozturk.multipledatabases.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;

class SharedCtbExportTest {
    @TempDir Path directory;
    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test void separateSingleAndSubtreeUseSameServiceAndArchiveOnlyAfterSuccess() throws Exception {
        try(var source=fixture()) {
            var service=service(source);
            var first=service.export(11,false,false);
            Path file=directory.resolve("exports").resolve(first.filename());
            assertThat(first.count()).isEqualTo(1);
            assertThat(service.export(11,true,false).count()).isEqualTo(7);
            try(var target=DriverManager.getConnection("jdbc:sqlite:"+file)) {
                assertThat(number(target,"SELECT COUNT(*) FROM children")).isEqualTo(7);valid(target);
            }
            try(var files=Files.list(file.getParent())) {
                assertThat(files.filter(path->path.toString().endsWith(".old")).count()).isEqualTo(1);
            }
            assertThat(number(source,"SELECT COUNT(*) FROM children")).isEqualTo(19);
        }
    }
    @Test void collectionSingleAndSubtreeAppendWithValidIndependentGroups() throws Exception {
        try(var source=fixture()) {
            var service=service(source);
            assertThat(service.export(11,false,true).count()).isEqualTo(1);
            assertThat(service.export(11,true,true).count()).isEqualTo(7);
            try(var target=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("exports/exportednodes.ctb"))) {
                assertThat(number(target,"SELECT COUNT(*) FROM children")).isEqualTo(8);valid(target);
            }
        }
    }
    @Test void failedSeparateExportPreservesPreviousFileAndRemovesTemporaryOutput() throws Exception {
        try(var source=fixture()) {
            var service=service(source);
            Path file=directory.resolve("exports").resolve(service.export(11,false,false).filename());
            byte[] before=Files.readAllBytes(file);
            try(var st=source.createStatement()){st.execute("DELETE FROM node WHERE node_id=1");}
            assertThatThrownBy(()->service.export(11,true,false))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            assertThat(Files.readAllBytes(file)).isEqualTo(before);
            try(var files=Files.list(file.getParent())) { assertThat(files.count()).isEqualTo(1); }
        }
    }
    @Test void failedCollectionExportRollsBackAppendedRows() throws Exception {
        try(var source=fixture()) {
            var service=service(source);service.export(2,false,true);
            try(var st=source.createStatement()){st.execute("DELETE FROM node WHERE node_id=1");}
            assertThatThrownBy(()->service.export(11,true,true))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            try(var target=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("exports/exportednodes.ctb"))) {
                assertThat(number(target,"SELECT COUNT(*) FROM children")).isEqualTo(1);valid(target);
            }
        }
    }
    private CtbExportService service(Connection source) throws Exception {
        TenantContext.setCurrentTenant("Fixture");
        var manager=mock(EntityManager.class);var session=mock(Session.class);
        when(manager.unwrap(Session.class)).thenReturn(session);
        when(session.doReturningWork(any())).thenAnswer(call->((ReturningWork<?>)call.getArgument(0)).execute(source));
        var service=new CtbExportService();
        ReflectionTestUtils.setField(service,"entityManager",manager);
        ReflectionTestUtils.setField(service,"applicationPath",directory.toString());
        ReflectionTestUtils.setField(service,"exportingFolderName","exports");
        return service;
    }

    @Test void singleSharedBecomesIndependentRealWithObjectsAndBookmark() throws Exception {
        try (var source = fixture(); var target = target()) {
            try (var st = source.createStatement()) {
                st.execute("INSERT INTO image VALUES (2,0,'left',NULL,X'00FF10',NULL,NULL,NULL)");
                st.execute("INSERT INTO bookmark VALUES (11,1)");
            }
            assertThat(CtbExportSql.copy(source,target,11,false,false)).isEqualTo(1);
            assertThat(number(target,"SELECT master_id FROM children WHERE node_id=11")).isZero();
            assertThat(number(target,"SELECT father_id FROM children WHERE node_id=11")).isZero();
            assertThat(text(target,"SELECT name FROM node WHERE node_id=11")).isEqualTo("Node 2");
            assertThat(text(target,"SELECT hex(png) FROM image WHERE node_id=11")).isEqualTo("00FF10");
            assertThat(number(target,"SELECT COUNT(*) FROM image WHERE anchor IS NULL AND time IS NULL")).isEqualTo(1);
            assertThat(number(target,"SELECT node_id FROM bookmark")).isEqualTo(11);
            valid(target);
        }
    }
    @Test void sharedSubtreeKeepsOwnHierarchyAndMaterializesExternalMaster() throws Exception {
        try (var source=fixture();var target=target()) {
            assertThat(CtbExportSql.copy(source,target,11,true,false)).isEqualTo(7);
            assertThat(number(target,"SELECT COUNT(*) FROM children WHERE node_id=3")).isZero();
            assertThat(number(target,"SELECT father_id FROM children WHERE node_id=19")).isEqualTo(18);
            assertThat(number(target,"SELECT master_id FROM children WHERE node_id=18")).isZero();
            assertThat(text(target,"SELECT name FROM node WHERE node_id=18")).isEqualTo("Node 1");
            valid(target);
        }
    }
    @Test void sharedRootCanKeepItsMasterWhenMasterIsItsOwnDescendant() throws Exception {
        try(var source=fixture();var target=target()) {
            try(var st=source.createStatement()){st.execute("UPDATE children SET father_id=11,sequence=4 WHERE node_id=2");}
            assertThat(CtbExportSql.copy(source,target,11,true,false)).isEqualTo(9);
            assertThat(number(target,"SELECT master_id FROM children WHERE node_id=11")).isEqualTo(2);
            assertThat(number(target,"SELECT father_id FROM children WHERE node_id=2")).isEqualTo(11);
            assertThat(number(target,"SELECT COUNT(*) FROM node WHERE node_id=11")).isZero();
            valid(target);
        }
    }
    @Test void repeatedExternalAliasesShareOneMaterializedContent() throws Exception {
        try (var source=fixture();var target=target()) {
            try(var st=source.createStatement()){st.execute("UPDATE children SET father_id=11,sequence=4 WHERE node_id=10");}
            CtbExportSql.copy(source,target,11,true,false);
            assertThat(number(target,"SELECT master_id FROM children WHERE node_id=10")).isEqualTo(18);
            assertThat(number(target,"SELECT COUNT(*) FROM node WHERE node_id=10")).isZero();
            valid(target);
        }
    }
    @Test void includedMasterKeepsReferencesWithMappedIdsAndRepeatedCollectionExportsDoNotCollide() throws Exception {
        try(var source=fixture();var target=target()) {
            try(var st=source.createStatement()){st.execute("UPDATE children SET father_id=11,sequence=4 WHERE node_id=1");}
            assertThat(CtbExportSql.copy(source,target,11,true,true)).isEqualTo(8);
            assertThat(number(target,"SELECT master_id FROM children WHERE node_id=6")).isEqualTo(8);
            assertThat(number(target,"SELECT COUNT(*) FROM node WHERE node_id=6")).isZero();
            assertThat(text(target,"SELECT name FROM node WHERE node_id=8")).isEqualTo("Node 1");
            CtbExportSql.copy(source,target,11,true,true);
            assertThat(number(target,"SELECT COUNT(*) FROM children")).isEqualTo(16);
            assertThat(number(target,"SELECT COUNT(*) FROM children WHERE father_id=0")).isEqualTo(2);
            valid(target);
        }
    }
    @Test void missingMasterAndCycleAreRejectedAndTransactionCanRollback() throws Exception {
        try(var source=fixture();var target=target()) {
            try(var st=source.createStatement()){st.execute("DELETE FROM node WHERE node_id=1");}
            assertThatThrownBy(()->CtbExportSql.copy(source,target,11,true,true))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            target.rollback();
            assertThat(number(target,"SELECT COUNT(*) FROM children")).isZero();
            try(var st=source.createStatement()){st.execute("UPDATE children SET father_id=19 WHERE node_id=11");}
            assertThatThrownBy(()->CtbExportSql.copy(source,target,11,true,true))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        }
    }
    @Test void realSingleCopiesOnlySelectedNode() throws Exception {
        try(var source=fixture();var target=target()) {
            assertThat(CtbExportSql.copy(source,target,2,false,true)).isEqualTo(1);
            assertThat(text(target,"SELECT name FROM node")).isEqualTo("Node 2");
            valid(target);
        }
    }
    private Connection fixture() throws Exception {
        var connection=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var in=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql");var st=connection.createStatement()) {
            String sql=new String(in.readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$","");
            for(String part:sql.split(";"))if(!part.isBlank())st.execute(part);
        }
        return connection;
    }
    private Connection target() throws Exception {
        var connection=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var st=connection.createStatement()){for(String sql:SqliteSchemaLoader.load().split(";"))if(!sql.isBlank())st.execute(sql);}
        connection.setAutoCommit(false);return connection;
    }
    private void valid(Connection connection) throws Exception {
        assertThat(number(connection,"SELECT COUNT(*) FROM children c LEFT JOIN children p ON p.node_id=c.father_id WHERE c.father_id<>0 AND p.node_id IS NULL")).isZero();
        assertThat(number(connection,"SELECT COUNT(*) FROM children c LEFT JOIN node n ON n.node_id=CASE WHEN c.master_id=0 THEN c.node_id ELSE c.master_id END WHERE n.node_id IS NULL")).isZero();
        assertThat(number(connection,"SELECT COUNT(*) FROM children c JOIN children m ON m.node_id=c.master_id WHERE c.master_id<>0 AND m.master_id<>0")).isZero();
    }
    private long number(Connection connection,String sql)throws Exception{try(var st=connection.createStatement();var rs=st.executeQuery(sql)){assertThat(rs.next()).isTrue();return rs.getLong(1);}}
    private String text(Connection connection,String sql)throws Exception{try(var st=connection.createStatement();var rs=st.executeQuery(sql)){assertThat(rs.next()).isTrue();return rs.getString(1);}}
}

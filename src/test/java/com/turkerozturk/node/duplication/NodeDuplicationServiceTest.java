package com.turkerozturk.node.duplication;

import com.turkerozturk.export.SqliteSchemaLoader;
import com.turkerozturk.node.properties.NodePropertiesService;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeDuplicationServiceTest {
    private Connection connection;
    @BeforeEach void setup() throws Exception {
        connection=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var statement=connection.createStatement()) {
            for(String sql:SqliteSchemaLoader.load().split(";")) if(!sql.isBlank()) statement.execute(sql);
            statement.execute("INSERT INTO node VALUES(1,'Root','<node><rich_text>Original</rich_text></node>','custom-colors','tags',29,123,1,1,1,0,10,20)");
            statement.execute("INSERT INTO node VALUES(2,'Other','target','plain-text','',0,0,0,0,0,0,10,20)");
            statement.execute("INSERT INTO node VALUES(3,'Child','webs https://example.test/','plain-text','',0,0,0,0,0,1,10,20)");
            statement.execute("INSERT INTO children VALUES(1,0,10,0),(2,0,20,0),(3,1,10,0),(10,1,20,3),(11,1,30,2),(12,0,30,1)");
            statement.execute("INSERT INTO bookmark VALUES(1,1),(10,2)");
            statement.execute("ALTER TABLE image ADD COLUMN extra BLOB");
            statement.execute("INSERT INTO image VALUES(1,1,'left','',X'0001FF80','file.bin','webs https://example.test',77,X'ABCD')");
            statement.execute("INSERT INTO image VALUES(1,2,'right','anchor',NULL,NULL,'',NULL,NULL)");
            statement.execute("INSERT INTO grid VALUES(1,3,'center','<table><row><cell>Cell</cell></row></table>',80,90)");
            statement.execute("INSERT INTO codebox VALUES(1,4,'right','print(1)','python',100,50,1,1,1)");
        }
        connection.setAutoCommit(false);
    }
    @AfterEach void close() throws Exception {connection.close();}
    private long duplicate(long id,boolean descendants) throws Exception {
        var state=NodeDuplicationService.state(NodeDuplicationService.read(connection),id);
        return NodeDuplicationService.duplicate(connection,id,descendants,state.revision(),1000);
    }
    private long number(String sql) throws Exception {
        try(var statement=connection.createStatement();var row=statement.executeQuery(sql)) {assertThat(row.next()).isTrue();return row.getLong(1);}
    }
    @Test void singleCopyPreservesContentPropertiesAndAddsSiblingWithFreshTimes() throws Exception {
        long id=duplicate(1,false);assertThat(id).isEqualTo(13);
        assertThat(number("SELECT father_id FROM children WHERE node_id=13")).isZero();
        assertThat(number("SELECT sequence FROM children WHERE node_id=13")).isEqualTo(2);
        assertThat(number("SELECT sequence FROM children WHERE node_id=2")).isEqualTo(3);
        assertThat(number("SELECT COUNT(*) FROM children WHERE father_id=13")).isZero();
        assertThat(number("SELECT ts_creation FROM node WHERE node_id=13")).isEqualTo(1000);
        assertThat(number("SELECT ts_lastsave FROM node WHERE node_id=13")).isEqualTo(1000);
        assertThat(number("SELECT ts_lastsave FROM node WHERE node_id=1")).isEqualTo(20);
        assertThat(number("SELECT is_ro FROM node WHERE node_id=13")).isEqualTo(29);
        assertThat(number("SELECT is_richtxt FROM node WHERE node_id=13")).isEqualTo(123);
        assertThat(number("SELECT COUNT(*) FROM node a JOIN node b ON a.node_id=1 AND b.node_id=13 WHERE a.txt=b.txt AND a.name=b.name AND a.tags=b.tags AND a.syntax=b.syntax")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM bookmark")).isEqualTo(2);
    }
    @Test void copiesBinaryNullAndObjectMetadataWithoutParsingOrConverting() throws Exception {
        long id=duplicate(1,false);
        try(var statement=connection.createStatement();var row=statement.executeQuery("SELECT png,filename,link,time,extra FROM image WHERE node_id="+id+" AND offset=1")) {
            assertThat(row.next()).isTrue();assertThat(row.getBytes(1)).containsExactly((byte)0,(byte)1,(byte)255,(byte)128);
            assertThat(row.getString(2)).isEqualTo("file.bin");assertThat(row.getString(3)).isEqualTo("webs https://example.test");
            assertThat(row.getLong(4)).isEqualTo(77);assertThat(row.getBytes(5)).containsExactly((byte)171,(byte)205);
        }
        assertThat(number("SELECT COUNT(*) FROM image WHERE node_id=13 AND offset=2 AND png IS NULL AND filename IS NULL AND time IS NULL AND extra IS NULL")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM grid WHERE node_id=13 AND offset=3 AND justification='center' AND col_min=80 AND col_max=90")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM codebox WHERE node_id=13 AND offset=4 AND syntax='python' AND width=100 AND do_show_linenum=1")).isEqualTo(1);
    }
    @Test void subtreeCopiesRealNodesAndKeepsAliasesLinkedToOriginalMasters() throws Exception {
        assertThat(NodeDuplicationService.state(NodeDuplicationService.read(connection),1).subtreeCount()).isEqualTo(4);
        duplicate(1,true);
        assertThat(number("SELECT father_id FROM children WHERE node_id=14")).isEqualTo(13);
        assertThat(number("SELECT master_id FROM children WHERE node_id=15")).isEqualTo(3);
        assertThat(number("SELECT master_id FROM children WHERE node_id=16")).isEqualTo(2);
        assertThat(number("SELECT master_id FROM children WHERE node_id=12")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id IN (13,14,15,16)")).isEqualTo(2);
        assertThat(number("SELECT father_id FROM children WHERE node_id=3")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM children WHERE father_id=13")).isEqualTo(3);
    }
    @Test void aliasCopyCreatesOnlyAnotherReference() throws Exception {
        long id=duplicate(10,false);
        assertThat(number("SELECT master_id FROM children WHERE node_id="+id)).isEqualTo(3);
        assertThat(number("SELECT father_id FROM children WHERE node_id="+id)).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM node")).isEqualTo(3);
        assertThat(number("SELECT COUNT(*) FROM image")).isEqualTo(2);
        long subtree=duplicate(10,true);
        assertThat(number("SELECT master_id FROM children WHERE node_id="+subtree)).isEqualTo(3);
    }
    @Test void staleTreeRejectsCopyBeforeAllocatingRows() throws Exception {
        String revision=NodeDuplicationService.state(NodeDuplicationService.read(connection),1).revision();
        try(var statement=connection.createStatement()){statement.execute("UPDATE children SET sequence=11 WHERE node_id=1");}
        assertThatThrownBy(()->NodeDuplicationService.duplicate(connection,1,true,revision,1000)).isInstanceOf(ResponseStatusException.class);
        assertThat(number("SELECT COUNT(*) FROM node")).isEqualTo(3);
    }
    @Test void missingParentIsRejectedWithoutOrphans() throws Exception {
        try(var statement=connection.createStatement()){statement.execute("UPDATE children SET father_id=99 WHERE node_id=1");}
        assertThatThrownBy(()->NodeDuplicationService.state(NodeDuplicationService.read(connection),1)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->duplicate(1,true)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->duplicate(3,false)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->duplicate(10,false)).isInstanceOf(ResponseStatusException.class);
        assertThat(number("SELECT COUNT(*) FROM node")).isEqualTo(3);
    }
    @Test void statesAndSingleCopiesAcceptSharedParentsAndSharedOccurrencesWithChildren() throws Exception {
        try(var statement=connection.createStatement()) {
            statement.execute("UPDATE children SET father_id=10 WHERE node_id=3");
        }
        var snapshot=NodeDuplicationService.read(connection);
        for(long id:new long[]{1,2,3,10,11,12})
            assertThat(NodeDuplicationService.state(snapshot,id).revision()).isNotBlank();
        // An alias can parent its own master occurrence without a hierarchy cycle.
        assertThat(NodeDuplicationService.state(snapshot,1).subtreeCount()).isEqualTo(4);
        long copy=duplicate(3,false);
        assertThat(number("SELECT father_id FROM children WHERE node_id="+copy)).isEqualTo(10);
        long alias=duplicate(10,false);
        assertThat(number("SELECT master_id FROM children WHERE node_id="+alias)).isEqualTo(3);
        assertThat(number("SELECT COUNT(*) FROM children WHERE father_id="+alias)).isZero();
        assertThat(number("SELECT father_id FROM children WHERE node_id=3")).isEqualTo(10);
    }

    @Test void identifierExhaustionRefusesWrite() throws Exception {
        try(var statement=connection.createStatement()){statement.execute("UPDATE children SET node_id=9223372036854775807 WHERE node_id=12");}
        assertThatThrownBy(()->duplicate(1,true)).isInstanceOf(ResponseStatusException.class);
        assertThat(number("SELECT COUNT(*) FROM node")).isEqualTo(3);
    }
    @Test void payloadInsertFailureCanRollBackTheWholeDuplicateTransaction() throws Exception {
        try(var statement=connection.createStatement()){statement.execute("CREATE TRIGGER reject_copied_image BEFORE INSERT ON image WHEN NEW.node_id>12 BEGIN SELECT RAISE(ABORT,'test'); END");}
        assertThatThrownBy(()->duplicate(1,false)).isInstanceOf(SQLException.class);
        connection.rollback();
        assertThat(number("SELECT COUNT(*) FROM node")).isEqualTo(3);
        assertThat(number("SELECT COUNT(*) FROM children")).isEqualTo(6);
    }
    @Test void writableGuardBlocksBothStateAndDuplication() {
        var properties=mock(NodePropertiesService.class);var service=new NodeDuplicationService(properties);
        when(properties.writable()).thenReturn(false);
        assertThatThrownBy(()->service.state(1)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->service.duplicate(1,true,"revision")).isInstanceOf(AccessDeniedException.class);
    }
    @Test void sharedCreationAddsOnlySiblingOccurrenceAndPreservesMasterPayloads() throws Exception {
        String revision=NodeDuplicationService.state(NodeDuplicationService.read(connection),1).revision();
        long id=NodeDuplicationService.createShared(connection,1,revision);
        assertThat(id).isEqualTo(13);
        assertThat(number("SELECT master_id FROM children WHERE node_id=13")).isEqualTo(1);
        assertThat(number("SELECT father_id FROM children WHERE node_id=13")).isZero();
        assertThat(number("SELECT sequence FROM children WHERE node_id=13")).isEqualTo(2);
        assertThat(number("SELECT sequence FROM children WHERE node_id=2")).isEqualTo(3);
        assertThat(number("SELECT COUNT(*) FROM node")).isEqualTo(3);
        assertThat(number("SELECT COUNT(*) FROM image")).isEqualTo(2);
        assertThat(number("SELECT COUNT(*) FROM grid")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM codebox")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM bookmark")).isEqualTo(2);
        assertThat(number("SELECT ts_lastsave FROM node WHERE node_id=1")).isEqualTo(20);
    }
    @Test void sharedCreationFromAliasPointsDirectlyToRealMaster() throws Exception {
        String revision=NodeDuplicationService.state(NodeDuplicationService.read(connection),10).revision();
        long id=NodeDuplicationService.createShared(connection,10,revision);
        assertThat(number("SELECT master_id FROM children WHERE node_id="+id)).isEqualTo(3);
        assertThat(number("SELECT father_id FROM children WHERE node_id="+id)).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id="+id)).isZero();
    }
    @Test void staleRevisionRejectsSharedCreationBeforeAnyChange() throws Exception {
        String revision=NodeDuplicationService.state(NodeDuplicationService.read(connection),1).revision();
        try(var statement=connection.createStatement()) {statement.execute("UPDATE children SET sequence=11 WHERE node_id=1");}
        assertThatThrownBy(()->NodeDuplicationService.createShared(connection,1,revision)).isInstanceOf(ResponseStatusException.class);
        assertThat(number("SELECT COUNT(*) FROM children")).isEqualTo(6);
    }
    @Test void readOnlyTenantRejectsSharedCreation() {
        var properties=mock(NodePropertiesService.class);
        when(properties.writable()).thenReturn(false);
        assertThatThrownBy(()->new NodeDuplicationService(properties).createShared(1,"revision")).isInstanceOf(AccessDeniedException.class);
    }
}

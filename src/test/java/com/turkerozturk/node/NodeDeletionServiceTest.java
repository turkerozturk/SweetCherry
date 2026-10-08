package com.turkerozturk.node;

import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeDeletionServiceTest {
    private Connection connection;
    @BeforeEach void setup() throws Exception {
        connection=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var script=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql");var statement=connection.createStatement()) {
            String sql=new String(script.readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
            for(String part:sql.split(";")) if(!part.isBlank()) statement.execute(part);
            statement.execute("INSERT INTO bookmark VALUES(6,1),(16,2),(8,3),(17,4),(7,5)");
            statement.execute("ALTER TABLE image ADD COLUMN extra BLOB");
            statement.execute("INSERT INTO image VALUES(6,1,'left','',X'0001FF80','file.bin','webs https://example.test',77,X'ABCD')");
            statement.execute("INSERT INTO image VALUES(6,2,'right','anchor',NULL,NULL,'',NULL,NULL)");
            statement.execute("INSERT INTO grid VALUES(6,3,'center','<table><row><cell>Cell</cell></row></table>',80,90)");
            statement.execute("INSERT INTO codebox VALUES(6,4,'right','print(1)','python',100,50,1,1,1)");
        }
        connection.setAutoCommit(false);
    }
    @AfterEach void close() throws Exception {connection.close();TenantContext.clear();}
    private long number(String sql) throws Exception {
        try(var statement=connection.createStatement();var result=statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();return result.getLong(1);
        }
    }
    private void validReferences() throws Exception {
        assertThat(number("SELECT COUNT(*) FROM children c LEFT JOIN children p ON p.node_id=c.father_id WHERE c.father_id!=0 AND p.node_id IS NULL")).isZero();
        assertThat(number("SELECT COUNT(*) FROM children c LEFT JOIN node n ON n.node_id=CASE WHEN c.master_id=0 THEN c.node_id ELSE c.master_id END WHERE n.node_id IS NULL")).isZero();
        assertThat(number("SELECT COUNT(*) FROM bookmark b LEFT JOIN children c ON c.node_id=b.node_id WHERE c.node_id IS NULL")).isZero();
    }
    @Test void deletesSharedSubtreeAndPromotesDescendantMasterOutsideIt() throws Exception {
        NodeDeletionSql.delete(connection,16);
        assertThat(number("SELECT COUNT(*) FROM children WHERE node_id IN(16,7,8)")).isZero();
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=6")).isEqualTo(1);
        assertThat(number("SELECT master_id FROM children WHERE node_id=17")).isZero();
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=17 AND name='Node 8'")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM bookmark WHERE node_id=17")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM bookmark WHERE node_id IN(16,7,8)")).isZero();
        validReferences();
    }
    @Test void deletingMasterPromotesSurvivorAndPreservesItsChildrenObjectsAndBookmark() throws Exception {
        NodeDeletionSql.delete(connection,6);
        assertThat(number("SELECT master_id FROM children WHERE node_id=16")).isZero();
        assertThat(number("SELECT father_id FROM children WHERE node_id=7")).isEqualTo(16);
        assertThat(number("SELECT father_id FROM children WHERE node_id=8")).isEqualTo(16);
        assertThat(number("SELECT COUNT(*) FROM bookmark WHERE node_id=16")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM bookmark WHERE node_id=6")).isZero();
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=16 AND name='Node 6'")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM grid WHERE node_id=16 AND col_max=90")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM codebox WHERE node_id=16 AND txt='print(1)' AND do_show_linenum=1")).isEqualTo(1);
        try(var statement=connection.createStatement();var result=statement.executeQuery("SELECT png,extra,time FROM image WHERE node_id=16 AND offset=1")) {
            assertThat(result.next()).isTrue();assertThat(result.getBytes(1)).containsExactly((byte)0,(byte)1,(byte)255,(byte)128);
            assertThat(result.getBytes(2)).containsExactly((byte)171,(byte)205);assertThat(result.getLong(3)).isEqualTo(77);
        }
        assertThat(number("SELECT COUNT(*) FROM image WHERE node_id=16 AND offset=2 AND png IS NULL AND filename IS NULL AND time IS NULL AND extra IS NULL")).isEqualTo(1);
        validReferences();
    }
    @Test void removesOnlyTheSelectedHierarchyAndRetargetsOtherSurvivors() throws Exception {
        NodeDeletionSql.delete(connection,1);
        assertThat(number("SELECT master_id FROM children WHERE node_id=10")).isZero();
        assertThat(number("SELECT master_id FROM children WHERE node_id=18")).isEqualTo(10);
        assertThat(number("SELECT father_id FROM children WHERE node_id=19")).isEqualTo(18);
        validReferences();
    }
    @Test void sharedRootWithNestedSharedChildrenDoesNotDeleteTheirOutsideMasters() throws Exception {
        NodeDeletionSql.delete(connection,11);
        assertThat(number("SELECT COUNT(*) FROM children WHERE node_id IN(11,12,13,14,15,18,19)")).isZero();
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id IN(1,2,3)")).isEqualTo(3);
        assertThat(number("SELECT master_id FROM children WHERE node_id=10")).isEqualTo(1);
        validReferences();
    }
    @Test void allGroupMembersInsideDeletedSubtreeRemoveThePayloadCompletely() throws Exception {
        try(var statement=connection.createStatement()) {
            statement.execute("INSERT INTO node VALUES(100,'Delete group','content','plain-text','',0,0,0,0,0,0,0,0)");
            statement.execute("INSERT INTO children VALUES(100,0,20,0)");
            statement.execute("UPDATE children SET father_id=100 WHERE node_id IN(1,10,18)");
        }
        NodeDeletionSql.delete(connection,100);
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=1")).isZero();
        assertThat(number("SELECT COUNT(*) FROM children WHERE node_id IN(1,10,18,19,100)")).isZero();
        validReferences();
    }
    @Test void aliasBelowItsOwnMasterCanBeDeletedWithoutRemovingMaster() throws Exception {
        try(var statement=connection.createStatement()) {statement.execute("UPDATE children SET father_id=1 WHERE node_id=10");}
        NodeDeletionSql.delete(connection,10);
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=1")).isEqualTo(1);
        assertThat(number("SELECT master_id FROM children WHERE node_id=18")).isEqualTo(1);
        validReferences();
    }
    @Test void sharedParentCanContainItsOwnMasterWithoutFollowingTheReferenceAsATreeEdge() throws Exception {
        try(var statement=connection.createStatement()) {
            statement.execute("UPDATE children SET father_id=17 WHERE node_id=8");
        }
        NodeDeletionSql.delete(connection,17);
        assertThat(number("SELECT COUNT(*) FROM children WHERE node_id IN(8,17)")).isZero();
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=8")).isZero();
        validReferences();
    }
    @Test void malformedHierarchyRejectsBeforeAnyDeletionOrPromotion() throws Exception {
        try(var statement=connection.createStatement()) {statement.execute("UPDATE children SET father_id=7 WHERE node_id=16");}
        assertThatThrownBy(()->NodeDeletionSql.delete(connection,6)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=6")).isEqualTo(1);
        assertThat(number("SELECT master_id FROM children WHERE node_id=16")).isEqualTo(6);
    }
    @Test void failedPromotionCanRollBackPayloadAndHierarchyTogether() throws Exception {
        connection.commit();
        try(var statement=connection.createStatement()) {
            statement.execute("CREATE TRIGGER fail_promotion BEFORE UPDATE OF node_id ON image BEGIN SELECT RAISE(ABORT,'test'); END");
        }
        assertThatThrownBy(()->NodeDeletionSql.delete(connection,6)).isInstanceOf(SQLException.class);
        connection.rollback();
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=6")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=16")).isZero();
        assertThat(number("SELECT master_id FROM children WHERE node_id=16")).isEqualTo(6);
        validReferences();
    }
    @Test void nextSelectionPrefersPreviousSiblingIncludingSharedOccurrence() throws Exception {
        assertThat(NodeDeletionSql.delete(connection,9)).isEqualTo(16);
    }
    @Test void firstSiblingDeletionSelectsNextSibling() throws Exception {
        assertThat(NodeDeletionSql.delete(connection,1)).isEqualTo(2);
    }
    @Test void onlyChildDeletionSelectsSharedParent() throws Exception {
        assertThat(NodeDeletionSql.delete(connection,19)).isEqualTo(18);
    }
    @Test void deletingLastRootReturnsVirtualRootWithoutADeadNodeId() throws Exception {
        try(var statement=connection.createStatement()) {
            statement.execute("DELETE FROM bookmark");statement.execute("DELETE FROM image");
            statement.execute("DELETE FROM grid");statement.execute("DELETE FROM codebox");
            statement.execute("DELETE FROM children WHERE node_id!=4");
            statement.execute("DELETE FROM node WHERE node_id!=4");
            statement.execute("UPDATE children SET father_id=0 WHERE node_id=4");
        }
        assertThat(NodeDeletionSql.delete(connection,4)).isZero();
        assertThat(number("SELECT COUNT(*) FROM children")).isZero();
    }
    @Test void readOnlyServiceDoesNotOpenTheTransactionConnection() {
        TenantContext.setCurrentTenant("readonly");var properties=new CustomPropertiesHolder();
        properties.addCustomProperties("readonly",Map.of("custom.isWritable","false"));
        var service=new NodeDeletionService(properties,mock(NodeRepository.class),
                mock(com.turkerozturk.children.ChildrenRepository.class),mock(com.turkerozturk.children.ChildrenService.class));
        var manager=mock(jakarta.persistence.EntityManager.class);ReflectionTestUtils.setField(service,"entityManager",manager);
        assertThatThrownBy(()->service.deleteNodeWithSubNodes(6)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(manager);
    }
}

package com.turkerozturk.node.moving;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import com.turkerozturk.node.properties.NodePropertiesService;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeMoveServiceTest {
    private Connection connection;
    @BeforeEach void setup() throws Exception {
        connection=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var statement=connection.createStatement()) {
            statement.execute("CREATE TABLE children(node_id INTEGER PRIMARY KEY,father_id INTEGER,sequence INTEGER,master_id INTEGER)");
            statement.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,txt TEXT)");
            statement.execute("CREATE TABLE bookmark(node_id INTEGER)");
            statement.execute("INSERT INTO node VALUES(1,'content'),(2,'second'),(3,'third'),(4,'child')");
            statement.execute("INSERT INTO children VALUES(1,0,10,0),(2,0,20,0),(3,0,30,0),(4,1,5,0),(9,0,40,1)");
            statement.execute("INSERT INTO bookmark VALUES(2),(9)");
            statement.execute("ALTER TABLE node ADD COLUMN ts_lastsave INTEGER DEFAULT 100");
        }
        connection.setAutoCommit(false);
    }
    @AfterEach void close() throws Exception {connection.close();}
    private void move(long id, NodeMoveService.Direction direction) throws Exception {
        NodeMoveService.move(connection,id,direction,NodeMoveService.state(NodeMoveService.read(connection),id).revision());
    }
    private void location(long id,long parent,long sequence) throws Exception {
        var row=NodeMoveService.read(connection).stream().filter(item->item.id()==id).findFirst().orElseThrow();
        assertThat(row.parent()).isEqualTo(parent);assertThat(row.sequence()).isEqualTo(sequence);
    }
    @Test void swapsUpAndDownAndNormalizesSiblingSequence() throws Exception {
        move(2,NodeMoveService.Direction.UP);location(2,0,1);location(1,0,2);
        move(2,NodeMoveService.Direction.DOWN);location(2,0,2);location(1,0,1);
    }
    @Test void rightAppendsToPreviousRealSiblingWithoutMovingItsSubtree() throws Exception {
        move(2,NodeMoveService.Direction.RIGHT);location(2,1,2);location(4,1,1);location(3,0,2);
    }
    @Test void leftPlacesNodeImmediatelyAfterParent() throws Exception {
        move(4,NodeMoveService.Direction.LEFT);location(4,0,2);location(2,0,3);
    }
    @Test void aliasMovesOnlyItsOccurrenceAndCanBecomeParent() throws Exception {
        move(9,NodeMoveService.Direction.UP);location(9,0,3);location(1,0,1);location(4,1,5);
        var state=NodeMoveService.state(NodeMoveService.read(connection),3);
        assertThat(state.right()).isTrue();
        move(3,NodeMoveService.Direction.RIGHT);location(3,9,1);
        assertThat(NodeMoveService.read(connection).stream().filter(row->row.id()==9).findFirst().orElseThrow().master()).isEqualTo(1);
    }
    @Test void sharedParentsSupportSiblingAndOutdentMovesWithoutChangingContentReferences() throws Exception {
        try(var statement=connection.createStatement()) {
            statement.execute("UPDATE children SET father_id=9 WHERE node_id=4");
            statement.execute("INSERT INTO children VALUES(10,9,6,2),(11,10,1,3)");
        }
        move(10,NodeMoveService.Direction.UP);location(10,9,1);location(4,9,2);
        move(10,NodeMoveService.Direction.DOWN);location(10,9,2);
        move(11,NodeMoveService.Direction.LEFT);location(11,9,3);
        move(9,NodeMoveService.Direction.UP);location(9,0,3);location(4,9,1);
        move(4,NodeMoveService.Direction.LEFT);location(4,0,4);
        assertThat(NodeMoveService.read(connection).stream().filter(row->row.id()==11).findFirst().orElseThrow().master()).isEqualTo(3);
    }

    @Test void boundariesAndStaleSnapshotsRejectWithoutFurtherWrites() throws Exception {
        var state=NodeMoveService.state(NodeMoveService.read(connection),1);
        assertThat(state.up()).isFalse();assertThat(state.left()).isFalse();assertThat(state.right()).isFalse();
        assertThatThrownBy(()->move(1,NodeMoveService.Direction.UP)).isInstanceOf(ResponseStatusException.class);
        move(2,NodeMoveService.Direction.UP);
        var before=NodeMoveService.read(connection);
        assertThatThrownBy(()->NodeMoveService.move(connection,1,NodeMoveService.Direction.DOWN,state.revision())).isInstanceOf(ResponseStatusException.class);
        assertThat(NodeMoveService.read(connection)).isEqualTo(before);
    }
    @Test void preservesContentAndBookmarksWhenMovingRealSubtree() throws Exception {
        move(1,NodeMoveService.Direction.DOWN);move(1,NodeMoveService.Direction.RIGHT);location(1,2,1);location(4,1,5);
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT txt FROM node WHERE node_id=1")) {
            assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("content");
        }
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT COUNT(*) FROM bookmark")) {
            assertThat(rows.next()).isTrue();assertThat(rows.getInt(1)).isEqualTo(2);
        }
    }
    @Test void refusesCyclesAndMissingParents() throws Exception {
        try(var statement=connection.createStatement()){statement.execute("UPDATE children SET father_id=4 WHERE node_id=1");}
        assertThatThrownBy(()->NodeMoveService.read(connection)).isInstanceOf(ResponseStatusException.class);
        connection.rollback();
        try(var statement=connection.createStatement()){statement.execute("UPDATE children SET father_id=99 WHERE node_id=4");}
        assertThatThrownBy(()->NodeMoveService.read(connection)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void writableGuardAppliesToReadsAndWrites() {
        var properties=mock(NodePropertiesService.class);var service=new NodeMoveService(properties);
        when(properties.writable()).thenReturn(false);
        assertThatThrownBy(()->service.state(1)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->service.move(1,NodeMoveService.Direction.UP,"revision")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->service.moveTo(1,2,"revision")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->service.bookmarkTargets(1)).isInstanceOf(AccessDeniedException.class);
    }

    @Test void movingSharedOccurrenceTouchesItsMasterButNotOtherContent() throws Exception {
        long before=java.time.Instant.now().getEpochSecond();
        move(9,NodeMoveService.Direction.UP);
        try(var statement=connection.createStatement();var row=statement.executeQuery("SELECT ts_lastsave FROM node WHERE node_id=1")) {
            assertThat(row.next()).isTrue();assertThat(row.getLong(1)).isBetween(before,java.time.Instant.now().getEpochSecond());
        }
        try(var statement=connection.createStatement();var row=statement.executeQuery("SELECT ts_lastsave FROM node WHERE node_id=3")) {
            assertThat(row.next()).isTrue();assertThat(row.getLong(1)).isEqualTo(100);
        }
    }
    @Test void realMoveTouchesSelectedContentAndRejectedMoveDoesNot() throws Exception {
        assertThatThrownBy(()->move(1,NodeMoveService.Direction.UP)).isInstanceOf(ResponseStatusException.class);
        try(var statement=connection.createStatement();var row=statement.executeQuery("SELECT ts_lastsave FROM node WHERE node_id=1")) {
            row.next();assertThat(row.getLong(1)).isEqualTo(100);
        }
        move(1,NodeMoveService.Direction.DOWN);
        try(var statement=connection.createStatement();var row=statement.executeQuery("SELECT ts_lastsave FROM node WHERE node_id=1")) {
            row.next();assertThat(row.getLong(1)).isGreaterThan(100);
        }
    }

    private void moveTo(long id, long parent) throws Exception {
        NodeMoveService.moveTo(connection,id,parent,NodeMoveService.state(NodeMoveService.read(connection),id).revision());
    }
    @Test void chosenParentGetsEntireRealSubtreeLastAndOnlyMovedContentTimestampChanges() throws Exception {
        try(var statement=connection.createStatement()){statement.execute("INSERT INTO children VALUES(10,2,12,3)");}
        moveTo(1,2);location(1,2,2);location(10,2,1);location(4,1,5);location(2,0,1);
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT node_id,txt,ts_lastsave FROM node ORDER BY node_id")) {
            rows.next();assertThat(rows.getString(2)).isEqualTo("content");assertThat(rows.getLong(3)).isGreaterThan(100);
            while(rows.next())assertThat(rows.getLong(3)).isEqualTo(100);
        }
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT node_id FROM bookmark ORDER BY node_id")) {
            rows.next();assertThat(rows.getLong(1)).isEqualTo(2);rows.next();assertThat(rows.getLong(1)).isEqualTo(9);assertThat(rows.next()).isFalse();
        }
    }
    @Test void chosenSharedParentUsesOccurrenceIdAndSharedSourceKeepsItsOwnChildren() throws Exception {
        moveTo(4,9);location(4,9,1);
        moveTo(9,2);location(9,2,1);location(4,9,1);location(1,0,1);
        assertThat(NodeMoveService.read(connection).stream().filter(row->row.id()==9).findFirst().orElseThrow().master()).isEqualTo(1);
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT ts_lastsave FROM node WHERE node_id=1")) {
            rows.next();assertThat(rows.getLong(1)).isGreaterThan(100);
        }
    }
    @Test void chosenRootAppendsButExistingParentDoesNotReorderOrTouchDates() throws Exception {
        var before=NodeMoveService.read(connection);moveTo(2,0);assertThat(NodeMoveService.read(connection)).isEqualTo(before);
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT ts_lastsave FROM node WHERE node_id=2")) {
            rows.next();assertThat(rows.getLong(1)).isEqualTo(100);
        }
        moveTo(4,0);location(4,0,5);
    }
    @Test void invalidChosenParentsAndStaleRevisionNeverWrite() throws Exception {
        var before=NodeMoveService.read(connection);var revision=NodeMoveService.state(before,1).revision();
        for(long parent:new long[]{1,4,999,-1}) {
            assertThatThrownBy(()->NodeMoveService.moveTo(connection,1,parent,revision)).isInstanceOf(ResponseStatusException.class);
            assertThat(NodeMoveService.read(connection)).isEqualTo(before);
        }
        assertThatThrownBy(()->NodeMoveService.moveTo(connection,1,2,"stale")).isInstanceOf(ResponseStatusException.class);
        assertThat(NodeMoveService.read(connection)).isEqualTo(before);
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT ts_lastsave FROM node WHERE node_id=1")) {
            rows.next();assertThat(rows.getLong(1)).isEqualTo(100);
        }
    }
    @Test void sharedMasterRelationshipAloneIsNotAHierarchyCycle() throws Exception {
        moveTo(1,9);location(1,9,1);location(4,1,5);
        // Now 9 really is an ancestor of 4; moving it there must be rejected.
        var before=NodeMoveService.read(connection);
        assertThatThrownBy(()->moveTo(9,4)).isInstanceOf(ResponseStatusException.class);
        assertThat(NodeMoveService.read(connection)).isEqualTo(before);
    }
    @Test void missingSharedTargetMasterRejectsBeforeRenumberingEitherGroup() throws Exception {
        try(var statement=connection.createStatement()){statement.execute("UPDATE children SET master_id=999 WHERE node_id=9");}
        var before=NodeMoveService.read(connection);
        assertThatThrownBy(()->moveTo(2,9)).isInstanceOf(ResponseStatusException.class);
        assertThat(NodeMoveService.read(connection)).isEqualTo(before);
    }

    private void bookmarkMetadataColumns() throws Exception {
        try(var statement=connection.createStatement()) {
            statement.execute("ALTER TABLE node ADD COLUMN name TEXT");
            statement.execute("ALTER TABLE node ADD COLUMN is_ro INTEGER DEFAULT 0");
            statement.execute("ALTER TABLE node ADD COLUMN is_richtxt INTEGER DEFAULT 0");
            statement.execute("ALTER TABLE node ADD COLUMN syntax TEXT DEFAULT 'plain-text'");
            statement.execute("ALTER TABLE bookmark ADD COLUMN sequence INTEGER DEFAULT 1");
        }
    }
    @Test void bookmarkTargetsUseOccurrenceIdsAndMasterStylingInBookmarkOrderWithoutReadingContent() throws Exception {
        bookmarkMetadataColumns();
        try(var statement=connection.createStatement()) {
            statement.execute("UPDATE node SET name='Java & <notes>',is_ro=49,is_richtxt="+(0x3584E4L*8+7)+" WHERE node_id=1");
            statement.execute("UPDATE node SET name='Second' WHERE node_id=2");
            statement.execute("UPDATE bookmark SET sequence=2 WHERE node_id=2");
            statement.execute("INSERT INTO bookmark VALUES(999,3)");
            statement.execute("INSERT INTO children VALUES(10,0,50,999)");
            statement.execute("INSERT INTO bookmark VALUES(10,4)");
            // The picker must work even when the note text column is absent.
            statement.execute("ALTER TABLE node DROP COLUMN txt");
        }
        var before=NodeMoveService.read(connection);
        var targets=NodeMoveService.bookmarkTargets(connection,4);
        assertThat(targets).extracting(NodeMoveService.BookmarkTarget::nodeId).containsExactly("9","2");
        assertThat(targets.get(0)).isEqualTo(new NodeMoveService.BookmarkTarget("9","Java & <notes>","ct_java",true,"#3584E4"));
        assertThat(targets.get(1).bold()).isFalse();assertThat(targets.get(1).color()).isEqualTo("#000000");
        assertThat(NodeMoveService.read(connection)).isEqualTo(before);
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT MIN(ts_lastsave),MAX(ts_lastsave) FROM node")) {
            rows.next();assertThat(rows.getLong(1)).isEqualTo(100);assertThat(rows.getLong(2)).isEqualTo(100);
        }
    }
    @Test void noBookmarksReturnsEmptyButDeletedSourceIsRejected() throws Exception {
        bookmarkMetadataColumns();
        try(var statement=connection.createStatement()){statement.execute("DELETE FROM bookmark");}
        assertThat(NodeMoveService.bookmarkTargets(connection,1)).isEmpty();
        assertThatThrownBy(()->NodeMoveService.bookmarkTargets(connection,999)).isInstanceOf(ResponseStatusException.class);
    }

}

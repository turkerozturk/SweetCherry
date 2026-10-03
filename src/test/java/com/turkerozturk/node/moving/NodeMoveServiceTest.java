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
    @Test void aliasMovesOnlyItsOccurrenceAndCannotBecomeParent() throws Exception {
        move(9,NodeMoveService.Direction.UP);location(9,0,3);location(1,0,1);location(4,1,5);
        var state=NodeMoveService.state(NodeMoveService.read(connection),3);
        assertThat(state.right()).isFalse();
        assertThatThrownBy(()->move(3,NodeMoveService.Direction.RIGHT)).isInstanceOf(ResponseStatusException.class);
        assertThat(NodeMoveService.read(connection).stream().filter(row->row.id()==9).findFirst().orElseThrow().master()).isEqualTo(1);
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
    }
}

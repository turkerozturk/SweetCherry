package com.turkerozturk.bookmark;

import com.turkerozturk.export.SqliteSchemaLoader;
import com.turkerozturk.node.properties.NodePropertiesService;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookmarkWriteServiceTest {
    private Connection connection;
    @BeforeEach void setup() throws Exception {
        connection=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var statement=connection.createStatement()) {
            for(String sql:SqliteSchemaLoader.load().split(";")) if(!sql.isBlank()) statement.execute(sql);
            statement.execute("INSERT INTO node(node_id,txt,is_ro,ts_lastsave) VALUES(1,'content',1,100),(2,'other',0,200)");
            statement.execute("INSERT INTO children VALUES(1,0,1,0),(2,0,2,0),(10,0,3,1),(11,0,4,99)");
        }
    }
    @AfterEach void close() throws Exception {connection.close();}
    private long number(String sql) throws Exception {
        try(var statement=connection.createStatement();var row=statement.executeQuery(sql)){assertThat(row.next()).isTrue();return row.getLong(1);}
    }
    @Test void appendsBookmarksAndRepeatedAddsKeepTheExistingOrder() throws Exception {
        BookmarkWriteService.add(connection,2);BookmarkWriteService.add(connection,1);BookmarkWriteService.add(connection,2);
        assertThat(number("SELECT COUNT(*) FROM bookmark")).isEqualTo(2);
        assertThat(number("SELECT sequence FROM bookmark WHERE node_id=2")).isEqualTo(1);
        assertThat(number("SELECT sequence FROM bookmark WHERE node_id=1")).isEqualTo(2);
    }
    @Test void sharedOccurrenceAndMasterAreBookmarkedIndependently() throws Exception {
        BookmarkWriteService.add(connection,10);BookmarkWriteService.add(connection,1);BookmarkWriteService.remove(connection,10);
        assertThat(number("SELECT COUNT(*) FROM bookmark WHERE node_id=1")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM bookmark WHERE node_id=10")).isZero();
        assertThat(number("SELECT master_id FROM children WHERE node_id=10")).isEqualTo(1);
    }
    @Test void removingBookmarkKeepsNodeContentTimestampAndTreePlacement() throws Exception {
        BookmarkWriteService.add(connection,1);BookmarkWriteService.remove(connection,1);BookmarkWriteService.remove(connection,1);
        assertThat(number("SELECT COUNT(*) FROM bookmark")).isZero();
        assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=1 AND txt='content' AND is_ro=1 AND ts_lastsave=100")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM children WHERE node_id=1 AND father_id=0 AND sequence=1")).isEqualTo(1);
    }
    @Test void orphanCanBeRemovedButMissingOccurrenceCannotBeAdded() throws Exception {
        try(var statement=connection.createStatement()){statement.execute("INSERT INTO bookmark VALUES(55,5)");}
        BookmarkWriteService.remove(connection,55);
        assertThat(number("SELECT COUNT(*) FROM bookmark")).isZero();
        assertThatThrownBy(()->BookmarkWriteService.add(connection,55)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->BookmarkWriteService.add(connection,11)).isInstanceOf(ResponseStatusException.class);
        assertThat(number("SELECT COUNT(*) FROM bookmark")).isZero();
    }
    @Test void existingSequenceGapsArePreservedWhenAppendingOrRemoving() throws Exception {
        try(var statement=connection.createStatement()){statement.execute("INSERT INTO bookmark VALUES(1,9)");}
        BookmarkWriteService.add(connection,2);BookmarkWriteService.remove(connection,1);
        assertThat(number("SELECT sequence FROM bookmark WHERE node_id=2")).isEqualTo(10);
    }
    @Test void sequenceExhaustionRefusesInsertion() throws Exception {
        try(var statement=connection.createStatement()){statement.execute("INSERT INTO bookmark VALUES(1,9223372036854775807)");}
        assertThatThrownBy(()->BookmarkWriteService.add(connection,2)).isInstanceOf(ResponseStatusException.class);
        assertThat(number("SELECT COUNT(*) FROM bookmark")).isEqualTo(1);
    }
    @Test void writableGuardProtectsStateAddAndRemove() {
        var properties=mock(NodePropertiesService.class);var service=new BookmarkWriteService(properties);
        when(properties.writable()).thenReturn(false);
        assertThatThrownBy(()->service.state(1)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->service.add(1)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->service.remove(1)).isInstanceOf(AccessDeniedException.class);
    }
}

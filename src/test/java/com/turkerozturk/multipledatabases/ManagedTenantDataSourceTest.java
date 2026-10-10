package com.turkerozturk.multipledatabases;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;

class ManagedTenantDataSourceTest {
    @TempDir Path directory;

    private ManagedTenantDataSource source(Path file) {
        return new ManagedTenantDataSource(() -> {
            var pool = new HikariDataSource();
            pool.setJdbcUrl("jdbc:sqlite:" + file);
            pool.setDriverClassName("org.sqlite.JDBC");
            pool.setInitializationFailTimeout(1);
            return pool;
        });
    }

    private Path database() throws Exception {
        Path file = directory.resolve("demo.ctb");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             var statement = connection.createStatement()) {
            try (var input = getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")) {
                String sql = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                for (String line : sql.split("\\R"))
                    if (line.startsWith("CREATE TABLE")) statement.execute(line);
            }
        }
        return file;
    }

    @Test void missingFileIsNeverCreated() {
        Path file = directory.resolve("missing.ctb");
        var source = source(file);
        assertThatThrownBy(source::activate).isInstanceOf(Exception.class);
        assertThat(Files.exists(file)).isFalse();
        assertThat(source.isClosed()).isTrue();
    }

    @Test void emptyDatabaseIsRejectedAndReleased() throws Exception {
        Path file = directory.resolve("empty.ctb");
        Files.createFile(file);
        var source = source(file);
        assertThatThrownBy(source::activate).isInstanceOf(SQLException.class);
        assertThat(source.isClosed()).isTrue();
        Files.delete(file);
    }

    @Test void closeReleasesFileAndSelectionCanReopen() throws Exception {
        Path file = database();
        var source = source(file);
        String generation = source.activate();
        source.close();
        Path moved = directory.resolve("moved.ctb");
        Files.move(file, moved);
        Files.move(moved, file);
        assertThat(source.activate()).isNotEqualTo(generation);
        source.close();
    }

    @Test void closeCannotInterruptBorrowedConnection() throws Exception {
        var source = source(database());
        source.activate();
        var connection = source.getConnection();
        assertThatThrownBy(source::close).isInstanceOf(IllegalStateException.class);
        assertThat(source.isClosed()).isFalse();
        connection.close();
        connection.close();
        source.close();
        assertThat(source.isClosed()).isTrue();
    }
    @Test void remoteDriverUsesConnectivityWithoutSqliteSchemaChecks() throws Exception {
        var pool = org.mockito.Mockito.mock(HikariDataSource.class);
        var connection = org.mockito.Mockito.mock(Connection.class);
        org.mockito.Mockito.when(pool.getJdbcUrl()).thenReturn("jdbc:postgresql://localhost/notes");
        org.mockito.Mockito.when(pool.getConnection()).thenReturn(connection);
        var source = new ManagedTenantDataSource(() -> pool);
        source.activate();
        org.mockito.Mockito.verify(connection, org.mockito.Mockito.never()).createStatement();
        source.close();
        org.mockito.Mockito.verify(pool).close();
    }
}

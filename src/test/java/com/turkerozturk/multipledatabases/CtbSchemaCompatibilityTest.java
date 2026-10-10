package com.turkerozturk.multipledatabases;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;

class CtbSchemaCompatibilityTest {
    @TempDir Path directory;

    private Path legacy() throws Exception {
        Path file = directory.resolve("legacy.ctb");
        try (var input = getClass().getResourceAsStream("/fixtures/legacy-ctb-schema.sql");
             var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             var statement = connection.createStatement()) {
            for (String sql : new String(input.readAllBytes(), StandardCharsets.UTF_8).split(";"))
                if (!sql.isBlank()) statement.execute(sql);
        }
        return file;
    }

    private ManagedTenantDataSource source(Path file, boolean enabled) {
        return new ManagedTenantDataSource(() -> {
            var pool = new HikariDataSource();
            pool.setJdbcUrl("jdbc:sqlite:" + file);
            pool.setDriverClassName("org.sqlite.JDBC");
            return pool;
        }, enabled);
    }

    @Test void disabledPermissionRejectsBeforeSelectionAndDoesNotChangeFile() throws Exception {
        Path file = legacy(); byte[] before = Files.readAllBytes(file);
        var source = source(file, false);
        assertThatThrownBy(() -> source.activate(true)).isInstanceOf(CtbSchemaCompatibility.Problem.class)
                .hasMessageContaining("children.master_id").hasMessageContaining("DEFAULT 0");
        assertThat(source.isClosed()).isTrue();
        assertThat(Files.readAllBytes(file)).isEqualTo(before);
    }

    @Test void userCannotUpgradeEvenWhenConfigurationAllowsIt() throws Exception {
        Path file = legacy(); byte[] before = Files.readAllBytes(file);
        var source = source(file, true);
        assertThatThrownBy(source::activate).isInstanceOf(CtbSchemaCompatibility.Problem.class);
        assertThat(Files.readAllBytes(file)).isEqualTo(before);
    }

    @Test void administratorUpgradePreservesRowsAndIsIdempotent() throws Exception {
        Path file = legacy(); var source = source(file, true);
        source.activate(true);
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            try (var row = statement.executeQuery("SELECT c.node_id,c.father_id,c.sequence,c.master_id,n.txt FROM children c JOIN node n ON n.node_id=c.node_id")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getInt(1)).isEqualTo(1); assertThat(row.getInt(2)).isZero();
                assertThat(row.getInt(3)).isEqualTo(1); assertThat(row.getInt(4)).isZero();
                assertThat(row.getString(5)).isEqualTo("Preserve Türkçe content");
                assertThat(row.next()).isFalse();
            }
            try (var row = statement.executeQuery("SELECT COUNT(*) FROM bookmark WHERE node_id=1")) {
                row.next(); assertThat(row.getInt(1)).isEqualTo(1);
            }
            statement.executeUpdate("INSERT INTO children(node_id,father_id,sequence) VALUES(2,0,2)");
            try (var row = statement.executeQuery("SELECT master_id FROM children WHERE node_id=2")) {
                row.next(); assertThat(row.getInt(1)).isZero(); assertThat(row.wasNull()).isFalse();
            }
        }
        source.close(); byte[] after = Files.readAllBytes(file);
        var noPermission = source(file, false);
        noPermission.activate(); noPermission.close();
        assertThat(Files.readAllBytes(file)).isEqualTo(after);
    }

    @Test void unsupportedSchemaNeverReceivesPartialUpgrade() throws Exception {
        Path file = legacy();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE image DROP COLUMN png");
        }
        byte[] before = Files.readAllBytes(file);
        assertThatThrownBy(() -> source(file, true).activate(true)).isInstanceOf(CtbSchemaCompatibility.Problem.class)
                .hasMessageContaining("image.png").hasMessageContaining("no automatic migration");
        assertThat(Files.readAllBytes(file)).isEqualTo(before);
    }

    @Test void readonlyConnectionFailsUpgradeWithoutCreatingColumn() throws Exception {
        Path file = legacy(); byte[] before = Files.readAllBytes(file);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:file:" + file + "?mode=ro")) {
            assertThatThrownBy(() -> CtbSchemaCompatibility.ensure(connection, true, true)).isInstanceOf(SQLException.class);
        }
        assertThat(Files.readAllBytes(file)).isEqualTo(before);
    }
}

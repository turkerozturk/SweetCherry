package com.turkerozturk.multipledatabases.info;

import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class DatabaseInfoServiceTest {
    private Connection connection;

    @BeforeEach void setup() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE node(node_id INTEGER,syntax TEXT)");
            sql.execute("CREATE TABLE children(node_id INTEGER,father_id INTEGER,master_id INTEGER)");
            for (String table : new String[]{"bookmark", "image", "codebox", "grid"})
                sql.execute("CREATE TABLE " + table + "(node_id INTEGER)");
        }
    }
    @AfterEach void close() throws Exception { connection.close(); }

    @Test void countsEmptyDatabase() throws Exception {
        var info = DatabaseInfoService.inspect(connection);
        assertThat(info.engine()).isEqualTo("SQLite");
        assertThat(info.contents()).isZero();
        assertThat(info.places()).isZero();
        assertThat(info.syntaxes()).isEmpty();
    }

    @Test void countsContentsSeparatelyFromSharedPlacementsAndIncludesOrphans() throws Exception {
        try (var sql = connection.createStatement()) {
            sql.execute("INSERT INTO node VALUES(1,'custom-colors'),(2,'plain-text'),(3,'plain-text'),(4,NULL)");
            sql.execute("INSERT INTO children VALUES(1,0,NULL),(2,1,0),(10,0,1),(11,1,1)");
            sql.execute("INSERT INTO bookmark VALUES(1),(10)");
            sql.execute("INSERT INTO image VALUES(1),(1),(2)");
            sql.execute("INSERT INTO codebox VALUES(1),(2)");
            sql.execute("INSERT INTO grid VALUES(2)");
        }
        var info = DatabaseInfoService.inspect(connection);
        assertThat(info.contents()).isEqualTo(4);
        assertThat(info.places()).isEqualTo(4);
        assertThat(info.sharedPlaces()).isEqualTo(2);
        assertThat(info.roots()).isEqualTo(2);
        assertThat(info.bookmarks()).isEqualTo(2);
        assertThat(info.objects()).isEqualTo(3);
        assertThat(info.codeBoxes()).isEqualTo(2);
        assertThat(info.tables()).isEqualTo(1);
        assertThat(info.syntaxes()).containsExactly(new DatabaseInfoService.SyntaxCount("",1),
                new DatabaseInfoService.SyntaxCount("custom-colors",1), new DatabaseInfoService.SyntaxCount("plain-text",2));
        // The fixture deliberately has no txt/png/name columns: aggregate queries cannot load them.
        assertThat(DatabaseInfoService.inspect(connection)).isEqualTo(info);
    }
}

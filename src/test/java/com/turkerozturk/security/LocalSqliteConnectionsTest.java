package com.turkerozturk.security;

import java.nio.file.*;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;
import static org.assertj.core.api.Assertions.*;

class LocalSqliteConnectionsTest {
    @TempDir Path directory;
    @Test void createsAndReopensAUnicodeFileWithTheSqliteDriver() throws Exception {
        Path file=directory.resolve("Türkçe notes.ctb");
        try(var c=LocalSqliteConnections.open(file);var s=c.createStatement()) {
            s.execute("CREATE TABLE example(value TEXT)");s.execute("INSERT INTO example VALUES('kept')");
        }
        var config=new SQLiteConfig();config.resetOpenMode(SQLiteOpenMode.CREATE);
        try(var c=LocalSqliteConnections.open(file,config);var s=c.createStatement();var rows=s.executeQuery("SELECT value FROM example")) {
            assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("kept");
        }
    }
    @Test void preservesNoCreateModeAndRejectsUrlMetacharactersBeforeWriting() {
        var config=new SQLiteConfig();config.resetOpenMode(SQLiteOpenMode.CREATE);
        Path missing=directory.resolve("missing.ctb");
        assertThatThrownBy(()->LocalSqliteConnections.open(missing,config)).isInstanceOf(SQLException.class);
        assertThat(Files.exists(missing)).isFalse();
        // Windows rejects some names while constructing Path; Unix reaches the SQLite guard.
        // Either rejection must happen before any file is created.
        for(String name:new String[]{"notes.ctb?mode=memory","notes#fragment.ctb","notes\n.ctb"})
            assertThatThrownBy(()->LocalSqliteConnections.open(directory.resolve(name)))
                    .isInstanceOfAny(SQLException.class, InvalidPathException.class);
        assertThat(directory.toFile().list()).isEmpty();
    }
}

package com.turkerozturk.backup;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import org.springframework.stereotype.Component;

/** SQLite owns snapshot consistency, including committed pages still in the WAL. */
@Component
public class CtbBackupEngine {
    static final List<String> TABLES = List.of("node", "children", "bookmark", "image", "codebox", "grid");
    Path sourcePath(Connection source) throws Exception {
        if (!source.isWrapperFor(SQLiteConnection.class)) throw new CtbBackupProblem("unsupported");
        try (var statement=source.createStatement();var rows=statement.executeQuery("PRAGMA database_list")) {
            while (rows.next()) if ("main".equals(rows.getString("name"))) {
                String file=rows.getString("file");
                if (file==null || file.isBlank() || !file.toLowerCase(Locale.ROOT).endsWith(".ctb")) break;
                Path path=Path.of(file).toRealPath();
                if (Files.isRegularFile(path)) return path;
            }
        }
        throw new CtbBackupProblem("unsupported");
    }
    void copy(Connection source, Path target, org.sqlite.core.DB.ProgressObserver observer) throws SQLException {
        int code=source.unwrap(SQLiteConnection.class).getDatabase().backup("main",target.toString(),observer,100,50,128);
        if (code != 0) throw new SQLException("SQLite snapshot failed", "", code);
    }
    static Connection readOnly(Path file) throws SQLException {
        var config=new SQLiteConfig();config.setReadOnly(true);config.setBusyTimeout(5000);
        return DriverManager.getConnection("jdbc:sqlite:"+file.toUri().toASCIIString(),config.toProperties());
    }
    Map<String,Long> verify(Path file) throws SQLException {
        try (var connection=readOnly(file);var statement=connection.createStatement()) {
            try (var rows=statement.executeQuery("PRAGMA integrity_check")) {
                if (!rows.next() || !"ok".equals(rows.getString(1)) || rows.next()) throw new CtbBackupProblem("integrity");
            }
            Map<String,Long> counts=new LinkedHashMap<>();
            for (String table:TABLES) try(var rows=statement.executeQuery("SELECT COUNT(*) FROM "+table)) {
                rows.next();counts.put(table,rows.getLong(1));
            }
            return Collections.unmodifiableMap(counts);
        }
    }
}

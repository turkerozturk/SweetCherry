package com.turkerozturk.security;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConfig;

/** SQLite files only: no caller-supplied JDBC URL, resource URL or URI/query options. */
public final class LocalSqliteConnections {
    private LocalSqliteConnections() { }

    public static Connection open(Path file) throws SQLException {
        return open(file, new SQLiteConfig());
    }

    public static Connection open(Path file, SQLiteConfig settings) throws SQLException {
        if (file == null || settings == null) throw new SQLException("Invalid SQLite file");
        String path = file.toAbsolutePath().normalize().toString();
        if (path.indexOf('?') >= 0 || path.indexOf('#') >= 0
                || path.codePoints().anyMatch(Character::isISOControl)) {
            throw new SQLException("SQLite file paths cannot contain URL options or control characters");
        }
        // Select the SQLite driver directly, rather than dispatching through the driver registry.
        return JDBC.createConnection("jdbc:sqlite:" + path, settings.toProperties());
    }
}

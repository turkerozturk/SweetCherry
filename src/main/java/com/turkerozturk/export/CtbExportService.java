package com.turkerozturk.export;

import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CtbExportService {
    @PersistenceContext private EntityManager entityManager;
    @Autowired
    private CustomPropertiesHolder customPropertiesHolder;
    @Value("${user.dir}") private String applicationPath;
    @Value("${myapp.exportingFolderName}") private String exportingFolderName;
    public record Result(int count, String filename) { }

    @Transactional
    public synchronized Result export(long root, boolean descendants, boolean collection) throws IOException, SQLException {
        Path directory = Path.of(applicationPath, exportingFolderName).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String tenant = TenantContext.getCurrentTenant().replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        String filename = collection ? "exportednodes.ctb" : "export_" + tenant + "_" + root + ".ctb";
        Path destination = directory.resolve(filename);
        // Reuse the transaction connection when exporting back into the selected SQLite file.
        // A second writer connection cannot commit while this transaction holds its read lock.
        if (collection && Files.exists(destination)) {
            Integer count = entityManager.unwrap(Session.class).doReturningWork(source -> {
                if (!sameSqliteFile(source, destination)) return null;
                var properties = customPropertiesHolder.getCustomProperties(TenantContext.getCurrentTenant());
                if (properties == null || !Boolean.parseBoolean(properties.get("custom.isWritable")))
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN, "Selected CTB is read-only");
                return appendInPlace(source, root, descendants);
            });
            if (count != null) return new Result(count, filename);
        }
        Path output = collection ? destination : Files.createTempFile(directory, "ctb-export-", ".tmp");
        boolean existed = collection && Files.exists(output);
        boolean success = false;
        try {
            int count;
            try (var target = DriverManager.getConnection("jdbc:sqlite:" + output)) {
                target.setAutoCommit(false);
                try {
                    try (var statement = target.createStatement()) {
                        for (String sql : SqliteSchemaLoader.load().split(";")) if (!sql.isBlank()) statement.execute(sql);
                    }
                    count = entityManager.unwrap(Session.class).doReturningWork(source ->
                            CtbExportSql.copy(source, target, root, descendants, collection));
                    target.commit();
                } catch (SQLException | IOException | RuntimeException failure) {
                    target.rollback(); throw failure;
                }
            }
            if (!collection) {
                Path previous = null;
                if (Files.exists(destination)) {
                    previous = directory.resolve(filename + "__" + LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + ".old");
                    Files.move(destination, previous);
                }
                try { Files.move(output, destination); }
                catch (IOException failure) {
                    if (previous != null) Files.move(previous, destination);
                    throw failure;
                }
            }
            success = true;
            return new Result(count, filename);
        } finally {
            if (!success && (!collection || !existed)) Files.deleteIfExists(output);
        }
    }
    static boolean sameSqliteFile(Connection source, Path destination) throws SQLException {
        if (!source.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")) return false;
        try (var statement = source.createStatement(); var rows = statement.executeQuery("PRAGMA database_list")) {
            while (rows.next()) if ("main".equals(rows.getString("name"))) {
                String file = rows.getString("file");
                if (file == null || file.isBlank()) return false;
                try { return Files.isSameFile(Path.of(file), destination); }
                catch (IOException failure) { throw new SQLException("Cannot identify CTB export destination", failure); }
            }
        }
        return false;
    }

    static int appendInPlace(Connection connection, long root, boolean descendants) throws SQLException {
        boolean ownTransaction = connection.getAutoCommit();
        if (ownTransaction) connection.setAutoCommit(false);
        Savepoint point = connection.setSavepoint();
        try {
            int count = CtbExportSql.copy(connection, connection, root, descendants, true);
            connection.releaseSavepoint(point);
            if (ownTransaction) connection.commit();
            return count;
        } catch (SQLException | RuntimeException failure) {
            if (ownTransaction) connection.rollback();
            else connection.rollback(point);
            throw failure;
        } finally {
            if (ownTransaction) connection.setAutoCommit(true);
        }
    }
}

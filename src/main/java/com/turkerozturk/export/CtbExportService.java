package com.turkerozturk.export;

import com.turkerozturk.multipledatabases.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CtbExportService {
    @PersistenceContext private EntityManager entityManager;
    @Value("${user.dir}") private String applicationPath;
    @Value("${myapp.exportingFolderName}") private String exportingFolderName;
    public record Result(int count, String filename) { }

    @Transactional(readOnly = true)
    public synchronized Result export(long root, boolean descendants, boolean collection) throws IOException, SQLException {
        Path directory = Path.of(applicationPath, exportingFolderName).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String tenant = TenantContext.getCurrentTenant().replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        String filename = collection ? "exportednodes.ctb" : "export_" + tenant + "_" + root + ".ctb";
        Path destination = directory.resolve(filename);
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
}

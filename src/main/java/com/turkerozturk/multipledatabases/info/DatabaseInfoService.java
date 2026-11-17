package com.turkerozturk.multipledatabases.info;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aggregate counts only: never reads note content, BLOBs, credentials or JDBC URLs. */
@Service
@Transactional(readOnly = true)
public class DatabaseInfoService {
    @PersistenceContext private EntityManager entityManager;

    public record SyntaxCount(String syntax, long count) { }
    public record Info(String engine, long contents, long places, long sharedPlaces, long roots,
            long bookmarks, long objects, long codeBoxes, long tables, List<SyntaxCount> syntaxes) { }

    public Info inspect() {
        return entityManager.unwrap(Session.class).doReturningWork(DatabaseInfoService::inspect);
    }

    static Info inspect(Connection connection) throws SQLException {
        long contents = count(connection, "SELECT COUNT(*) FROM node");
        long places = count(connection, "SELECT COUNT(*) FROM children");
        long shared = count(connection, "SELECT COUNT(*) FROM children WHERE COALESCE(master_id,0)<>0");
        long roots = count(connection, "SELECT COUNT(*) FROM children WHERE COALESCE(father_id,0)=0");
        long bookmarks = count(connection, "SELECT COUNT(*) FROM bookmark");
        long objects = count(connection, "SELECT COUNT(*) FROM image");
        long codeBoxes = count(connection, "SELECT COUNT(*) FROM codebox");
        long tables = count(connection, "SELECT COUNT(*) FROM grid");
        var syntaxes = new ArrayList<SyntaxCount>();
        try (var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT COALESCE(syntax,''),COUNT(*) FROM node GROUP BY COALESCE(syntax,'') ORDER BY COALESCE(syntax,'')")) {
            while (rows.next()) syntaxes.add(new SyntaxCount(rows.getString(1), rows.getLong(2)));
        }
        return new Info(connection.getMetaData().getDatabaseProductName(), contents, places, shared,
                roots, bookmarks, objects, codeBoxes, tables, List.copyOf(syntaxes));
    }

    private static long count(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1);
        }
    }
}

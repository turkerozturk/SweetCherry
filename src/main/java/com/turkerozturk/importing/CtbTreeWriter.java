package com.turkerozturk.importing;

import com.turkerozturk.jdbc.JdbcIdentifiers;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Inserts a plain-text tree in the caller's transaction using freshly allocated CTB IDs. */
public final class CtbTreeWriter {
    private CtbTreeWriter() {}

    public record Entry(String name, String text, String tags, int parent, int sequence,
                        long titleBits, int icon, long created, long modified) {}
    public record Result(long rootId, int count) {}

    /** Returns null for an existing root when a unique root name is required. */
    public static Result insert(Connection connection, List<Entry> nodes, boolean uniqueRoot) throws SQLException {
        if (connection.getAutoCommit() || connection.isReadOnly() || !connection.getMetaData().supportsTransactions()) {
            throw new SQLException("Writable transaction required");
        }
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        boolean mysql = product.contains("mysql") || product.contains("mariadb");
        if (mysql) {
            var transactional = new HashSet<String>();
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()")) {
                while (rows.next()) {
                    if ("InnoDB".equalsIgnoreCase(rows.getString(2))) transactional.add(rows.getString(1));
                }
            }
            if (!transactional.containsAll(Set.of("node", "children"))) throw new SQLException("Transactional tables required");
        }
        if (uniqueRoot && rootExists(connection, nodes.get(0).name(), product, mysql)) return null;

        long maxId = scalar(connection, """
                SELECT COALESCE(MAX(node_id),0)
                FROM (SELECT node_id FROM node UNION ALL SELECT node_id FROM children) AS tree_ids
                """);
        long rootSequence = Math.addExact(scalar(connection,
                "SELECT COALESCE(MAX(sequence),0) FROM children WHERE father_id=0"), 1);
        Math.addExact(maxId, nodes.size());

        String[] columns = {"node_id", "name", "txt", "syntax", "tags", "is_ro", "is_richtxt", "has_codebox",
                "has_table", "has_image", "level", "ts_creation", "ts_lastsave"};
        var quoted = new ArrayList<String>();
        for (String name : columns) quoted.add(JdbcIdentifiers.quote(connection, name));
        String nodeSql = "INSERT INTO " + JdbcIdentifiers.quote(connection, "node") + " ("
                + String.join(",", quoted) + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String childSql = "INSERT INTO " + JdbcIdentifiers.quote(connection, "children")
                + " (node_id,father_id,sequence,master_id) VALUES (?,?,?,?)";
        try (var node = connection.prepareStatement(nodeSql); var child = connection.prepareStatement(childSql)) {
            for (int index = 0; index < nodes.size(); index++) {
                Entry entry = nodes.get(index);
                long id = maxId + index + 1;
                Object[] values = {id, entry.name(), entry.text(), "plain-text", entry.tags(), entry.icon(),
                        entry.titleBits(), 0, 0, 0, 0, entry.created(), entry.modified()};
                for (int column = 0; column < values.length; column++) node.setObject(column + 1, values[column]);
                node.executeUpdate();
                child.setLong(1, id);
                child.setLong(2, entry.parent() < 0 ? 0 : maxId + entry.parent() + 1);
                child.setLong(3, entry.parent() < 0 ? rootSequence : entry.sequence());
                child.setLong(4, 0);
                child.executeUpdate();
            }
        }
        return new Result(maxId + 1, nodes.size());
    }

    private static boolean rootExists(Connection connection, String name, String product, boolean mysql)
            throws SQLException {
        try (var statement = connection.createStatement()) {
            // Take the write lock before checking for a duplicate, including on an empty CTB.
            lockForRootWrite(connection, product);
            String sql = """
                    SELECT n.name FROM children c
                    JOIN node n ON n.node_id = COALESCE(NULLIF(c.master_id, 0), c.node_id)
                    WHERE c.father_id = 0
                    """;
            try (var rows = statement.executeQuery(sql + (mysql ? " FOR UPDATE" : ""))) {
                while (rows.next()) {
                    if (name.equals(rows.getString(1))) return true;
                }
            }
        }
        return false;
    }

    public static void lockForRootWrite(Connection connection, String product) throws SQLException {
        try (var statement = connection.createStatement()) {
            if (product.contains("sqlite")) statement.executeUpdate("UPDATE children SET sequence=sequence WHERE 1=0");
            if (product.contains("postgresql")) statement.execute("LOCK TABLE node, children IN SHARE ROW EXCLUSIVE MODE");
        }
    }

    private static long scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}

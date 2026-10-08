package com.turkerozturk.export;

import java.sql.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Copies tree occurrences; each content group has a master inside the target branch. */
final class CtbExportSql {
    private CtbExportSql() { }
    private record Row(long id, long parent, long sequence, long master) { }

    static int copy(Connection source, Connection target, long root, boolean descendants,
                    boolean allocateIds) throws SQLException {
        var rows = new LinkedHashMap<Long, Row>();
        var children = new HashMap<Long, List<Row>>();
        try (var statement = source.createStatement(); var result = statement.executeQuery(
                "SELECT node_id,father_id,sequence,COALESCE(master_id,0) FROM children ORDER BY sequence,node_id")) {
            while (result.next()) {
                var row = new Row(result.getLong(1), result.getLong(2), result.getLong(3), result.getLong(4));
                if (row.id <= 0 || rows.put(row.id, row) != null) throw conflict();
                children.computeIfAbsent(row.parent, key -> new ArrayList<>()).add(row);
            }
        }
        if (!rows.containsKey(root)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var branch = new ArrayList<Row>();
        var pending = new ArrayDeque<Row>(); pending.push(rows.get(root));
        var visited = new HashSet<Long>();
        while (!pending.isEmpty()) {
            var row = pending.pop();
            if (!visited.add(row.id)) throw conflict();
            branch.add(row);
            if (descendants) {
                var ownChildren = children.getOrDefault(row.id, List.of());
                for (int i = ownChildren.size() - 1; i >= 0; i--) pending.push(ownChildren.get(i));
            }
        }
        var masters = new HashMap<Long, Long>();
        for (var row : branch) if (row.master == 0) masters.put(row.id, row.id);
        for (var row : branch) if (row.master != 0) {
            var master = rows.get(row.master);
            if (master == null || master.master != 0) throw conflict();
            masters.putIfAbsent(row.master, row.id);
        }
        long next = scalar(target, "SELECT COALESCE(MAX(node_id),0)+1 FROM "
                + "(SELECT node_id FROM node UNION ALL SELECT node_id FROM children)");
        var ids = new HashMap<Long, Long>();
        for (var row : branch) ids.put(row.id, allocateIds ? next++ : row.id);
        long rootSequence = scalar(target, "SELECT COALESCE(MAX(sequence),0)+1 FROM children WHERE father_id=0");
        long bookmarkSequence = scalar(target, "SELECT COALESCE(MAX(sequence),0)+1 FROM bookmark");
        for (var row : branch) {
            long content = row.master == 0 ? row.id : row.master;
            long representative = masters.get(content);
            long id = ids.get(row.id);
            try (var insert = target.prepareStatement("INSERT INTO children VALUES (?,?,?,?)")) {
                insert.setLong(1, id);
                insert.setLong(2, row.id == root ? 0 : ids.get(row.parent));
                insert.setLong(3, row.id == root ? rootSequence : row.sequence);
                insert.setLong(4, representative == row.id ? 0 : ids.get(representative));
                insert.executeUpdate();
            }
            if (representative == row.id) {
                int count = copyRows(source, target, "node", content, id);
                if (count != 1) throw conflict();
                for (String table : List.of("image", "grid", "codebox")) copyRows(source, target, table, content, id);
            }
            try (var bookmark = source.prepareStatement("SELECT node_id FROM bookmark WHERE node_id=?")) {
                bookmark.setLong(1, row.id);
                try (var result = bookmark.executeQuery()) {
                    if (result.next()) try (var insert = target.prepareStatement("INSERT INTO bookmark VALUES (?,?)")) {
                        insert.setLong(1, id); insert.setLong(2, bookmarkSequence++); insert.executeUpdate();
                    }
                }
            }
        }
        return branch.size();
    }

    private static int copyRows(Connection source, Connection target, String table, long oldId, long newId)
            throws SQLException {
        try (var query = source.prepareStatement("SELECT * FROM " + table + " WHERE node_id=?")) {
            query.setLong(1, oldId);
            try (var result = query.executeQuery()) {
                var columns = new ArrayList<String>();
                for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) {
                    String column = result.getMetaData().getColumnName(i);
                    if (!column.matches("[A-Za-z_]+")) throw conflict();
                    columns.add(column);
                }
                String sql = "INSERT INTO " + table + " (" + String.join(",", columns) + ") VALUES ("
                        + String.join(",", Collections.nCopies(columns.size(), "?")) + ")";
                int count = 0;
                try (var insert = target.prepareStatement(sql)) {
                    while (result.next()) {
                        for (int i = 0; i < columns.size(); i++) {
                            Object value = columns.get(i).equals("node_id") ? newId
                                    : columns.get(i).equals("png") ? result.getBytes(i + 1) : result.getObject(i + 1);
                            if (value instanceof Clob) value = result.getString(i + 1);
                            insert.setObject(i + 1, value);
                        }
                        insert.executeUpdate(); count++;
                    }
                }
                return count;
            }
        }
    }

    private static long scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next(); return result.getLong(1);
        }
    }
    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Invalid CTB hierarchy or content reference");
    }
}

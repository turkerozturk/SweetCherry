package com.turkerozturk.node.moving;

import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Moves tree occurrences, including aliases, without changing their content or bookmarks. */
@Service
public class NodeMoveService {
    private final NodePropertiesService properties;
    @PersistenceContext private EntityManager entityManager;
    public NodeMoveService(NodePropertiesService properties) { this.properties = properties; }
    public enum Direction { UP, DOWN, LEFT, RIGHT }
    record Row(long id, long parent, long sequence, long master) { }
    public record State(String revision, boolean up, boolean down, boolean left, boolean right) { }

    /** Returns only moves that are valid for the current tree snapshot. */
    @Transactional(readOnly = true)
    public State state(long id) {
        requireWritable();
        return entityManager.unwrap(org.hibernate.Session.class).doReturningWork(connection -> state(read(connection), id));
    }

    /** Rechecks the displayed snapshot inside the write transaction and commits one structural change. */
    @Transactional
    public void move(long id, Direction direction, String revision) {
        requireWritable();
        entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> move(connection, id, direction, revision));
        entityManager.clear();
    }
    private void requireWritable() {
        if (!properties.writable()) throw new AccessDeniedException("The selected CTB is read-only.");
    }

    /** Reads tree identities directly through the tenant transaction's SQLite connection. */
    static List<Row> read(Connection connection) throws SQLException {
        var result = new ArrayList<Row>();
        try (var statement = connection.prepareStatement("SELECT node_id, father_id, sequence, COALESCE(master_id,0) FROM children ORDER BY node_id");
                var rows = statement.executeQuery()) {
            while (rows.next()) result.add(new Row(rows.getLong(1), rows.getLong(2), rows.getLong(3), rows.getLong(4)));
        }
        validate(result);
        return result;
    }

    /** Rejects broken parent chains and aliases with children before proposing a move. */
    private static void validate(List<Row> rows) {
        var index = new HashMap<Long, Row>();
        for (var row : rows) if (row.id <= 0 || index.put(row.id, row) != null) throw conflict();
        var checked = new HashSet<Long>();
        for (var row : rows) {
            var path = new HashSet<Long>();
            long id = row.id;
            while (id != 0 && !checked.contains(id)) {
                var item = index.get(id);
                if (item == null || !path.add(id)) throw conflict();
                if (item.parent != 0) {
                    var parent = index.get(item.parent);
                    if (parent == null || parent.master != 0) throw conflict();
                }
                id = item.parent;
            }
            checked.addAll(path);
        }
    }
    private static Row selected(List<Row> rows, long id) {
        return rows.stream().filter(row -> row.id == id).findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Tree node not found"));
    }
    private static List<Row> siblings(List<Row> rows, long parent) {
        return new ArrayList<>(rows.stream().filter(row -> row.parent == parent)
                .sorted(Comparator.comparingLong(Row::sequence).thenComparingLong(Row::id)).toList());
    }
    static State state(List<Row> rows, long id) {
        validate(rows);
        var node = selected(rows,id); var siblings = siblings(rows,node.parent); int at = siblings.indexOf(node);
        return new State(revision(rows), at > 0, at < siblings.size()-1, node.parent != 0,
                at > 0 && siblings.get(at-1).master == 0);
    }

    /** Renumbers only affected sibling groups, preserving subtree and master references. */
    static void move(Connection connection, long id, Direction direction, String expected) throws SQLException {
        var rows = read(connection); var state = state(rows,id);
        if (!state.revision.equals(expected)) throw conflict();
        boolean allowed = switch (direction) { case UP -> state.up; case DOWN -> state.down; case LEFT -> state.left; case RIGHT -> state.right; };
        if (!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT, "This move has no valid destination.");
        var node = selected(rows,id); var source = siblings(rows,node.parent); int at = source.indexOf(node);
        if (direction == Direction.UP || direction == Direction.DOWN) {
            Collections.swap(source,at,at + (direction == Direction.UP ? -1 : 1));
            write(connection,source,node.parent);
        } else {
            long targetParent;
            List<Row> target;
            int insertAt;
            if (direction == Direction.RIGHT) {
                targetParent = source.get(at-1).id;
                target = siblings(rows,targetParent); insertAt = target.size();
            } else {
                var parent = selected(rows,node.parent); targetParent = parent.parent;
                target = siblings(rows,targetParent); insertAt = target.indexOf(parent)+1;
            }
            // A real parent must exist in node, not just in the hierarchy table.
            if (targetParent != 0) try (var query = connection.prepareStatement("SELECT node_id FROM node WHERE node_id = ?")) {
                query.setLong(1,targetParent);
                try (var result = query.executeQuery()) { if (!result.next()) throw conflict(); }
            }
            source.remove(at); target.add(insertAt,node);
            write(connection,source,node.parent); write(connection,target,targetParent);
        }
    }
    private static void write(Connection connection, List<Row> rows, long parent) throws SQLException {
        try (var statement = connection.prepareStatement("UPDATE children SET father_id = ?, sequence = ? WHERE node_id = ?")) {
            for (int index=0;index<rows.size();index++) {
                statement.setLong(1,parent); statement.setLong(2,index+1L); statement.setLong(3,rows.get(index).id);
                if (statement.executeUpdate()!=1) throw conflict();
            }
        }
    }
    /** Fingerprints hierarchy fields so stale controls cannot reinterpret a direction after another move. */
    private static String revision(List<Row> rows) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            rows.stream().sorted(Comparator.comparingLong(Row::id)).forEach(row -> digest.update(
                    (row.id+":"+row.parent+":"+row.sequence+":"+row.master+";").getBytes(StandardCharsets.UTF_8)));
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT,"Tree changed or has an invalid hierarchy; reload the page.");
    }
}

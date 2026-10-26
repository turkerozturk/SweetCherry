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
    public record BookmarkTarget(String nodeId, String name, String iconName, boolean bold, String color) { }

    /** Only bookmark display metadata: never load note text or embedded objects for this picker. */
    @Transactional(readOnly = true)
    public List<BookmarkTarget> bookmarkTargets(long id) {
        requireWritable();
        return entityManager.unwrap(org.hibernate.Session.class).doReturningWork(connection -> bookmarkTargets(connection,id));
    }

    static List<BookmarkTarget> bookmarkTargets(Connection connection, long id) throws SQLException {
        try (var query = connection.prepareStatement("SELECT node_id FROM children WHERE node_id = ?")) {
            query.setLong(1,id);
            try (var rows = query.executeQuery()) {
                if (!rows.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Tree node not found");
            }
        }
        var targets = new ArrayList<BookmarkTarget>();
        String sql = "SELECT b.node_id,n.name,n.is_ro,n.is_richtxt,n.syntax FROM bookmark b "
                + "JOIN children c ON c.node_id=b.node_id JOIN node n ON n.node_id="
                + "CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END "
                + "ORDER BY b.sequence,b.node_id";
        try (var query = connection.prepareStatement(sql); var rows = query.executeQuery()) {
            while (rows.next()) {
                // Use the same icon/color decoding as the normal node views, without an entity load.
                var display = new com.turkerozturk.node.Node();
                display.setIsReadOnly16bit(rows.getLong(3));
                display.setIsRichText(rows.getLong(4));
                display.setSyntax(Objects.requireNonNullElse(rows.getString(5),"plain-text"));
                display.parseNodeIconAndReadOnlyFlag();
                display.parseNodeTitleColorAndBoldnessAndTextType();
                targets.add(new BookmarkTarget(Long.toString(rows.getLong(1)),
                        Objects.requireNonNullElse(rows.getString(2),""),display.getNodeIcon().getIconName(),
                        display.isBoldnessBit(),String.format(Locale.ROOT,"#%06X",display.getTitleColor())));
            }
        }
        return List.copyOf(targets);
    }

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

    /** Moves an occurrence and its existing subtree to the end of a chosen parent's children. */
    @Transactional
    public void moveTo(long id, long parentId, String revision) {
        requireWritable();
        entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> moveTo(connection,id,parentId,revision));
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

    /** Rejects broken or cyclic occurrence parent chains before proposing a move. */
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
                    if (parent == null) throw conflict();
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
                at > 0);
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
            // A shared parent has no node row of its own; validate its master content.
            requireParentContent(connection,rows,targetParent);
            source.remove(at); target.add(insertAt,node);
            write(connection,source,node.parent); write(connection,target,targetParent);
        }
        com.turkerozturk.node.NodeTimestamps.touchOccurrence(connection,id,
                com.turkerozturk.node.NodeTimestamps.now());
    }

    /** Validates everything before writing; following parent IDs avoids content/master-based cycles. */
    static void moveTo(Connection connection, long id, long parentId, String expected) throws SQLException {
        if (id <= 0 || parentId < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid node ID");
        var rows = read(connection);
        if (!revision(rows).equals(expected)) throw conflict();
        var node = selected(rows,id);
        var index = new HashMap<Long,Row>();
        for (var row : rows) index.put(row.id,row);
        if (parentId != 0) selected(rows,parentId);
        for (long ancestor=parentId; ancestor!=0; ancestor=index.get(ancestor).parent) {
            if (ancestor == id) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A node cannot be moved into itself or its descendants.");
        }
        requireParentContent(connection,rows,parentId);
        // An already-selected parent is a no-op, retaining order and modified date.
        if (node.parent == parentId) return;
        var source = siblings(rows,node.parent);
        var target = siblings(rows,parentId);
        source.remove(node);
        target.add(node);
        write(connection,source,node.parent);
        write(connection,target,parentId);
        com.turkerozturk.node.NodeTimestamps.touchOccurrence(connection,id,
                com.turkerozturk.node.NodeTimestamps.now());
    }

    private static void requireParentContent(Connection connection, List<Row> rows, long parentId) throws SQLException {
        if (parentId == 0) return;
        var parent = selected(rows,parentId);
        try (var query = connection.prepareStatement("SELECT node_id FROM node WHERE node_id = ?")) {
            query.setLong(1,parent.master == 0 ? parent.id : parent.master);
            try (var result = query.executeQuery()) { if (!result.next()) throw conflict(); }
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

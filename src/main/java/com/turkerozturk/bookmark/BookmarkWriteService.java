package com.turkerozturk.bookmark;

import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.sql.*;

/** Changes bookmark occurrences and touches their content master without altering text or tree placement. */
@Service
public class BookmarkWriteService {
    private final NodePropertiesService properties;
    @PersistenceContext private EntityManager entityManager;
    public BookmarkWriteService(NodePropertiesService properties) {this.properties=properties;}
    public boolean writable() {return properties.writable();}
    public record State(boolean bookmarked) { }

    /** Uses the selected tree identity, so an alias and its master may have independent bookmarks. */
    @Transactional(readOnly=true)
    public State state(long id) {
        requireWritable();
        return entityManager.unwrap(org.hibernate.Session.class).doReturningWork(connection -> {
            requireOccurrence(connection,id);return new State(exists(connection,id));
        });
    }
    @Transactional
    public void add(long id) {
        requireWritable();
        entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> add(connection,id));
        entityManager.clear();
    }
    @Transactional
    public void remove(long id) {
        requireWritable();
        entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> remove(connection,id));
        entityManager.clear();
    }
    /** Updates one stored sort key; equal keys retain the existing node-ID tie breaker. */
    @Transactional
    public void reorder(long id, long sequence) {
        requireWritable();
        entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> reorder(connection,id,sequence));
        entityManager.clear();
    }
    static void reorder(Connection connection, long id, long sequence) throws SQLException {
        if (sequence <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid bookmark sequence");
        requireOccurrence(connection,id);
        if (!exists(connection,id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bookmark not found");
        try (var update = connection.prepareStatement("UPDATE bookmark SET sequence=? WHERE node_id=? AND sequence<>?")) {
            update.setLong(1,sequence); update.setLong(2,id); update.setLong(3,sequence);
            if (update.executeUpdate() != 0) com.turkerozturk.node.NodeTimestamps.touchOccurrence(
                    connection,id,com.turkerozturk.node.NodeTimestamps.now());
        }
    }
    private record OrderRow(long id,long sequence,boolean available) { }
    private static java.util.List<OrderRow> orderRows(Connection connection) throws SQLException {
        var rows=new java.util.ArrayList<OrderRow>();
        try(var query=connection.prepareStatement("SELECT b.node_id,b.sequence,CASE WHEN n.node_id IS NULL THEN 0 ELSE 1 END "
                + "FROM bookmark b LEFT JOIN children c ON c.node_id=b.node_id LEFT JOIN node n ON n.node_id="
                + "CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END ORDER BY b.sequence,b.node_id");
                var result=query.executeQuery()) {
            while(result.next())rows.add(new OrderRow(result.getLong(1),result.getLong(2),result.getInt(3)!=0));
        }
        return rows;
    }
    private static String orderRevision(java.util.List<OrderRow> rows) {
        try {
            var digest=java.security.MessageDigest.getInstance("SHA-256");
            for(var row:rows)digest.update((row.id+":"+row.sequence+":"+row.available+";").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest.digest());
        }catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}
    }
    /** Fingerprints exactly the rows rendered by the GET, avoiding a second query racing with that snapshot. */
    public String orderRevision(java.util.List<Bookmark> bookmarks,java.util.List<Bookmark> available) {
        var visible=new java.util.HashSet<Long>();available.forEach(bookmark->visible.add(bookmark.getNodeId()));
        var rows=bookmarks.stream().map(bookmark->new OrderRow(bookmark.getNodeId(),bookmark.getSequence(),visible.contains(bookmark.getNodeId())))
                .sorted(java.util.Comparator.comparingLong(OrderRow::sequence).thenComparingLong(OrderRow::id)).toList();
        return orderRevision(rows);
    }
    @Transactional
    public void reorderAll(java.util.List<Long> order,String revision) {
        requireWritable();
        entityManager.unwrap(org.hibernate.Session.class).doWork(connection->reorderAll(connection,order,revision));
        entityManager.clear();
    }
    static String orderRevision(Connection connection) throws SQLException { return orderRevision(orderRows(connection)); }

    /** Merges the visible permutation into existing slots; orphan bookmarks remain present in their slots. */
    static void reorderAll(Connection connection,java.util.List<Long> order,String revision) throws SQLException {
        var rows=orderRows(connection);
        if(!orderRevision(rows).equals(revision))throw new ResponseStatusException(HttpStatus.CONFLICT,"Bookmarks changed; reload the page.");
        var visible=rows.stream().filter(OrderRow::available).map(OrderRow::id).toList();
        if(order==null || order.size()>10000 || order.size()!=visible.size()
                || order.stream().anyMatch(id->id==null || id<=0)
                || new java.util.HashSet<>(order).size()!=order.size()
                || !new java.util.HashSet<>(order).equals(new java.util.HashSet<>(visible)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid bookmark order");
        if(visible.equals(order))return;
        var byId=new java.util.HashMap<Long,OrderRow>();rows.forEach(row->byId.put(row.id,row));
        int at=0;long sequence=0,timestamp=com.turkerozturk.node.NodeTimestamps.now();
        try(var update=connection.prepareStatement("UPDATE bookmark SET sequence=? WHERE node_id=? AND sequence=?")) {
            for(var slot:rows) {
                long id=slot.available?order.get(at++):slot.id;sequence++;
                var old=byId.get(id);
                if(old.sequence==sequence)continue;
                update.setLong(1,sequence);update.setLong(2,id);update.setLong(3,old.sequence);
                if(update.executeUpdate()!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Bookmarks changed; reload the page.");
                com.turkerozturk.node.NodeTimestamps.touchOccurrence(connection,id,timestamp);
            }
        }
    }
    private void requireWritable() {
        if(!writable()) throw new AccessDeniedException("The selected CTB is read-only.");
    }
    /** Validates the real content target but retains the occurrence ID for bookmark storage. */
    private static void requireOccurrence(Connection connection,long id) throws SQLException {
        try(var statement=connection.prepareStatement("SELECT c.node_id FROM children c JOIN node n ON n.node_id = "
                + "CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END WHERE c.node_id=?")) {
            statement.setLong(1,id);
            try(var row=statement.executeQuery()) {
                if(!row.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Tree node or its master not found");
            }
        }
    }
    private static boolean exists(Connection connection,long id) throws SQLException {
        try(var statement=connection.prepareStatement("SELECT 1 FROM bookmark WHERE node_id=?")) {
            statement.setLong(1,id);try(var rows=statement.executeQuery()){return rows.next();}
        }
    }
    /** Appends only once; repeat submissions leave existing sequence and metadata untouched. */
    static void add(Connection connection,long id) throws SQLException {
        requireOccurrence(connection,id);
        if(exists(connection,id))return;
        long sequence;
        try(var statement=connection.prepareStatement("SELECT COALESCE(MAX(sequence),0) FROM bookmark");var row=statement.executeQuery()) {
            row.next();sequence=row.getLong(1);
        }
        if(sequence==Long.MAX_VALUE) throw new ResponseStatusException(HttpStatus.CONFLICT,"Bookmark sequence exhausted");
        try(var statement=connection.prepareStatement("INSERT INTO bookmark(node_id,sequence) VALUES(?,?)")) {
            statement.setLong(1,id);statement.setLong(2,sequence+1);
            if(statement.executeUpdate()!=1)throw new SQLException("Bookmark insert failed");
        }
        com.turkerozturk.node.NodeTimestamps.touchOccurrence(connection,id,
                com.turkerozturk.node.NodeTimestamps.now());
    }
    /** Also permits explicit removal of an orphan bookmark; no content row is required or deleted. */
    static void remove(Connection connection,long id) throws SQLException {
        if(id<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid bookmark ID");
        try(var statement=connection.prepareStatement("DELETE FROM bookmark WHERE node_id=?")) {
            statement.setLong(1,id);
            if (statement.executeUpdate() != 0) com.turkerozturk.node.NodeTimestamps.touchOccurrence(
                    connection,id,com.turkerozturk.node.NodeTimestamps.now());
        }
    }
}

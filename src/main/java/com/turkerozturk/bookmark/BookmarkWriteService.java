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

/** Changes bookmark occurrences without updating node content, timestamps or tree placement. */
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
    }
    /** Also permits explicit removal of an orphan bookmark; no content row is required or deleted. */
    static void remove(Connection connection,long id) throws SQLException {
        if(id<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid bookmark ID");
        try(var statement=connection.prepareStatement("DELETE FROM bookmark WHERE node_id=?")) {
            statement.setLong(1,id);statement.executeUpdate();
        }
    }
}

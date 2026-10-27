package com.turkerozturk.node.navigation;

import com.turkerozturk.bookmark.BookmarkShortcuts;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.*;
import java.util.List;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NodeNavigationService {
    @PersistenceContext private EntityManager entityManager;

    public boolean exists(long id) {
        return entityManager.unwrap(Session.class).doReturningWork(connection -> exists(connection, id));
    }

    static boolean exists(Connection connection, long id) throws SQLException {
        try (var query = connection.prepareStatement("SELECT c.node_id FROM children c JOIN node n ON n.node_id="
                + "CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END WHERE c.node_id=?")) {
            query.setLong(1, id);
            try (var rows = query.executeQuery()) { return rows.next(); }
        }
    }

    public List<BookmarkShortcuts.Shortcut> bookmarks() {
        return entityManager.unwrap(Session.class).doReturningWork(BookmarkShortcuts::read);
    }
}

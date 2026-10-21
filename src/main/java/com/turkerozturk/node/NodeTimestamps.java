package com.turkerozturk.node;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

/** Uses CTB epoch seconds and the caller's transaction; shared occurrences touch their master. */
public final class NodeTimestamps {
    private NodeTimestamps() { }
    public static long now() { return Instant.now().getEpochSecond(); }

    public static void touchOccurrence(EntityManager manager, long id, long timestamp) {
        manager.createNativeQuery("UPDATE node SET ts_lastsave = :timestamp WHERE node_id = "
                + "(SELECT CASE WHEN COALESCE(master_id,0)=0 THEN node_id ELSE master_id END "
                + "FROM children WHERE node_id = :id)")
                .setParameter("timestamp", timestamp).setParameter("id", id).executeUpdate();
    }

    public static void touchOccurrence(Connection connection, long id, long timestamp) throws SQLException {
        try (var query = connection.prepareStatement("SELECT CASE WHEN COALESCE(master_id,0)=0 "
                + "THEN node_id ELSE master_id END FROM children WHERE node_id=?")) {
            query.setLong(1,id);
            Long content = null;
            try (var rows = query.executeQuery()) { if (rows.next()) content = rows.getLong(1); }
            if (content != null) touchContent(connection,content,timestamp);
        }
    }

    /** Missing rows are permitted when removing orphan bookmarks or wholly deleted groups. */
    public static void touchContent(Connection connection, long id, long timestamp) throws SQLException {
        try (var update = connection.prepareStatement("UPDATE node SET ts_lastsave=? WHERE node_id=?")) {
            update.setLong(1,timestamp); update.setLong(2,id); update.executeUpdate();
        }
    }
}

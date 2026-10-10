package com.turkerozturk.multipledatabases;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Explicit SQLite CTB compatibility checks; never infers arbitrary column types or defaults. */
final class CtbSchemaCompatibility {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CtbSchemaCompatibility.class);
    static final String SETTING = "custom.allowLegacySchemaUpgrade";
    static final String UPGRADE_SQL = "ALTER TABLE children ADD COLUMN master_id INTEGER DEFAULT 0";
    private static final Map<String, List<String>> REQUIRED = new LinkedHashMap<>();
    static {
        REQUIRED.put("node", List.of("node_id", "name", "txt", "syntax", "tags", "is_ro", "is_richtxt", "has_codebox", "has_table", "has_image", "level", "ts_creation", "ts_lastsave"));
        REQUIRED.put("children", List.of("node_id", "father_id", "sequence"));
        REQUIRED.put("bookmark", List.of("node_id", "sequence"));
        REQUIRED.put("image", List.of("node_id", "offset", "justification", "anchor", "png", "filename", "link", "time"));
        REQUIRED.put("grid", List.of("node_id", "offset", "justification", "txt", "col_min", "col_max"));
        REQUIRED.put("codebox", List.of("node_id", "offset", "justification", "txt", "syntax", "width", "height", "is_width_pix", "do_highl_bra", "do_show_linenum"));
    }

    static void ensure(Connection connection, boolean enabled, boolean administrator) throws SQLException {
        List<String> missing = new ArrayList<>();
        for (var entry : REQUIRED.entrySet()) {
            var columns = columns(connection, entry.getKey());
            for (String name : entry.getValue()) {
                if (!columns.contains(name)) missing.add(entry.getKey() + "." + name);
            }
            if (entry.getKey().equals("children") && !columns.contains("master_id")) missing.add("children.master_id");
        }
        if (missing.isEmpty()) return;
        boolean supported = missing.equals(List.of("children.master_id"));
        if (!supported || !enabled || !administrator) throw new Problem(missing, supported);

        // SQLite transactional DDL: failure rolls back the complete supported change.
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            if (!columns(connection, "children").contains("master_id")) {
                try (var statement = connection.createStatement()) { statement.executeUpdate(UPGRADE_SQL); }
            }
            if (!columns(connection, "children").contains("master_id")) throw new SQLException("CTB schema upgrade verification failed");
            connection.commit();
            LOGGER.info("CTB legacy schema upgrade completed: children.master_id INTEGER DEFAULT 0");
        } catch (SQLException | RuntimeException error) {
            try { connection.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
            throw error;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private static Set<String> columns(Connection connection, String table) throws SQLException {
        Set<String> result = new HashSet<>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) result.add(rows.getString("name").toLowerCase(Locale.ROOT));
        }
        return result;
    }

    static final class Problem extends SQLException {
        private static final long serialVersionUID = 1L;
        private final List<String> missing;
        private final boolean supported;
        Problem(List<String> missing, boolean supported) {
            super("Incompatible CTB schema: missing " + String.join(", ", missing) +
                    (supported ? "; supported upgrade: " + UPGRADE_SQL + "; enable " + SETTING + " and select as ADMIN" : "; no automatic migration is defined"));
            this.missing = List.copyOf(missing);
            this.supported = supported;
        }
        public List<String> getMissing() { return missing; }
        public boolean isSupported() { return supported; }
        public String getSql() { return supported ? UPGRADE_SQL : ""; }
    }
}

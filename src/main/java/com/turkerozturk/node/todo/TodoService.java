package com.turkerozturk.node.todo;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeTimestamps;
import com.turkerozturk.node.TrashSql;
import com.turkerozturk.node.audit.AuditSql;
import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.node.tags.NodeTags;
import jakarta.persistence.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Tasks use ordinary tags; listings never load note content or object BLOBs. */
@Service
public class TodoService {
    @PersistenceContext private EntityManager manager;
    private final NodePropertiesService properties;
    public TodoService(NodePropertiesService properties) { this.properties = properties; }
    public record Options(String q, String status, int limit, int page) {
        public Options {
            if (q == null || q.length() > 500 || !Set.of("open", "done", "all").contains(status)
                    || limit < 1 || limit > 200 || page < 1 || page > 1_000_000)
                throw new IllegalArgumentException("Invalid task filters");
        }
    }
    public record Task(long id, long contentId, String name, String icon, boolean bold, String color,
            List<String> tags, boolean done, int locations, String breadcrumb, String revision) { }
    public record View(List<Task> rows, long open, long done, long matched, Options options) {
        public long total() { return open + done; }
        public long pages() { return Math.max(1, (matched + options.limit() - 1) / options.limit()); }
        public long first() { return rows.isEmpty() ? 0 : (long)(options.page() - 1) * options.limit() + 1; }
        public long last() { return rows.isEmpty() ? 0 : first() + rows.size() - 1; }
    }
    private record Meta(long id, String name, String raw, Long modified, String icon, boolean bold, String color) {
        List<String> tags() { return NodeTags.split(raw); }
    }
    private record Branch(long id, long parent, long content) { }
    @Transactional(readOnly = true)
    public View inspect(Options options) {
        return manager.unwrap(Session.class).doReturningWork(c -> inspect(c, options));
    }
    static View inspect(Connection c, Options options) throws SQLException {
        var nodes = new HashMap<Long, Meta>();
        try (var q = c.createStatement(); var rs = q.executeQuery("SELECT node_id,name,tags,ts_lastsave,is_ro,is_richtxt,syntax FROM node")) {
            while (rs.next()) {
                var display = new Node(); display.setIsReadOnly16bit(rs.getLong(5)); display.setIsRichText(rs.getLong(6));
                display.setSyntax(Objects.requireNonNullElse(rs.getString(7), "plain-text"));
                display.parseNodeIconAndReadOnlyFlag(); display.parseNodeTitleColorAndBoldnessAndTextType();
                long stamp = rs.getLong(4); Long modified = rs.wasNull() ? null : stamp;
                nodes.put(rs.getLong(1), new Meta(rs.getLong(1), Objects.requireNonNullElse(rs.getString(2), ""), rs.getString(3),
                        modified, display.getNodeIcon().getIconName(), display.isBoldnessBit(), String.format(Locale.ROOT, "#%06X", display.getTitleColor())));
            }
        }
        var tree = new TreeMap<Long, Branch>();
        try (var q = c.createStatement(); var rs = q.executeQuery("SELECT node_id,father_id,COALESCE(master_id,0) FROM children")) {
            while (rs.next()) { long id = rs.getLong(1), master = rs.getLong(3); tree.put(id, new Branch(id, rs.getLong(2), master == 0 ? id : master)); }
        }
        var locations = new HashMap<Long, List<Long>>(); var visibility = new HashMap<Long, Boolean>();
        for (var b : tree.values()) {
            var meta = nodes.get(b.content()); var master = tree.get(b.content());
            if (meta == null || master == null || master.content() != master.id() || !meta.tags().contains("todo")) continue;
            if (visible(b.id(), tree, nodes, visibility)) locations.computeIfAbsent(b.content(), unused -> new ArrayList<>()).add(b.id());
        }
        var tasks = new ArrayList<Task>(); long open = 0, done = 0; String query = normalize(options.q());
        for (var entry : locations.entrySet()) {
            var meta = nodes.get(entry.getKey()); var tags = meta.tags(); boolean completed = tags.contains("done");
            if (completed) done++; else open++;
            var ids = entry.getValue(); long id = ids.contains(meta.id()) ? meta.id() : ids.get(0);
            if (options.status().equals("open") && completed || options.status().equals("done") && !completed) continue;
            if (!normalize(meta.name() + " " + String.join(" ", tags) + " " + meta.id() + " " + ids).contains(query)) continue;
            tasks.add(new Task(id, meta.id(), meta.name(), meta.icon(), meta.bold(), meta.color(), tags, completed,
                    ids.size(), "", revision(id, meta.id(), meta.raw(), meta.modified())));
        }
        tasks.sort(Comparator.comparing(Task::name, NodeTags.ORDER).thenComparingLong(Task::contentId));
        long matched = tasks.size(), offset = (long)(options.page() - 1) * options.limit(); var page = new ArrayList<Task>();
        for (long i = offset; i < Math.min(matched, offset + options.limit()); i++) {
            var t = tasks.get((int)i);
            page.add(new Task(t.id(), t.contentId(), t.name(), t.icon(), t.bold(), t.color(), t.tags(), t.done(), t.locations(),
                    breadcrumb(t.id(), tree, nodes), t.revision()));
        }
        return new View(List.copyOf(page), open, done, matched, options);
    }
    private static boolean visible(long id, Map<Long, Branch> tree, Map<Long, Meta> nodes, Map<Long, Boolean> cache) {
        var path = new LinkedHashSet<Long>(); boolean result = true;
        while (id != 0) {
            if (cache.containsKey(id)) { result = cache.get(id); break; }
            if (!path.add(id)) { result = false; break; }
            var b = tree.get(id); var meta = b == null ? null : nodes.get(b.content());
            if (meta == null || !tree.containsKey(b.content()) || tree.get(b.content()).content() != b.content() || meta.tags().contains(TrashSql.TAG) || meta.tags().contains(AuditSql.TAG)) { result = false; break; }
            id = b.parent();
        }
        for (long seen : path) cache.put(seen, result);
        return result;
    }
    private static String breadcrumb(long id, Map<Long, Branch> tree, Map<Long, Meta> nodes) {
        var path = new ArrayDeque<String>(); var seen = new HashSet<Long>();
        while (id != 0 && seen.size() < 200 && seen.add(id)) {
            var b = tree.get(id); var m = b == null ? null : nodes.get(b.content());
            if (m == null) break;
            path.addFirst(m.name().substring(0, Math.min(300, m.name().length())) + " [" + id + "]"); id = b.parent();
        }
        return String.join(" / ", path);
    }
    private static String normalize(String text) {
        return java.text.Normalizer.normalize(text.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('ı', 'i');
    }
    private static String revision(long id, long content, String tags, Long modified) {
        try {
            String raw = id + "\n" + content + "\n" + modified + "\n" + (tags == null ? "NULL" : "TEXT" + tags);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    @Transactional
    public void update(long id, boolean completed, String expected) {
        if (!properties.writable()) throw problem(HttpStatus.FORBIDDEN, "readonly");
        manager.unwrap(Session.class).doWork(c -> update(c, id, completed, expected));
        manager.clear();
    }
    static void update(Connection c, long id, boolean completed, String expected) throws SQLException {
        if (id <= 0) throw problem(HttpStatus.BAD_REQUEST, "invalid");
        String suffix = c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite") ? "" : " FOR UPDATE";
        activePath(c, id, suffix);
        try (var q = c.prepareStatement("SELECT n.node_id,n.tags,n.ts_lastsave FROM children c JOIN node n ON n.node_id=CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END JOIN children m ON m.node_id=n.node_id AND COALESCE(m.master_id,0)=0 WHERE c.node_id=?" + suffix)) {
            q.setLong(1, id);
            try (var rs = q.executeQuery()) {
                if (!rs.next()) throw problem(HttpStatus.NOT_FOUND, "missing");
                long content = rs.getLong(1); String raw = rs.getString(2); long stamp = rs.getLong(3); Long modified = rs.wasNull() ? null : stamp;
                if (!revision(id, content, raw, modified).equals(expected)) throw problem(HttpStatus.CONFLICT, "changed");
                var tags = new LinkedHashSet<>(NodeTags.split(raw));
                if (!tags.contains("todo") || tags.contains(TrashSql.TAG) || tags.contains(AuditSql.TAG)) throw problem(HttpStatus.CONFLICT, "changed");
                if (completed == tags.contains("done")) return;
                if (completed) tags.add("done"); else tags.remove("done");
                final String updated;
                try { updated = NodeTags.normalize(String.join(" ", tags)); }
                catch (IllegalArgumentException invalid) { throw problem(HttpStatus.BAD_REQUEST, "invalid"); }
                try (var write = c.prepareStatement("UPDATE node SET tags=?,ts_lastsave=? WHERE node_id=?" + (modified == null ? " AND ts_lastsave IS NULL" : " AND ts_lastsave=?") + (raw == null ? " AND tags IS NULL" : " AND tags=?"))) {
                    write.setString(1, updated); write.setLong(2, NodeTimestamps.now()); write.setLong(3, content);
                    int parameter = 4; if (modified != null) write.setLong(parameter++, modified); if (raw != null) write.setString(parameter, raw);
                    if (write.executeUpdate() != 1) throw problem(HttpStatus.CONFLICT, "changed");
                }
            }
        }
    }
    /** Check and lock the live occurrence path; tagging never uses subtree deletion rules. */
    private static void activePath(Connection c, long id, String suffix) throws SQLException {
        var seen = new HashSet<Long>();
        try (var q = c.prepareStatement("SELECT c.father_id,n.tags FROM children c JOIN node n ON n.node_id=CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END JOIN children m ON m.node_id=n.node_id AND COALESCE(m.master_id,0)=0 WHERE c.node_id=?" + suffix)) {
            while (id != 0) {
                if (!seen.add(id)) throw problem(HttpStatus.CONFLICT,"changed");
                q.setLong(1,id);
                try (var rs = q.executeQuery()) {
                    if (!rs.next()) throw problem(HttpStatus.NOT_FOUND,"missing");
                    var tags = NodeTags.split(rs.getString(2));
                    if (tags.contains(TrashSql.TAG) || tags.contains(AuditSql.TAG)) throw problem(HttpStatus.CONFLICT,"changed");
                    id = rs.getLong(1);
                }
            }
        }
    }
    private static ResponseStatusException problem(HttpStatus status, String reason) { return new ResponseStatusException(status, reason); }
}

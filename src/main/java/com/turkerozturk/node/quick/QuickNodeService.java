package com.turkerozturk.node.quick;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.creation.ChildNodeService;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.sql.*;
import java.text.Normalizer;
import java.util.*;

/** Title metadata only; no text/BLOB scan and no CTB schema changes. */
@Service
public class QuickNodeService {
    public static final int MAX_TEXT = 200_000;
    static final int SCAN_LIMIT = 50_000, RESULT_LIMIT = 40;
    @PersistenceContext private EntityManager entityManager;
    private final ChildNodeService creation;
    private final NodePropertiesService properties;
    public QuickNodeService(ChildNodeService creation, NodePropertiesService properties) {
        this.creation = creation; this.properties = properties;
    }
    public boolean writable() { return properties.writable(); }
    public record Target(String id, String name, String icon, boolean bold, String color,
                         boolean shared, String breadcrumb) { }
    public record Search(List<Target> items, boolean more, boolean scanLimited) { }
    private record Entry(long id, long parent, String name, String icon, boolean bold, String color, boolean shared, String normalized) { }

    @Transactional(readOnly = true)
    public Search search(String query) {
        if (query == null || query.length() > 160 || query.indexOf('\0') >= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (query.isBlank()) return new Search(List.of(), false, false);
        return entityManager.unwrap(Session.class).doReturningWork(connection -> search(connection, query));
    }

    static Search search(Connection connection, String query) throws SQLException {
        String needle = fold(query.strip());
        if (needle.isEmpty()) return new Search(List.of(), false, false);
        Map<Long,Entry> entries = new HashMap<>(); boolean limited = false;
        String sql = "SELECT c.node_id,c.father_id,COALESCE(c.master_id,0),n.name,n.is_ro,n.is_richtxt,n.syntax "
                + "FROM children c JOIN node n ON n.node_id=CASE WHEN COALESCE(c.master_id,0)=0 "
                + "THEN c.node_id ELSE c.master_id END ORDER BY c.node_id";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setMaxRows(SCAN_LIMIT + 1);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    if (entries.size() == SCAN_LIMIT) { limited = true; break; }
                    var display = new Node(); display.setIsReadOnly16bit(rows.getLong(5));
                    display.setIsRichText(rows.getLong(6));
                    display.setSyntax(Objects.requireNonNullElse(rows.getString(7), "plain-text"));
                    display.parseNodeIconAndReadOnlyFlag(); display.parseNodeTitleColorAndBoldnessAndTextType();
                    long id = rows.getLong(1);
                    entries.put(id, new Entry(id, rows.getLong(2), Objects.requireNonNullElse(rows.getString(4), ""),
                            display.getNodeIcon().getIconName(), display.isBoldnessBit(),
                            String.format(Locale.ROOT,"#%06X", display.getTitleColor()), rows.getLong(3) != 0, fold(Objects.requireNonNullElse(rows.getString(4), ""))));
                }
            }
        }
        var matches = entries.values().stream().filter(e -> e.normalized().contains(needle))
                .sorted(Comparator.comparingInt((Entry e) -> e.normalized().equals(needle) ? 0 : e.normalized().startsWith(needle) ? 1 : 2)
                        .thenComparing(Entry::normalized).thenComparingLong(Entry::id)).toList();
        var result = matches.stream().limit(RESULT_LIMIT).map(e -> new Target(Long.toString(e.id()), e.name(), e.icon(),
                e.bold(), e.color(), e.shared(), breadcrumb(e, entries))).toList();
        return new Search(result, matches.size() > RESULT_LIMIT, limited);
    }
    private static final java.util.regex.Pattern MARKS = java.util.regex.Pattern.compile("\\p{M}");
    private static String fold(String value) {
        return MARKS.matcher(Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD))
                .replaceAll("").replace('ı', 'i');
    }
    private static String breadcrumb(Entry entry, Map<Long,Entry> entries) {
        var parts = new LinkedList<String>(); var seen = new HashSet<Long>();
        long id = entry.id();
        for (int depth=0; id != 0 && depth<64; depth++) {
            if (!seen.add(id)) { parts.addFirst("…"); break; }
            Entry current = entries.get(id);
            if (current == null) { parts.addFirst("[" + id + "]"); break; }
            parts.addFirst(current.name() + " [" + id + "]"); id = current.parent();
        }
        return String.join(" / ", parts);
    }

    /** Creation and source assignment share one transaction; existing creation/timestamp rules apply. */
    @Transactional
    public long create(long parent, String title, String text, String syntax) {
        if (!properties.writable()) throw new AccessDeniedException("The selected CTB is read-only.");
        if (parent < 0 || title == null || title.isBlank() || title.length()>255 || title.indexOf('\0')>=0
                || text == null || text.length()>MAX_TEXT || text.indexOf('\0')>=0
                || !("plain-text".equals(syntax) || "markdown".equals(syntax)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        long id = parent == 0 ? creation.createTopLevel() : creation.create(parent);
        int updated = entityManager.createNativeQuery("UPDATE node SET name=:name,txt=:text,syntax=:syntax WHERE node_id=:id")
                .setParameter("name", title.strip()).setParameter("text", text).setParameter("syntax", syntax)
                .setParameter("id", id).executeUpdate();
        if (updated != 1) throw new IllegalStateException("New note was not found");
        return id;
    }
}

package com.turkerozturk.richtext.editing;

import com.turkerozturk.node.Node;
import jakarta.persistence.*;
import java.sql.*;
import java.util.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Current-tenant occurrence metadata only; never reads node text or attachment BLOBs. */
@Service
@Transactional(readOnly = true)
public class InternalRichTextTargets {
    @PersistenceContext private EntityManager entityManager;
    public record Target(String nodeId, String name, String iconName, boolean bold, String color, List<String> anchors) { }
    public record Matches(List<Target> targets, boolean truncated) { }
    private static final String SELECT = "SELECT c.node_id,n.name,n.is_ro,n.is_richtxt,n.syntax,n.node_id FROM children c JOIN node n ON n.node_id="
            + "CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END ";

    public Target target(long id) {
        return entityManager.unwrap(Session.class).doReturningWork(connection -> read(connection,id,true));
    }
    public Matches matches(String name) {
        return entityManager.unwrap(Session.class).doReturningWork(connection -> matches(connection,name));
    }
    static Matches matches(Connection connection, String name) throws SQLException {
        var matches = new ArrayList<Target>();
        try (var query = connection.prepareStatement(SELECT + "WHERE n.name=? ORDER BY c.node_id")) {
            query.setString(1,name);
            try (var rows = query.executeQuery()) {
                while (rows.next()) {
                    // MySQL/MariaDB collations can be case/accent insensitive. Require actual equality.
                    if (!name.equals(rows.getString(2))) continue;
                    if (matches.size() == 200) return new Matches(List.copyOf(matches),true);
                    matches.add(display(rows,List.of()));
                }
            }
        }
        return new Matches(List.copyOf(matches),false);
    }
    static Target read(Connection connection, long id, boolean withAnchors) throws SQLException {
        try (var query = connection.prepareStatement(SELECT + "WHERE c.node_id=?")) {
            query.setLong(1,id);
            try (var rows = query.executeQuery()) {
                if (!rows.next()) return null;
                long contentId = rows.getLong(6);
                var anchors = new TreeSet<String>();
                if (withAnchors) try (var objects = connection.prepareStatement("SELECT anchor FROM image WHERE node_id=? AND anchor IS NOT NULL AND anchor<>'' ORDER BY anchor")) {
                    objects.setLong(1,contentId);
                    try (var values = objects.executeQuery()) { while(values.next()) anchors.add(values.getString(1)); }
                }
                return display(rows,List.copyOf(anchors));
            }
        }
    }
    private static Target display(ResultSet rows, List<String> anchors) throws SQLException {
        var display = new Node();
        display.setIsReadOnly16bit(rows.getLong(3)); display.setIsRichText(rows.getLong(4));
        display.setSyntax(Objects.requireNonNullElse(rows.getString(5),"plain-text"));
        display.parseNodeIconAndReadOnlyFlag(); display.parseNodeTitleColorAndBoldnessAndTextType();
        return new Target(Long.toString(rows.getLong(1)),Objects.requireNonNullElse(rows.getString(2),""),
                display.getNodeIcon().getIconName(),display.isBoldnessBit(),String.format(Locale.ROOT,"#%06X",display.getTitleColor()),anchors);
    }
    /** Rechecks new/changed links on the same JDBC transaction used to save rich text. */
    static void validate(Connection connection, String link) throws SQLException {
        if (link.length()>4096 || link.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid internal link");
        var matcher = java.util.regex.Pattern.compile("node ([1-9][0-9]{0,18})(?: (.+))?").matcher(link);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid internal link");
        final long id;
        try { id=Long.parseLong(matcher.group(1)); } catch(NumberFormatException error){throw new IllegalArgumentException("Invalid node ID",error);}
        String anchor=matcher.group(2);
        Target target=read(connection,id,anchor!=null);
        if (target==null || anchor!=null && !target.anchors().contains(anchor)) throw new IllegalArgumentException("Internal link target no longer exists");
    }
}

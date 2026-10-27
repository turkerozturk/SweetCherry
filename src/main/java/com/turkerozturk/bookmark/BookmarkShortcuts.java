package com.turkerozturk.bookmark;

import java.sql.*;
import java.util.*;

/** Read-only display metadata shared by navigation and move pickers; no text/BLOB loads. */
public final class BookmarkShortcuts {
    private BookmarkShortcuts() { }
    public record Shortcut(String nodeId, String name, String iconName, boolean bold, String color) { }
    public static List<Shortcut> read(Connection connection) throws SQLException {
        var targets = new ArrayList<Shortcut>();
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
                targets.add(new Shortcut(Long.toString(rows.getLong(1)),
                        Objects.requireNonNullElse(rows.getString(2),""),display.getNodeIcon().getIconName(),
                        display.isBoldnessBit(),String.format(Locale.ROOT,"#%06X",display.getTitleColor())));
            }
        }
        return List.copyOf(targets);
    }
}

package com.turkerozturk.export;

import com.turkerozturk.richtext.experimental.RichTextDocument;
import com.turkerozturk.richtext.experimental.RichTextXmlReader;
import com.turkerozturk.richtext.experimental.RichTextXmlWriter;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Rewrites only CTB link attributes; visible text and embedded-object offsets never change. */
final class CtbExportLinks {
    private static final Pattern INTERNAL = Pattern.compile("^node\\s+([0-9]+)(\\s.*)?$", Pattern.DOTALL);
    private record Target(boolean shared, String name) { }
    private record Issue(long owner, String ownerName, String type, String target, String name, String text) { }
    private final Map<Long, Target> sourceTargets = new HashMap<>();
    private final Map<Long, Long> ids;
    private final long container;
    private final List<Issue> issues = new ArrayList<>();
    private final RichTextXmlReader reader = new RichTextXmlReader();
    private final RichTextXmlWriter writer = new RichTextXmlWriter();

    CtbExportLinks(Connection source, Map<Long, Long> ids, long container) throws SQLException {
        this.ids = Map.copyOf(ids);
        this.container = container;
        // Snapshot names and occurrence types before writing, including an in-place collection export.
        try (var statement = source.createStatement(); var rows = statement.executeQuery(
                "SELECT c.node_id,COALESCE(c.master_id,0),n.name FROM children c LEFT JOIN node n ON n.node_id="
                + "CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END")) {
            while (rows.next()) sourceTargets.put(rows.getLong(1),
                    new Target(rows.getLong(2) != 0, rows.getString(3)));
        }
    }

    String richText(long owner, String name, String xml) {
        if (xml == null || xml.isBlank()) return xml;
        try {
            var original = reader.read(xml);
            var rewritten = new ArrayList<RichTextDocument.TextRun>();
            boolean changed = false;
            // A styled link may span several consecutive runs: report its full visible label once.
            for (int i = 0; i < original.runs().size();) {
                var first = original.runs().get(i);
                String link = first.attributes().get("link");
                int end = i + 1;
                var label = new StringBuilder(first.text());
                if (link != null) while (end < original.runs().size()
                        && link.equals(original.runs().get(end).attributes().get("link")))
                    label.append(original.runs().get(end++).text());
                String replacement = rewrite(owner, name, link, label.toString());
                for (; i < end; i++) {
                    var run = original.runs().get(i);
                    if (!Objects.equals(link, replacement)) {
                        var attributes = new LinkedHashMap<>(run.attributes());
                        attributes.put("link", replacement);
                        run = new RichTextDocument.TextRun(run.text(), attributes, run.textOffset());
                        changed = true;
                    }
                    rewritten.add(run);
                }
            }
            return changed ? writer.write(new RichTextDocument(rewritten)) : xml;
        } catch (IllegalArgumentException failure) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid rich-text XML in exported source node " + owner);
        }
    }

    String image(long owner, String link, String filename, long offset) {
        var content = sourceTargets.get(owner);
        String name = content == null ? "" : content.name;
        return rewrite(owner, name, link,
                "[image object at offset " + offset + (filename == null ? "" : ": " + filename) + "]");
    }

    private String rewrite(long owner, String ownerName, String link, String text) {
        if (link == null) return null;
        var match = INTERNAL.matcher(link);
        if (!match.matches()) return link; // HTTP(S), file/folder links and other attributes are untouched.
        String rawId = match.group(1);
        Long oldId;
        try { oldId = Long.valueOf(rawId); }
        catch (NumberFormatException failure) { oldId = null; }
        Long mapped = oldId == null ? null : ids.get(oldId);
        if (mapped != null) return "node " + mapped + Objects.toString(match.group(2), "");
        var target = oldId == null ? null : sourceTargets.get(oldId);
        boolean missing = target == null || target.name == null;
        issues.add(new Issue(owner, Objects.toString(ownerName, ""),
                missing ? "orphan" : target.shared ? "shared node" : "node", rawId,
                missing ? "—" : target.name, text));
        // A source anchor cannot exist on the new report node; deliberately discard its suffix.
        return "node " + container;
    }

    String report() {
        var text = new StringBuilder("CTB export — internal links\n\n")
                .append("Links to exported occurrences keep their mapped targets. Links to missing source targets ")
                .append("or targets outside this export now open this container. Source IDs below belong to the source database.\n\n")
                .append("Source type | Source ID | Source target name | Internal link text\n");
        if (issues.isEmpty()) text.append("No missing or excluded internal-link targets.\n");
        long previous = -1;
        for (var issue : issues) {
            if (previous != issue.owner) {
                text.append("\nLinks in source node ").append(issue.owner).append(" — ").append(issue.ownerName).append('\n');
                previous = issue.owner;
            }
            text.append(issue.type).append(" | ").append(issue.target).append(" | ")
                    .append(issue.name).append(" | ").append(issue.text).append('\n');
        }
        return writer.write(new RichTextDocument(List.of(
                new RichTextDocument.TextRun(text.toString(), Map.of(), 0))));
    }
}

package com.turkerozturk.richtext.editing;

import com.turkerozturk.jdbc.JdbcIdentifiers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.richtext.experimental.*;

/** Edits real-node text while preserving every existing CTB object and binary payload. */
@Service
public class RichTextEditingService {
    private final NodePropertiesService properties;
    private final EmbeddedUploadPolicy uploads;
    @PersistenceContext private EntityManager entityManager;
    public RichTextEditingService(NodePropertiesService properties, EmbeddedUploadPolicy uploads) { this.properties = properties; this.uploads = uploads; }

    public record Editor(Node node, RichTextDocument document, String revision, Map<String, ObjectInfo> objects, int maxEmbeddedFileBytes) { }
    public record ObjectInfo(String label, String imageUrl, List<List<String>> tableRows, RichTextCodeboxes.Value codebox) { }
    static record Stored(ProtectedRichTextCodec.Reference reference, Object[] values, Map<String, Object> columns) { }

    /** Checks actual object tables as well as read-only and alias restrictions before offering an editor. */
    @Transactional(readOnly = true)
    public Editor open(long id) {
        Node node = editableNode(id);
        RichTextDocument document = "custom-colors".equals(node.getSyntax())
                ? new RichTextXmlReader().read(node.getTxt())
                : new RichTextDocument(List.of(new RichTextDocument.TextRun(node.getTxt() == null ? "" : node.getTxt(), Map.of(), 0)));
        var objects = storedObjects(id);
        var info = new java.util.LinkedHashMap<String, ObjectInfo>();
        for (var object : objects) {
            String table = object.reference().table(), label = table;
            String imageUrl = null;
            if (table.equals("image")) {
                String anchor = object.values()[4] == null ? "" : object.values()[4].toString();
                String filename = object.values()[6] == null ? "" : object.values()[6].toString();
                if (!anchor.isEmpty()) label = "⚓ " + anchor;
                else if (!filename.isEmpty()) label = "📎 " + filename;
                else { label = "Image"; imageUrl = "/images/" + id + "/" + object.reference().offset(); }
            }
            List<List<String>> tableRows = null;
            if (table.equals("grid")) {
                try { tableRows = RichTextTableCodec.open(tableXml(object)); }
                catch (IllegalArgumentException ignored) { /* Unsupported table remains protected and untouched. */ }
            }
            info.put(object.reference().key(), new ObjectInfo(label, imageUrl, tableRows, table.equals("codebox") ? codebox(object) : null));
        }
        return new Editor(node, new ProtectedRichTextCodec().open(document, references(objects)), revision(node, objects), info, uploads.fileBytes());
    }

    private Node editableNode(long id) {
        if (!properties.writable()) throw new AccessDeniedException("The selected CTB is read-only.");
        Node node = properties.realNode(id);
        if ((node.getIsReadOnly16bit() & 1) != 0) throw new AccessDeniedException("The node is read-only.");
        if (!"plain-text".equals(node.getSyntax()) && !"custom-colors".equals(node.getSyntax())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only plain-text or rich-text nodes can use this editor.");
        }
        return node;
    }

    /** Saves canonical CTB XML and the rich-text bit together, refusing stale pages or newly added objects. */
    @Transactional
    public void save(long id, String xml, String expectedRevision) {
        save(id, xml, expectedRevision, "{}");
    }

    /** Saves text, protected offsets and new PNG rows atomically; uploads are never persisted on GET or cancel. */
    @Transactional
    public void save(long id, String xml, String expectedRevision, String imageJson) {
        save(id, xml, expectedRevision, imageJson, "{}");
    }

    /** Validates attachments/images together and commits their payloads, offsets and node metadata atomically. */
    @Transactional
    public void save(long id, String xml, String expectedRevision, String imageJson, String fileJson) {
        save(id, xml, expectedRevision, imageJson, fileJson, "{}");
    }

    /** Saves table cell edits and new tables without changing existing table metadata or other object payloads. */
    @Transactional
    public void save(long id, String xml, String expectedRevision, String imageJson, String fileJson, String tableJson) {
        save(id, xml, expectedRevision, imageJson, fileJson, tableJson, "[]", false);
    }

    /** Deletes explicitly selected objects and saves surviving offsets/payloads in the same tenant transaction. */
    @Transactional
    public void save(long id, String xml, String expectedRevision, String imageJson, String fileJson,
            String tableJson, String deletedJson, boolean allowObjectDeletion) {
        save(id, xml, expectedRevision, imageJson, fileJson, tableJson, deletedJson, allowObjectDeletion, "{}");
    }

    /** Codebox changes share revision validation, object offsets, undo state and the same atomic transaction. */
    @Transactional
    public void save(long id, String xml, String expectedRevision, String imageJson, String fileJson,
            String tableJson, String deletedJson, boolean allowObjectDeletion, String codeboxJson) {
        Node node = editableNode(id);
        var objects = storedObjects(id);
        if (!revision(node, objects).equals(expectedRevision)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Node changed; reload the editor.");
        String canonical;
        ProtectedRichTextCodec.Saved saved;
        Map<String, byte[]> images;
        Map<String, PendingRichTextFiles.File> files;
        Map<String, String> tables;
        Map<String, RichTextCodeboxes.Value> codeboxes;
        java.util.Set<String> deleted;
        List<Stored> survivors;
        try {
            deleted = RichTextObjectDeletions.decode(deletedJson, allowObjectDeletion, references(objects));
            survivors = objects.stream().filter(object -> !deleted.contains(object.reference().key())).toList();
            if (imageJson == null || fileJson == null || (long) imageJson.length() + fileJson.length() > EmbeddedUploadPolicy.JSON_CHARACTERS)
                throw new IllegalArgumentException("Upload too large");
            int limit = uploads.fileBytes();
            images = PendingRichTextImages.decode(imageJson, limit);
            files = PendingRichTextFiles.decode(fileJson, limit);
            if (images.size() + files.size() > 10 || images.values().stream().mapToLong(bytes -> bytes.length).sum()
                    + files.values().stream().mapToLong(file -> file.bytes().length).sum() > EmbeddedUploadPolicy.TOTAL_BYTES)
                throw new IllegalArgumentException("Upload too large");
            var tableRows = RichTextTableCodec.decode(tableJson);
            var tableXmls = new java.util.LinkedHashMap<String, String>();
            for (var entry : tableRows.entrySet()) {
                if (deleted.contains(entry.getKey())) throw new IllegalArgumentException("Deleted table cannot be edited");
                Stored original = objects.stream().filter(object -> object.reference().key().equals(entry.getKey())).findFirst().orElse(null);
                if (!entry.getKey().startsWith("new-table:") && original == null) throw new IllegalArgumentException("Unknown table");
                tableXmls.put(entry.getKey(), RichTextTableCodec.write(entry.getValue(), original == null ? null : tableXml(original)));
            }
            tables = Map.copyOf(tableXmls);
            codeboxes = RichTextCodeboxes.decode(codeboxJson);
            for (var entry : codeboxes.entrySet()) {
                Stored original = objects.stream().filter(object -> object.reference().key().equals(entry.getKey())).findFirst().orElse(null);
                if (deleted.contains(entry.getKey()) || !entry.getKey().startsWith("new-codebox:") && original == null)
                    throw new IllegalArgumentException("Unknown/deleted codebox");
                if (!RichTextCodeboxes.supported(entry.getValue().syntax())
                        && (original == null || !codebox(original).syntax().equals(entry.getValue().syntax())))
                    throw new IllegalArgumentException("Unsupported codebox syntax");
            }
            var keys = new java.util.HashSet<String>(images.keySet()); keys.addAll(files.keySet());
            tables.keySet().stream().filter(key -> key.startsWith("new-table:")).forEach(keys::add);
            codeboxes.keySet().stream().filter(key -> key.startsWith("new-codebox:")).forEach(keys::add);
            if (keys.size() > 10) throw new IllegalArgumentException("Too many new objects");
            saved = new ProtectedRichTextCodec().save(new RichTextXmlReader().read(xml), references(survivors), keys);
            var original = "custom-colors".equals(node.getSyntax()) ? new RichTextXmlReader().read(node.getTxt())
                    : new RichTextDocument(List.of());
            entityManager.unwrap(org.hibernate.Session.class).doWork(connection ->
                ExternalRichTextLinks.validateChanges(saved.text(), original, link -> {
                    if (link.startsWith("node ")) {
                        try { InternalRichTextTargets.validate(connection,link); }
                        catch(java.sql.SQLException error){throw new IllegalStateException("Cannot validate internal link",error);}
                    } else ExternalRichTextLinks.validate(link);
                }));
            canonical = new RichTextXmlWriter().write(saved.text());
        }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rich-text XML", error); }
        // Validate revision, object protections and payloads before deciding this is a no-op.
        // Preserve the original XML bytes and object rows too when nothing effectively changed.
        boolean unchangedTables = tables.entrySet().stream().allMatch(entry -> objects.stream()
                .filter(object -> object.reference().key().equals(entry.getKey()))
                .anyMatch(object -> RichTextTableCodec.open(tableXml(object)).equals(
                        RichTextTableCodec.open(entry.getValue()))));
        boolean unchangedOffsets = survivors.stream().allMatch(object ->
                saved.offsets().get(object.reference()) == object.reference().offset());
        if ("custom-colors".equals(node.getSyntax()) && (node.getIsRichText() & 1L) != 0
                && deleted.isEmpty() && images.isEmpty() && files.isEmpty()
                && codeboxes.entrySet().stream().allMatch(entry -> objects.stream().anyMatch(object ->
                    object.reference().key().equals(entry.getKey()) && codebox(object).equals(entry.getValue())))
                && unchangedTables && unchangedOffsets
                && RichTextChanges.sameText(new RichTextXmlReader().read(node.getTxt()), saved.text())) return;
        int updated = entityManager.createNativeQuery("UPDATE node SET txt = :xml, syntax = 'custom-colors', "
                + "is_richtxt = (is_richtxt | 1), ts_lastsave = :timestamp WHERE node_id = :id AND (is_ro & 1) = 0 "
                + "AND txt = :oldText AND syntax = :oldSyntax AND is_richtxt = :oldRich")
                .setParameter("xml", canonical).setParameter("timestamp", Instant.now().getEpochSecond())
                .setParameter("id", id).setParameter("oldText", node.getTxt()).setParameter("oldSyntax", node.getSyntax())
                .setParameter("oldRich", node.getIsRichText()).executeUpdate();
        if (updated != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Node changed; reload the editor.");
        for (var object : objects) if (deleted.contains(object.reference().key())) {
            entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> {
                try (var statement = connection.prepareStatement("DELETE FROM " + object.reference().table()
                        + " WHERE node_id = ? AND "+JdbcIdentifiers.quote(connection,"offset")+" = ?")) {
                    statement.setLong(1, id); statement.setInt(2, object.reference().offset());
                    if (statement.executeUpdate() != 1) throw new java.sql.SQLException("Object changed; reload the editor");
                }
            });
        }
        // Move all keys to a disjoint range first to avoid composite-key collisions.
        for (var object : survivors) move(id, object.reference().table(), object.reference().offset(), -object.reference().offset() - 1);
        for (var object : survivors) move(id, object.reference().table(), -object.reference().offset() - 1, saved.offsets().get(object.reference()));
        var pending = new java.util.LinkedHashMap<String, PendingRichTextFiles.File>();
        images.forEach((key, bytes) -> pending.put(key, new PendingRichTextFiles.File("", bytes)));
        pending.putAll(files);
        for (var entry : pending.entrySet()) {
            entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> {
                try (var insert = connection.prepareStatement("INSERT INTO image "
                        + "(node_id, "+JdbcIdentifiers.quote(connection,"offset")+", justification, anchor, png, filename, link, time) VALUES (?, ?, 'left', '', ?, ?, '', ?)")) {
                    insert.setLong(1, id); insert.setInt(2, saved.newImages().get(entry.getKey()));
                    insert.setBytes(3, entry.getValue().bytes()); insert.setString(4, entry.getValue().name());
                    insert.setLong(5, entry.getValue().name().isEmpty() ? 0 : Instant.now().getEpochSecond());
                    if (insert.executeUpdate() != 1) throw new java.sql.SQLException("Image insert failed");
                }
            });
        }
        if (!pending.isEmpty()) entityManager.createNativeQuery("UPDATE node SET has_image = 1 WHERE node_id = :id")
                .setParameter("id", id).executeUpdate();
        for (var entry : tables.entrySet()) {
            boolean fresh = entry.getKey().startsWith("new-table:");
            var reference = fresh ? null : objects.stream().map(Stored::reference).filter(ref -> ref.key().equals(entry.getKey())).findFirst().orElseThrow();
            int offset = fresh ? saved.newImages().get(entry.getKey()) : saved.offsets().get(reference);
            entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> {
                String sql = fresh ? "INSERT INTO grid (txt, node_id, "+JdbcIdentifiers.quote(connection,"offset")+", justification, col_min, col_max) VALUES (?, ?, ?, 'left', 200, 200)"
                        : "UPDATE grid SET txt = ? WHERE node_id = ? AND "+JdbcIdentifiers.quote(connection,"offset")+" = ?";
                try (var statement = connection.prepareStatement(sql)) {
                    statement.setString(1, entry.getValue()); statement.setLong(2, id); statement.setInt(3, offset);
                    if (statement.executeUpdate() != 1) throw new java.sql.SQLException("Table changed; reload the editor");
                }
            });
        }
        if (tables.keySet().stream().anyMatch(key -> key.startsWith("new-table:"))) entityManager.createNativeQuery("UPDATE node SET has_table = 1 WHERE node_id = :id")
                .setParameter("id", id).executeUpdate();
        for (var entry : codeboxes.entrySet()) {
            boolean fresh = entry.getKey().startsWith("new-codebox:");
            var reference = fresh ? null : objects.stream().map(Stored::reference).filter(ref -> ref.key().equals(entry.getKey())).findFirst().orElseThrow();
            int offset = fresh ? saved.newImages().get(entry.getKey()) : saved.offsets().get(reference);
            entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> {
                String sql = fresh ? "INSERT INTO codebox (txt, syntax, node_id, " + JdbcIdentifiers.quote(connection,"offset")
                        + ", justification, width, height, is_width_pix, do_highl_bra, do_show_linenum) VALUES (?, ?, ?, ?, 'left', 300, 150, 1, 1, 0)"
                        : "UPDATE codebox SET txt = ?, syntax = ? WHERE node_id = ? AND " + JdbcIdentifiers.quote(connection,"offset") + " = ?";
                try (var statement = connection.prepareStatement(sql)) {
                    statement.setString(1, entry.getValue().text()); statement.setString(2, entry.getValue().syntax());
                    statement.setLong(3, id); statement.setInt(4, offset);
                    if (statement.executeUpdate() != 1) throw new java.sql.SQLException("Codebox changed; reload the editor");
                }
            });
        }
        if (codeboxes.keySet().stream().anyMatch(key -> key.startsWith("new-codebox:")))
            entityManager.createNativeQuery("UPDATE node SET has_codebox = 1 WHERE node_id = :id").setParameter("id", id).executeUpdate();
        if (!deleted.isEmpty()) entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> {
            try (var statement = connection.prepareStatement("UPDATE node SET "
                    + "has_image = CASE WHEN EXISTS(SELECT 1 FROM image WHERE node_id = ?) THEN 1 ELSE 0 END, "
                    + "has_table = CASE WHEN EXISTS(SELECT 1 FROM grid WHERE node_id = ?) THEN 1 ELSE 0 END, "
                    + "has_codebox = CASE WHEN EXISTS(SELECT 1 FROM codebox WHERE node_id = ?) THEN 1 ELSE 0 END WHERE node_id = ?")) {
                for (int parameter = 1; parameter <= 4; parameter++) statement.setLong(parameter, id);
                if (statement.executeUpdate() != 1) throw new java.sql.SQLException("Node changed; reload the editor");
            }
        });
        entityManager.clear();
    }

    private static RichTextCodeboxes.Value codebox(Stored object) {
        Object text = object.columns().get("txt"), syntax = object.columns().get("syntax");
        String source = text instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : text == null ? "" : text.toString();
        return new RichTextCodeboxes.Value(source, syntax == null ? "plain-text" : syntax.toString());
    }

    /** SQLite may expose table XML as text or UTF-8 binary, depending on how the CTB was written. */
    private static String tableXml(Stored object) {
        Object value = object.values()[4];
        if (value instanceof byte[] bytes) {
            try { return StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString(); }
            catch (java.nio.charset.CharacterCodingException error) { throw new IllegalArgumentException("Invalid table UTF-8", error); }
        }
        return value == null ? "" : value.toString();
    }

    /** Uses the current tenant transaction's JDBC connection, avoiding inferred Hibernate scalar types. */
    private List<Stored> storedObjects(long id) {
        return entityManager.unwrap(org.hibernate.Session.class)
                .doReturningWork(connection -> readObjects(connection, id));
    }

    /** Reads mixed CTB rows with SQLite-supported getters; binary, empty and NULL values stay distinct. */
    static List<Stored> readObjects(java.sql.Connection connection, long id) throws java.sql.SQLException {
        var result = new java.util.ArrayList<Stored>();
        for (String table : List.of("image", "codebox", "grid")) {
            try (var query = connection.prepareStatement("SELECT "+JdbcIdentifiers.quote(connection,"offset")+" AS editor_offset, " + table + ".* FROM " + table
                    + " WHERE node_id = ? ORDER BY "+JdbcIdentifiers.quote(connection,"offset"))) {
                query.setLong(1, id);
                try (var rows = query.executeQuery()) {
                    var metadata = rows.getMetaData();
                    while (rows.next()) {
                        Object[] row = new Object[metadata.getColumnCount()];
                        for (int column = 1; column <= row.length; column++) {
                            row[column - 1] = table.equals("image") && metadata.getColumnName(column).equalsIgnoreCase("png")
                                    ? rows.getBytes(column) : rows.getObject(column);
                        }
                        var columns = new java.util.HashMap<String, Object>();
                        for (int column = 2; column <= row.length; column++) columns.put(metadata.getColumnName(column).toLowerCase(java.util.Locale.ROOT), row[column - 1]);
                        result.add(new Stored(new ProtectedRichTextCodec.Reference(table, rows.getInt(1)), row, java.util.Collections.unmodifiableMap(columns)));
                    }
                }
            }
        }
        result.sort(java.util.Comparator.comparingInt(object -> object.reference().offset()));
        return result;
    }
    private List<ProtectedRichTextCodec.Reference> references(List<Stored> objects) {
        return objects.stream().map(Stored::reference).toList();
    }
    /** Updates only the offset column; payload/metadata and row count are preserved. */
    private void move(long id, String table, int from, int to) {
        entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> {
            String offset=JdbcIdentifiers.quote(connection,"offset");
            try(var statement=connection.prepareStatement("UPDATE "+table+" SET "+offset+"=? WHERE node_id=? AND "+offset+"=?")) {
                statement.setInt(1,to);statement.setLong(2,id);statement.setInt(3,from);
                if(statement.executeUpdate()!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Object changed; reload the editor.");
            }
        });
    }

    /** Covers the source content/type and flags so a stale editor cannot silently overwrite changes. */
    private String revision(Node node, List<Stored> objects) {
        try {
            String source = node.getSyntax() + "\u0000" + node.getTxt() + "\u0000" + node.getIsRichText() + "\u0000" + node.getIsReadOnly16bit();
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(source.getBytes(StandardCharsets.UTF_8));
            for (var object : objects) {
                digest.update(object.reference().key().getBytes(StandardCharsets.UTF_8));
                for (var value : object.values()) {
                    byte[] bytes = value instanceof byte[] binary ? binary : String.valueOf(value).getBytes(StandardCharsets.UTF_8);
                    digest.update((byte) (value == null ? 0 : value instanceof byte[] ? 1 : 2));
                    digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array()); digest.update(bytes);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}

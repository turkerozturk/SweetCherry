package com.turkerozturk.richtext.editing;

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
    public record ObjectInfo(String label, String imageUrl) { }
    static record Stored(ProtectedRichTextCodec.Reference reference, Object[] values) { }

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
            info.put(object.reference().key(), new ObjectInfo(label, imageUrl));
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
        Node node = editableNode(id);
        var objects = storedObjects(id);
        if (!revision(node, objects).equals(expectedRevision)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Node changed; reload the editor.");
        String canonical;
        ProtectedRichTextCodec.Saved saved;
        Map<String, byte[]> images;
        Map<String, PendingRichTextFiles.File> files;
        try {
            if (imageJson == null || fileJson == null || (long) imageJson.length() + fileJson.length() > EmbeddedUploadPolicy.JSON_CHARACTERS)
                throw new IllegalArgumentException("Upload too large");
            int limit = uploads.fileBytes();
            images = PendingRichTextImages.decode(imageJson, limit);
            files = PendingRichTextFiles.decode(fileJson, limit);
            if (images.size() + files.size() > 10 || images.values().stream().mapToLong(bytes -> bytes.length).sum()
                    + files.values().stream().mapToLong(file -> file.bytes().length).sum() > EmbeddedUploadPolicy.TOTAL_BYTES)
                throw new IllegalArgumentException("Upload too large");
            var keys = new java.util.HashSet<String>(images.keySet()); keys.addAll(files.keySet());
            saved = new ProtectedRichTextCodec().save(new RichTextXmlReader().read(xml), references(objects), keys);
            var original = "custom-colors".equals(node.getSyntax()) ? new RichTextXmlReader().read(node.getTxt())
                    : new RichTextDocument(List.of());
            ExternalRichTextLinks.validateChanges(saved.text(), original);
            canonical = new RichTextXmlWriter().write(saved.text());
        }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rich-text XML", error); }
        int updated = entityManager.createNativeQuery("UPDATE node SET txt = :xml, syntax = 'custom-colors', "
                + "is_richtxt = (is_richtxt | 1), ts_lastsave = :timestamp WHERE node_id = :id AND (is_ro & 1) = 0 "
                + "AND txt = :oldText AND syntax = :oldSyntax AND is_richtxt = :oldRich")
                .setParameter("xml", canonical).setParameter("timestamp", Instant.now().getEpochSecond())
                .setParameter("id", id).setParameter("oldText", node.getTxt()).setParameter("oldSyntax", node.getSyntax())
                .setParameter("oldRich", node.getIsRichText()).executeUpdate();
        if (updated != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Node changed; reload the editor.");
        // Move all keys to a disjoint range first to avoid composite-key collisions.
        for (var object : objects) move(id, object.reference().table(), object.reference().offset(), -object.reference().offset() - 1);
        for (var object : objects) move(id, object.reference().table(), -object.reference().offset() - 1, saved.offsets().get(object.reference()));
        var pending = new java.util.LinkedHashMap<String, PendingRichTextFiles.File>();
        images.forEach((key, bytes) -> pending.put(key, new PendingRichTextFiles.File("", bytes)));
        pending.putAll(files);
        for (var entry : pending.entrySet()) {
            entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> {
                try (var insert = connection.prepareStatement("INSERT INTO image "
                        + "(node_id, offset, justification, anchor, png, filename, link, time) VALUES (?, ?, 'left', '', ?, ?, '', ?)")) {
                    insert.setLong(1, id); insert.setInt(2, saved.newImages().get(entry.getKey()));
                    insert.setBytes(3, entry.getValue().bytes()); insert.setString(4, entry.getValue().name());
                    insert.setLong(5, entry.getValue().name().isEmpty() ? 0 : Instant.now().getEpochSecond());
                    if (insert.executeUpdate() != 1) throw new java.sql.SQLException("Image insert failed");
                }
            });
        }
        if (!pending.isEmpty()) entityManager.createNativeQuery("UPDATE node SET has_image = 1 WHERE node_id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.clear();
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
            try (var query = connection.prepareStatement("SELECT offset AS editor_offset, * FROM " + table
                    + " WHERE node_id = ? ORDER BY offset")) {
                query.setLong(1, id);
                try (var rows = query.executeQuery()) {
                    var metadata = rows.getMetaData();
                    while (rows.next()) {
                        Object[] row = new Object[metadata.getColumnCount()];
                        for (int column = 1; column <= row.length; column++) {
                            row[column - 1] = table.equals("image") && metadata.getColumnName(column).equalsIgnoreCase("png")
                                    ? rows.getBytes(column) : rows.getObject(column);
                        }
                        result.add(new Stored(new ProtectedRichTextCodec.Reference(table, rows.getInt(1)), row));
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
        int changed = entityManager.createNativeQuery("UPDATE " + table + " SET offset = :to WHERE node_id = :id AND offset = :from")
                .setParameter("to", to).setParameter("id", id).setParameter("from", from).executeUpdate();
        if (changed != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Object changed; reload the editor.");
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

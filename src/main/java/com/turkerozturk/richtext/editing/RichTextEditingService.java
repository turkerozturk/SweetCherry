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

/** First rich-text editing stage: real nodes without any embedded objects. */
@Service
public class RichTextEditingService {
    private final NodePropertiesService properties;
    @PersistenceContext private EntityManager entityManager;
    public RichTextEditingService(NodePropertiesService properties) { this.properties = properties; }

    public record Editor(Node node, RichTextDocument document, String revision) { }

    /** Checks actual object tables as well as read-only and alias restrictions before offering an editor. */
    @Transactional(readOnly = true)
    public Editor open(long id) {
        Node node = editableNode(id);
        RichTextDocument document = "custom-colors".equals(node.getSyntax())
                ? new RichTextXmlReader().read(node.getTxt())
                : new RichTextDocument(List.of(new RichTextDocument.TextRun(node.getTxt() == null ? "" : node.getTxt(), Map.of(), 0)));
        return new Editor(node, document, revision(node));
    }

    private Node editableNode(long id) {
        if (!properties.writable()) throw new AccessDeniedException("The selected CTB is read-only.");
        Node node = properties.realNode(id);
        if ((node.getIsReadOnly16bit() & 1) != 0) throw new AccessDeniedException("The node is read-only.");
        if (!"plain-text".equals(node.getSyntax()) && !"custom-colors".equals(node.getSyntax())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only plain-text or rich-text nodes can use this editor.");
        }
        Number count = (Number) entityManager.createNativeQuery("SELECT (SELECT count(*) FROM image WHERE node_id = :id) "
                + "+ (SELECT count(*) FROM codebox WHERE node_id = :id) + (SELECT count(*) FROM grid WHERE node_id = :id)")
                .setParameter("id", id).getSingleResult();
        if (count.longValue() != 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Embedded objects are not editable in this version.");
        return node;
    }

    /** Saves canonical CTB XML and the rich-text bit together, refusing stale pages or newly added objects. */
    @Transactional
    public void save(long id, String xml, String expectedRevision) {
        Node node = editableNode(id);
        if (!revision(node).equals(expectedRevision)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Node changed; reload the editor.");
        String canonical;
        try { canonical = new RichTextXmlWriter().write(new RichTextXmlReader().read(xml)); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rich-text XML", error); }
        int updated = entityManager.createNativeQuery("UPDATE node SET txt = :xml, syntax = 'custom-colors', "
                + "is_richtxt = (is_richtxt | 1), ts_lastsave = :timestamp WHERE node_id = :id AND (is_ro & 1) = 0 "
                + "AND txt = :oldText AND syntax = :oldSyntax AND is_richtxt = :oldRich "
                + "AND NOT EXISTS (SELECT 1 FROM image WHERE node_id = :id) "
                + "AND NOT EXISTS (SELECT 1 FROM codebox WHERE node_id = :id) "
                + "AND NOT EXISTS (SELECT 1 FROM grid WHERE node_id = :id)")
                .setParameter("xml", canonical).setParameter("timestamp", Instant.now().getEpochSecond())
                .setParameter("id", id).setParameter("oldText", node.getTxt()).setParameter("oldSyntax", node.getSyntax())
                .setParameter("oldRich", node.getIsRichText()).executeUpdate();
        if (updated != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Node changed; reload the editor.");
    }

    /** Covers the source content/type and flags so a stale editor cannot silently overwrite changes. */
    private String revision(Node node) {
        try {
            String source = node.getSyntax() + "\u0000" + node.getTxt() + "\u0000" + node.getIsRichText() + "\u0000" + node.getIsReadOnly16bit();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}

package com.turkerozturk.pdf;

import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.node.NodeService;
import com.turkerozturk.richtext.experimental.RichTextRenderingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NodePdfExportService {
    private final ChildrenService children;
    private final NodeService nodes;
    private final RichTextRenderingService richText;
    private final NodePdfRenderer renderer;
    // One PDF at a time per application; avoids concurrent large render jobs on Raspberry Pi.
    private final java.util.concurrent.Semaphore permit = new java.util.concurrent.Semaphore(1);
    public NodePdfExportService(ChildrenService children, NodeService nodes, RichTextRenderingService richText, NodePdfRenderer renderer) {
        this.children = children; this.nodes = nodes; this.richText = richText; this.renderer = renderer;
    }
    private PdfExportLimits limits = new PdfExportLimits();
    @org.springframework.beans.factory.annotation.Autowired
    public void configureLimits(PdfExportLimits limits) { this.limits = limits; }

    public record Export(String title, byte[] bytes) {}

    @Transactional(readOnly = true)
    public Export export(long occurrenceId) {
        return export(occurrenceId, PdfExportOptions.defaults(), null);
    }

    @Transactional(readOnly = true)
    public String title(long occurrenceId) {
        var occurrence = children.findById(occurrenceId);
        if (occurrence == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        long contentId = occurrence.getMasterId() == null || occurrence.getMasterId() == 0 ? occurrenceId : occurrence.getMasterId();
        var node = nodes.findById(contentId);
        if (node == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return node.getName();
    }

    @Transactional(readOnly = true)
    public Export export(long occurrenceId, PdfExportOptions options, PdfSourceMetadata.Source source) {
        return export(occurrenceId, options, source, com.turkerozturk.richtext.experimental.PdfObjectSelection.all());
    }

    @Transactional(readOnly = true)
    public Export export(long occurrenceId, PdfExportOptions options, PdfSourceMetadata.Source source,
            com.turkerozturk.richtext.experimental.PdfObjectSelection objects) {
        if (!permit.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        try {
            var occurrence = children.findById(occurrenceId);
            if (occurrence == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            long contentId = occurrence.getMasterId() == null || occurrence.getMasterId() == 0
                    ? occurrenceId : occurrence.getMasterId();
            var node = nodes.findById(contentId);
            if (node == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            String text = node.getTxt() == null ? "" : node.getTxt();
            if (text.length() > limits.getMaxNodeTextCharacters()) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
            String html;
            if ("custom-colors".equals(node.getSyntax()) && !text.isBlank()) html = renderRichText(node, objects);
            else {
                var fragment = org.jsoup.Jsoup.parseBodyFragment("");
                fragment.body().appendElement("div").text(text);
                fragment.outputSettings().prettyPrint(false);
                html = fragment.body().html();
            }
            return new Export(node.getName(), renderer.render(new NodePdfDocument(limits).prepare(node.getName(), html, contentId, options, source, PdfFilename.create(node.getName(), occurrenceId))));
        } finally { permit.release(); }
    }

    @Transactional(readOnly = true)
    public Export exportSubtree(long occurrenceId, PdfExportOptions options,
            PdfSourceMetadata.Source source, boolean contents, String contentsTitle) {
        return exportSubtree(occurrenceId, options, source, contents, contentsTitle,
                com.turkerozturk.richtext.experimental.PdfObjectSelection.all());
    }

    @Transactional(readOnly = true)
    public Export exportSubtree(long occurrenceId, PdfExportOptions options,
            PdfSourceMetadata.Source source, boolean contents, String contentsTitle,
            com.turkerozturk.richtext.experimental.PdfObjectSelection objects) {
        if (!permit.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        try {
            var root = children.findById(occurrenceId);
            if (root == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            var byParent = new java.util.HashMap<Long, java.util.List<com.turkerozturk.children.Children>>();
            for (var child : children.getChildren())
                byParent.computeIfAbsent(child.getFatherId(), ignored -> new java.util.ArrayList<>()).add(child);
            for (var list : byParent.values()) list.sort(java.util.Comparator
                    .comparingLong(com.turkerozturk.children.Children::getSequence)
                    .thenComparingLong(com.turkerozturk.children.Children::getNodeId));
            record Pending(com.turkerozturk.children.Children occurrence, int depth) {}
            var stack = new java.util.ArrayDeque<Pending>();
            stack.push(new Pending(root, 0));
            var seen = new java.util.HashSet<Long>();
            var parts = new java.util.ArrayList<SubtreePdfDocument.Part>();
            long total = 0;
            while (!stack.isEmpty()) {
                var pending = stack.pop(); var occurrence = pending.occurrence();
                if (!seen.add(occurrence.getNodeId()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT);
                if (parts.size() >= limits.getMaxNodes() || pending.depth() > limits.getMaxDepth())
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
                boolean shared = occurrence.getMasterId() != null && occurrence.getMasterId() != 0;
                long contentId = shared ? occurrence.getMasterId() : occurrence.getNodeId();
                var node = nodes.findById(contentId);
                if (node == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                String text = node.getTxt() == null ? "" : node.getTxt();
                if (text.length() > limits.getMaxNodeTextCharacters())
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
                String html;
                if ("custom-colors".equals(node.getSyntax()) && !text.isBlank()) html = renderRichText(node, objects);
                else {
                    var fragment = org.jsoup.Jsoup.parseBodyFragment("");
                    fragment.outputSettings().prettyPrint(false);
                    fragment.body().appendElement("div").text(text);
                    html = fragment.body().html();
                }
                total += html.length();
                if (total > limits.getMaxHtmlCharacters()) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
                parts.add(new SubtreePdfDocument.Part(occurrence.getNodeId(), contentId,
                        pending.depth(), node.getName(), html));
                // Shared occurrences are leaves in the readers; do not expand the master's subtree.
                if (!shared) {
                    var list = byParent.getOrDefault(occurrence.getNodeId(), java.util.List.of());
                    for (int n = list.size() - 1; n >= 0; n--) stack.push(new Pending(list.get(n), pending.depth() + 1));
                }
            }
            return new Export(parts.get(0).title(), renderer.render(new SubtreePdfDocument(limits)
                    .prepare(parts, options, source, contents, contentsTitle)));
        } finally { permit.release(); }
    }
    private String renderRichText(com.turkerozturk.node.Node node,
            com.turkerozturk.richtext.experimental.PdfObjectSelection objects) {
        return objects.equals(com.turkerozturk.richtext.experimental.PdfObjectSelection.all())
                ? richText.renderForPdf(node, "") : richText.renderForPdf(node, "", objects);
    }
}

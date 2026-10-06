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
    public record Export(String title, byte[] bytes) {}

    @Transactional(readOnly = true)
    public Export export(long occurrenceId) {
        if (!permit.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        try {
            var occurrence = children.findById(occurrenceId);
            if (occurrence == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            long contentId = occurrence.getMasterId() == null || occurrence.getMasterId() == 0
                    ? occurrenceId : occurrence.getMasterId();
            var node = nodes.findById(contentId);
            if (node == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            String text = node.getTxt() == null ? "" : node.getTxt();
            if (text.length() > 8 * 1024 * 1024) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
            String html;
            if ("custom-colors".equals(node.getSyntax()) && !text.isBlank()) html = richText.render(node, "");
            else {
                var fragment = org.jsoup.Jsoup.parseBodyFragment("");
                fragment.body().appendElement("div").text(text);
                fragment.outputSettings().prettyPrint(false);
                html = fragment.body().html();
            }
            return new Export(node.getName(), renderer.render(new NodePdfDocument().prepare(node.getName(), html, contentId)));
        } finally { permit.release(); }
    }
}

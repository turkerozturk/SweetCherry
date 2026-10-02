package com.turkerozturk.richtext.experimental;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.turkerozturk.node.NodeRepository;
import com.turkerozturk.node.NodeContentParserService;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.image.ImageService;
import com.turkerozturk.richtext.experimental.RichTextLayout.EmbeddedObject;

/** Read-only comparison of the existing renderer with the experimental pipeline. */
@Service
public class RichTextPreviewService {
    private static final Logger log = LoggerFactory.getLogger(RichTextPreviewService.class);
    private final NodeRepository nodes;
    private final ChildrenRepository children;
    private final ImageService images;
    private final NodeContentParserService legacy;

    public RichTextPreviewService(NodeRepository nodes, ChildrenRepository children, ImageService images,
                                  NodeContentParserService legacy) {
        this.nodes = nodes; this.children = children; this.images = images; this.legacy = legacy;
    }

    public record Pane(String html, boolean failed) { }
    public record Preview(long treeNodeId, long contentNodeId, String name, Pane legacy, Pane experimental) { }

    /** Resolves aliases and loads lazy object collections within one read-only transaction. */
    @Transactional(readOnly = true)
    public Preview compare(long treeNodeId, String tenantView) {
        var child = children.findByNodeId(treeNodeId);
        if (child == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found");
        long contentId = child.getMasterId() == null || child.getMasterId() == 0 ? treeNodeId : child.getMasterId();
        var node = nodes.findById(contentId);
        if (node == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content node not found");
        if (!"custom-colors".equals(node.getSyntax())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rich-text preview requires custom-colors syntax");
        }
        Pane oldPane;
        try {
            oldPane = new Pane(previewHtml(legacy.parseNodeTxt(node)), false);
        } catch (RuntimeException error) {
            log.warn("Legacy rich-text preview failed for node {}", contentId, error);
            oldPane = new Pane("", true);
        }
        Pane newPane;
        try {
            var references = new CtbObjectReferences();
            var adapter = new EmbeddedContentAdapter();
            var payloads = new LinkedHashMap<EmbeddedObject, EmbeddedContent>();
            for (var image : images.getImagesByNodeId(contentId)) {
                add(payloads, references.fromImage(image), adapter.fromImage(image));
            }
            for (var box : node.getCodeBoxes()) add(payloads, references.fromCodeBox(box), adapter.fromCodeBox(box));
            for (var table : node.getGrids()) add(payloads, references.fromTable(table), adapter.fromTable(table));
            var document = new RichTextXmlReader().read(node.getTxt());
            var layout = new RichTextLayoutBuilder().build(document, new java.util.ArrayList<>(payloads.keySet()));
            newPane = new Pane(previewHtml(new RichTextObjectHtmlRenderer().render(layout, payloads, tenantView)), false);
        } catch (RuntimeException error) {
            log.warn("Experimental rich-text preview failed for node {}", contentId, error);
            newPane = new Pane("", true);
        }
        return new Preview(treeNodeId, contentId, node.getName(), oldPane, newPane);
    }

    /** Makes comparison output non-interactive; the iframe sandbox remains the security boundary. */
    private String previewHtml(String html) {
        var document = org.jsoup.Jsoup.parse(html);
        document.outputSettings().prettyPrint(false);
        document.select("script,iframe,object,embed,base,meta,link,form").remove();
        document.select("a").removeAttr("href").removeAttr("target");
        document.select("input,button,select,textarea").attr("disabled", "disabled");
        document.head().appendElement("meta").attr("charset", "UTF-8");
        document.head().appendElement("meta").attr("http-equiv", "Content-Security-Policy")
                .attr("content", "default-src 'none'; img-src 'self' data:; style-src 'unsafe-inline'; form-action 'none'; base-uri 'none'");
        document.head().appendElement("style").text("body{margin:12px;font-family:system-ui,sans-serif;white-space:pre-wrap;overflow-wrap:anywhere;}"
                + "img{max-width:100%;height:auto;}table{border-collapse:collapse;}td,th{border:1px solid #ccc;padding:4px;}"
                + "pre{max-width:100%;overflow-x:auto;}");
        return document.outerHtml();
    }

    /** Prevents duplicate references from silently replacing an earlier payload. */
    private void add(Map<EmbeddedObject, EmbeddedContent> payloads, EmbeddedObject ref, EmbeddedContent payload) {
        if (payloads.putIfAbsent(ref, payload) != null) throw new IllegalArgumentException("Duplicate object reference");
    }
}

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
            oldPane = new Pane(previewHtml(legacy.parseNodeTxt(node), contentId), false);
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
            newPane = new Pane(previewHtml(new RichTextObjectHtmlRenderer().render(layout, payloads, tenantView), contentId), false);
        } catch (RuntimeException error) {
            log.warn("Experimental rich-text preview failed for node {}", contentId, error);
            newPane = new Pane("", true);
        }
        return new Preview(treeNodeId, contentId, node.getName(), oldPane, newPane);
    }

    /** Makes comparison output non-interactive; the iframe sandbox remains the security boundary. */
    private String previewHtml(String html, long contentId) {
        var document = org.jsoup.Jsoup.parse(html);
        document.outputSettings().prettyPrint(false);
        document.select("script,iframe,object,embed,base,meta,link,form").remove();
        for (var link : document.select("a")) {
            String href = link.attr("href");
            try {
                var uri = java.net.URI.create(href);
                if (("/nodes/" + contentId).equals(uri.getPath()) && uri.getRawFragment() != null
                        && uri.getScheme() == null && uri.getRawAuthority() == null) {
                    href = "#" + uri.getRawFragment();
                }
            } catch (IllegalArgumentException ignored) { }
            if (href.startsWith("#")) {
                link.attr("href", "about:srcdoc" + href).attr("target", "_self").removeAttr("rel");
                continue;
            }
            boolean internal = href.matches("/(nodes/[1-9][0-9]*|download/[1-9][0-9]*/[0-9]+)\\?_tenantView=[^\\s]*");
            boolean external = false;
            try {
                var uri = java.net.URI.create(href);
                external = ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null;
            } catch (IllegalArgumentException ignored) { }
            if (!internal && !external && !href.startsWith("#")) link.removeAttr("href");
            else if (!href.startsWith("#")) link.attr("target", "_blank").attr("rel", "noopener noreferrer");
        }
        for (var element : document.getAllElements()) {
            for (var attribute : new java.util.ArrayList<>(element.attributes().asList())) {
                if (attribute.getKey().toLowerCase(java.util.Locale.ROOT).startsWith("on")) element.removeAttr(attribute.getKey());
            }
        }
        for (var image : document.select("img[src]")) {
            String src = image.attr("src");
            var matcher = java.util.regex.Pattern.compile("/images/([1-9][0-9]*)/([0-9]+)").matcher(src);
            if (matcher.matches() && matcher.group(1).equals(Long.toString(contentId))) {
                var stored = images.findByNodeIdAndOffset(contentId, Integer.valueOf(matcher.group(2)));
                if (stored.isPresent() && stored.get().getPng() != null) {
                    image.attr("src", "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(stored.get().getPng()));
                } else image.removeAttr("src");
            } else if (!src.startsWith("data:image/png;base64,")) image.removeAttr("src");
        }
        document.select("input,button,select,textarea").attr("disabled", "disabled");
        document.head().appendElement("meta").attr("charset", "UTF-8");
        document.head().appendElement("meta").attr("http-equiv", "Content-Security-Policy")
                .attr("content", "default-src 'none'; img-src data:; style-src 'unsafe-inline'; form-action 'none'; base-uri 'none'");
        try (var css = getClass().getResourceAsStream("/static/css/thirdparty/highlightjs/default.min.css")) {
            if (css == null) throw new IllegalStateException("Highlight stylesheet is missing");
            String styles = new String(css.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("(?s)/\\*[^*]*[#@]\\s*sourceMappingURL=.*?\\*/", "");
            document.head().appendElement("style").text(styles);
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot read highlight stylesheet", error); }
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

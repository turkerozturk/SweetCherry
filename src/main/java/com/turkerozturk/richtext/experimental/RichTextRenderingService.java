package com.turkerozturk.richtext.experimental;

import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.turkerozturk.node.Node;
import com.turkerozturk.image.ImageService;
import com.turkerozturk.richtext.experimental.RichTextLayout.EmbeddedObject;

/** Shared rich-text pipeline for comparison and the new reader/workspace views. */
@Service
public class RichTextRenderingService {
    private final ImageService images;
    public RichTextRenderingService(ImageService images) { this.images = images; }

    /** Loads object snapshots and produces escaped HTML without invoking the legacy parser. */
    @Transactional(readOnly = true)
    public String render(Node node, String tenantView) {
        return render(node, tenantView, false);
    }

    @Transactional(readOnly = true)
    public String renderForPdf(Node node, String tenantView) {
        return render(node, tenantView, true);
    }

    private String render(Node node, String tenantView, boolean pdfHeadings) {
        long contentId = node.getNodeId();
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
        return new RichTextObjectHtmlRenderer(pdfHeadings).render(layout, payloads, tenantView);
    }

    /** Keeps existing image zoom endpoints and turns same-node anchors into in-page links. */
    @Transactional(readOnly = true)
    public String renderLive(Node node, String tenantView) {
        var document = org.jsoup.Jsoup.parseBodyFragment(render(node, tenantView));
        document.outputSettings().prettyPrint(false);
        // Inline PNGs are emitted in buffer order; use the same order for original-image URLs.
        var pictures = images.getImagesByNodeId(node.getNodeId()).stream()
                .filter(image -> (image.getAnchor() == null || image.getAnchor().isEmpty())
                        && (image.getFileName() == null || image.getFileName().isEmpty()))
                .sorted(java.util.Comparator.comparingInt(com.turkerozturk.image.Image::getOffset)).toList();
        var elements = document.select("img.rich-text-image");
        if (pictures.size() != elements.size()) throw new IllegalArgumentException("Image layout mismatch");
        String token = java.net.URLEncoder.encode(tenantView == null ? "" : tenantView, java.nio.charset.StandardCharsets.UTF_8);
        for (int i = 0; i < pictures.size(); i++) {
            elements.get(i).attr("src", "/images/" + node.getNodeId() + "/" + pictures.get(i).getOffset() + "?_tenantView=" + token);
        }
        for (var link : document.select("a[href]")) {
            var uri = java.net.URI.create(link.attr("href"));
            if (("/nodes/" + node.getNodeId()).equals(uri.getPath()) && uri.getRawFragment() != null) {
                link.attr("href", "#" + uri.getRawFragment()).removeAttr("target");
            }
        }
        return document.body().html();
    }

    private void add(java.util.Map<EmbeddedObject, EmbeddedContent> payloads, EmbeddedObject ref, EmbeddedContent payload) {
        if (payloads.putIfAbsent(ref, payload) != null) throw new IllegalArgumentException("Duplicate object reference");
    }
}

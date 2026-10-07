package com.turkerozturk.pdf;

import java.util.List;
import org.w3c.dom.Element;

/** Combines independently sanitized node content without merging occurrence/anchor identities. */
public final class SubtreePdfDocument {
    private final PdfExportLimits limits;
    public SubtreePdfDocument() { this(new PdfExportLimits()); }
    public SubtreePdfDocument(PdfExportLimits limits) { this.limits = limits; }
    public record Part(long occurrenceId, long contentId, int depth, String title, String html) {}

    public org.w3c.dom.Document prepare(List<Part> parts, PdfExportOptions options,
            PdfSourceMetadata.Source source, boolean contents, String contentsTitle) {
        if (parts.isEmpty()) throw new IllegalArgumentException("Empty PDF tree");
        Part root = parts.get(0);
        var document = new NodePdfDocument(limits).prepare(root.title(), "", root.contentId(),
                options, source, PdfFilename.create(root.title(), root.occurrenceId()));
        Element body = (Element) document.getElementsByTagName("body").item(0);
        // The root title is added below with the same hierarchy rules as every other occurrence.
        var headings = body.getElementsByTagName("h1");
        if (headings.getLength() > 0) body.removeChild(headings.item(0));
        if (contents) {
            Element toc = element(document, "div", "");
            toc.setAttribute("class", "pdf-toc");
            Element tocTitle = element(document, "div", contentsTitle);
            tocTitle.setAttribute("style", "font-size:20pt;font-weight:bold;margin-bottom:12pt");
            toc.appendChild(tocTitle);
            for (Part part : parts) {
                Element line = element(document, "div", "");
                line.setAttribute("style", "margin-left:" + Math.min(part.depth(), 12) * 12 + "pt");
                Element link = element(document, "a", part.title());
                link.setAttribute("href", "#sc-node-" + part.occurrenceId());
                line.appendChild(link); toc.appendChild(line);
            }
            body.appendChild(toc);
        }
        var contentOptions = new PdfExportOptions(options.paper(), options.orientation(), options.colors(),
                false, options.outline(), false, false, false, false, false);
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            Element section = element(document, "div", "");
            if (i > 0 || contents) section.setAttribute("style", "page-break-before:always");
            Element title = element(document, "h1", options.nodeTitle() ? part.title() : "");
            title.setAttribute("data-pdf-bookmark-name", part.title() == null ? "" : part.title());
            title.setAttribute("id", "sc-node-" + part.occurrenceId());
            title.setAttribute("class", "node-title");
            title.setAttribute("style", options.nodeTitle() ? "page-break-inside:avoid" :
                    "font-size:0;height:0;margin:0;page-break-inside:avoid");
            title.setAttribute("data-pdf-bookmark", options.outline() ? Integer.toString(part.depth() + 1) : "exclude");
            section.appendChild(title);
            var nodeDocument = new NodePdfDocument(limits).prepare(part.title(), part.html(), part.contentId(), contentOptions, null);
            Element nodeBody = (Element) nodeDocument.getElementsByTagName("body").item(0);
            for (var child = nodeBody.getFirstChild(); child != null; child = child.getNextSibling())
                section.appendChild(document.importNode(child, true));
            var all = section.getElementsByTagName("*");
            for (int n = 0; n < all.getLength(); n++) {
                Element item = (Element) all.item(n);
                if (item == title) continue;
                if (item.hasAttribute("id")) item.setAttribute("id", "sc-content-" + part.occurrenceId() + "-" + item.getAttribute("id"));
                if (item.getAttribute("href").startsWith("#")) item.setAttribute("href",
                        "#sc-content-" + part.occurrenceId() + "-" + item.getAttribute("href").substring(1));
                if (item.hasAttribute("data-pdf-bookmark")) item.setAttribute("data-pdf-bookmark",
                        Integer.toString(Integer.parseInt(item.getAttribute("data-pdf-bookmark")) + part.depth() + 1));
            }
            body.appendChild(section);
        }
        return document;
    }

    private static Element element(org.w3c.dom.Document document, String tag, String text) {
        Element element = document.createElementNS("http://www.w3.org/1999/xhtml", tag);
        element.setTextContent(text == null ? "" : text);
        return element;
    }
}

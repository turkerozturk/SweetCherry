package com.turkerozturk.pdf;

import java.util.ArrayList;
import java.util.Set;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/** Mirrors the reader's fragment flow: inline edges, explicit internal line boundaries. */
final class PdfFragmentFlow {
    private static final Set<String> LINES = Set.of("div", "p", "h1", "h2", "h3", "h4", "h5", "h6", "pre");
    private static final Set<String> BLOCKS = Set.of("table", "thead", "tbody", "tfoot", "tr", "td", "th", "ul", "ol", "li", "blockquote", "hr");

    private PdfFragmentFlow() {}

    static void inline(Element parent) {
        var children = new ArrayList<>(parent.childNodes());
        boolean hasLines = children.stream().anyMatch(node -> node instanceof Element e && LINES.contains(e.normalName()));
        boolean occupied = false, previousLine = false;
        for (int index = 0; index < children.size(); index++) {
            Node child = children.get(index);
            if (child instanceof TextNode text) {
                if (hasLines && text.isBlank()) child.remove();
                else if (!text.getWholeText().isEmpty()) {
                    if (previousLine) child.before(new Element("br"));
                    occupied = true; previousLine = false;
                }
                continue;
            }
            if (!(child instanceof Element element)) continue;
            String tag = element.normalName();
            // A codebox is already a structured object, not a text paragraph.
            if (tag.equals("div") && element.attr("style").matches("(?is).*(?:^|;)\\s*overflow-x\\s*:\\s*auto\\s*(?:;|$).*")) {
                occupied = false; previousLine = false; continue;
            }
            if (!LINES.contains(tag)) {
                inline(element);
                if (tag.equals("br") || BLOCKS.contains(tag)) { occupied = false; previousLine = false; }
                else if (!element.text().isEmpty() || tag.equals("img")) {
                    if (previousLine) element.before(new Element("br"));
                    occupied = true; previousLine = false;
                }
                continue;
            }
            boolean onlyBreak = element.childNodeSize() == 1 && element.childNode(0) instanceof Element e && e.normalName().equals("br");
            if (onlyBreak && children.subList(index + 1, children.size()).stream().anyMatch(PdfFragmentFlow::meaningful))
                element.childNode(0).remove();
            inline(element);
            element.tagName("span").addClass("pdf-fragment-line");
            if (tag.equals("pre")) element.addClass("pdf-fragment-pre");
            if (tag.matches("h[1-6]")) element.addClass("pdf-fragment-" + tag);
            if (occupied) element.before(new Element("br"));
            occupied = true; previousLine = true;
        }
    }

    private static boolean meaningful(Node node) {
        return node instanceof Element || node instanceof TextNode text && !text.isBlank();
    }
}

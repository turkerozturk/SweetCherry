package com.turkerozturk.richtext.experimental;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.turkerozturk.richtext.experimental.RichTextLayout.*;

/** Experimental HTML composition. Missing or mismatched payloads fail visibly instead of dropping objects. */
public final class RichTextObjectHtmlRenderer {
    private final RichTextHtmlRenderer textRenderer = new RichTextHtmlRenderer();
    private final boolean pdfHeadings;
    public RichTextObjectHtmlRenderer() { this(false); }
    public RichTextObjectHtmlRenderer(boolean pdfHeadings) { this.pdfHeadings = pdfHeadings; }

    /** Requires a tenant-view token for download links; images are embedded PNG snapshots. */
    public String render(RichTextLayout layout, Map<EmbeddedObject, EmbeddedContent> contents, String tenantView) {
        var html = new StringBuilder("<div class=\"rich-text-preview\" style=\"overflow-wrap:anywhere\">");
        var paragraph = new StringBuilder();
        String alignment = "left";
        boolean afterBlock = false;
        for (var part : layout.parts()) {
            if (part instanceof TextPart text) {
                var run = text.run();
                String value = run.text();
                if (afterBlock && value.startsWith("\n")) value = value.substring(1);
                if (!value.isEmpty()) afterBlock = false;
                String align = switch (run.attributes().getOrDefault("justification", "")) {
                    case "right" -> "right"; case "center" -> "center"; case "fill" -> "justify"; default -> "left";
                };
                String[] lines = value.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    if (!lines[i].isEmpty()) {
                        alignment = align;
                        var fragment = new RichTextDocument.TextRun(lines[i], run.attributes(), 0);
                        paragraph.append(textRenderer.renderContent(new RichTextDocument(List.of(fragment)), tenantView, pdfHeadings));
                    }
                    if (i < lines.length - 1) {
                        appendParagraph(html, paragraph, alignment, true);
                        alignment = "left";
                    }
                }
            } else {
                var ref = ((ObjectPart) part).object();
                var payload = contents.get(ref);
                if (payload == null) throw new IllegalArgumentException("Missing object payload: " + ref);
                if (ref.kind() == ObjectKind.TABLE || ref.kind() == ObjectKind.CODEBOX) {
                    appendParagraph(html, paragraph, alignment, false);
                    html.append(renderObject(ref, payload, tenantView));
                    afterBlock = true;
                    alignment = "left";
                } else paragraph.append(renderObject(ref, payload, tenantView));
            }
        }
        appendParagraph(html, paragraph, alignment, false);
        return html.append("</div>").toString();
    }

    /** Formats a complete line once, so mixed inline styles do not break paragraph alignment. */
    private void appendParagraph(StringBuilder html, StringBuilder paragraph, String alignment, boolean keepEmpty) {
        if (paragraph.length() == 0 && !keepEmpty) return;
        html.append("<div style=\"white-space:pre-wrap;text-align:").append(alignment).append(";margin:0\">")
                .append(paragraph.length() == 0 ? "<br>" : paragraph).append("</div>");
        paragraph.setLength(0);
    }

    /** Applies kind-specific rendering; stored text, filenames and anchors are always HTML-escaped. */
    private String renderObject(EmbeddedObject ref, EmbeddedContent payload, String tenantView) {
        return switch (ref.kind()) {
            case IMAGE -> {
                if (!(payload instanceof EmbeddedContent.PngImage image)) throw mismatch(ref);
                byte[] bytes = image.bytes();
                if (!isPng(bytes)) throw new IllegalArgumentException("Inline image is not a PNG snapshot");
                yield "<img class=\"rich-text-image\" style=\"max-width:100%;height:auto\" alt=\"\" src=\"data:image/png;base64,"
                        + Base64.getEncoder().encodeToString(bytes) + "\">";
            }
            case ATTACHMENT -> {
                if (!(payload instanceof EmbeddedContent.Attachment attachment)) throw mismatch(ref);
                String label = "<span aria-hidden=\"true\">📎</span> " + escape(attachment.filename());
                if (tenantView == null || tenantView.isBlank()) yield "<span class=\"rich-text-attachment\">" + label + "</span>";
                String url = "/download/" + ref.nodeId() + "/" + ref.bufferOffset() + "?_tenantView="
                        + URLEncoder.encode(tenantView, StandardCharsets.UTF_8);
                yield "<a class=\"rich-text-attachment\" target=\"_blank\" rel=\"noopener noreferrer\" href=\"" + escape(url) + "\">" + label + "</a>";
            }
            case ANCHOR -> {
                if (!(payload instanceof EmbeddedContent.Anchor anchor)) throw mismatch(ref);
                yield "<span id=\"" + escape(anchor.name()) + "\" class=\"rich-text-anchor\" aria-label=\"Anchor\">⚓</span>";
            }
            case CODEBOX -> {
                if (!(payload instanceof EmbeddedContent.CodeBox box)) throw mismatch(ref);
                yield "<div style=\"max-width:100%;overflow-x:auto\">"
                        + com.turkerozturk.helpers.highlighter.CodeHighLighter.highlightLanguage(box.syntax(), box.text()) + "</div>";
            }
            case TABLE -> {
                if (!(payload instanceof EmbeddedContent.Table table)) throw mismatch(ref);
                yield renderTable(table);
            }
        };
    }

    /** Displays the final storage row as a header without changing the stored row order. */
    private String renderTable(EmbeddedContent.Table table) {
        var html = new StringBuilder("<div style=\"max-width:100%;overflow-x:auto\"><table>");
        var rows = table.rows();
        if (!rows.isEmpty()) {
            html.append("<thead>").append(row(rows.get(rows.size() - 1), "th")).append("</thead><tbody>");
            for (int i = 0; i < rows.size() - 1; i++) html.append(row(rows.get(i), "td"));
            html.append("</tbody>");
        }
        return html.append("</table></div>").toString();
    }

    private String row(List<String> cells, String tag) {
        var html = new StringBuilder("<tr>");
        for (var cell : cells) html.append('<').append(tag).append('>').append(escape(cell))
                .append("</").append(tag).append('>');
        return html.append("</tr>").toString();
    }

    private boolean isPng(byte[] bytes) {
        byte[] signature = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if (bytes[i] != signature[i]) return false;
        return true;
    }

    private IllegalArgumentException mismatch(EmbeddedObject ref) {
        return new IllegalArgumentException("Object kind and payload disagree: " + ref);
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

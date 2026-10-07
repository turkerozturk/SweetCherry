package com.turkerozturk.pdf;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.Element;
import org.xml.sax.InputSource;

/** Converts parser output to self-contained PDF XHTML; no browser scripts or external resources survive. */
public final class NodePdfDocument {
    private final PdfExportLimits limits;
    public NodePdfDocument() { this(new PdfExportLimits()); }
    public NodePdfDocument(PdfExportLimits limits) { this.limits = limits; }
    private static final String CSS = """
        @page { size: A4 portrait; margin: 18mm; }
        body { font-family: 'Liberation Sans'; font-size: 11pt; line-height: 1.35; color: #111; }
        h1.node-title { font-size: 20pt; margin: 0 0 12pt; }
        div { margin: 0; white-space: pre-wrap; } a { color: #3584e4; text-decoration: underline; }
        pre, code { font-family: 'Liberation Mono'; white-space: pre-wrap; font-size: 9pt; }
        pre { padding: 6pt; background: #f4f4f4; border: 1pt solid #ddd; }
        table { width: 100%; border-collapse: collapse; table-layout: fixed; -fs-table-paginate: paginate; }
        td, th { border: .5pt solid #aaa; padding: 4pt; vertical-align: top; }
        th { background: #eee; } thead { display: table-header-group; }
        img { vertical-align: middle; } .rich-text-anchor { font-size: 0; }
        """;

    public org.w3c.dom.Document prepare(String title, String html, long contentId) {
        return prepare(title, html, contentId, PdfExportOptions.defaults(), null);
    }

    public org.w3c.dom.Document prepare(String title, String html, long contentId,
            PdfExportOptions options, PdfSourceMetadata.Source source) {
        return prepare(title, html, contentId, options, source, PdfFilename.create(title, contentId));
    }

    public org.w3c.dom.Document prepare(String title, String html, long contentId,
            PdfExportOptions options, PdfSourceMetadata.Source source, String filename) {
        if (html.length() > limits.getMaxHtmlCharacters()) throw tooLarge();
        var page = Jsoup.parse("<html xmlns='http://www.w3.org/1999/xhtml'><head><meta charset='UTF-8'/></head><body></body></html>");
        page.head().appendElement("title").text(title == null ? "SweetCherry" : title);
        String css = CSS.replace("size: A4 portrait", "size: " + options.paper() + " " + options.orientation());
        if (!options.colors()) css = css.replace("#3584e4", "#111");
        if (options.filenameHeader()) css += "@page { @top-center { content: element(pdf-header); } } .pdf-header { position: running(pdf-header); font-size: 8pt; text-align: center; }";
        if (options.pageNumbers()) css += "@page { @bottom-center { content: counter(page) ' / ' counter(pages); font-family: 'Liberation Sans'; font-size: 8pt; } }";
        page.head().appendElement("style").text(fontFaces() + css);
        if (!options.outline()) page.selectFirst("html").attr("data-pdf-bookmark", "exclude");
        page.head().appendElement("meta").attr("name", "sc-pdf-metadata").attr("content", Boolean.toString(options.metadata()));
        if (options.metadata() && source != null) {
            if (options.sourceName() && source.name() != null) page.head().appendElement("meta").attr("name", "sc-source-name").attr("content", source.name());
            if (options.sourceAddress() && source.address() != null) page.head().appendElement("meta").attr("name", "sc-source-address").attr("content", source.address());
        }
        if (options.filenameHeader()) page.body().appendElement("div").addClass("pdf-header").text(filename);
        if (options.nodeTitle()) page.body().appendElement("h1").addClass("node-title").attr("data-pdf-bookmark", "1").text(title == null ? "" : title);
        var content = Jsoup.parseBodyFragment(html);
        content.select("script,style,link,iframe,object,embed,form,input,button").remove();
        for (var element : content.body().getAllElements()) {
            for (var attribute : new java.util.ArrayList<>(element.attributes().asList())) {
                if (!java.util.Set.of("style", "href", "src", "id", "class", "colspan", "rowspan", "alt", "data-pdf-heading").contains(attribute.getKey()))
                    element.removeAttr(attribute.getKey());
            }
            // Only parser-generated CSS is retained; never allow resource-fetching CSS.
            String style = element.attr("style");
            var safe = new StringBuilder();
            for (String declaration : style.split(";")) {
                String[] pair = declaration.split(":", 2);
                if (pair.length != 2) continue;
                String key = pair[0].trim().toLowerCase(java.util.Locale.ROOT);
                String value = pair[1].trim();
                if (java.util.Set.of("font-weight", "font-style", "text-decoration", "font-size", "vertical-align", "color", "background-color", "text-align").contains(key)
                        && value.matches("[#a-zA-Z0-9 .%_-]+")) safe.append(key).append(':').append(value).append(';');
                if ("font-family".equals(key)) safe.append("font-family:'Liberation Mono';");
            }
            String cleaned = safe.toString();
            if (!options.colors()) cleaned = cleaned.replaceAll("(?i)(^|;)(color):[^;]*", "$1$2:#111")
                    .replaceAll("(?i)(^|;)(background-color):[^;]*", "$1$2:#fff");
            element.attr("style", cleaned);
            String heading = element.attr("data-pdf-heading");
            if (options.outline() && heading.matches("h[1-6]")) {
                var line = element.closest("div");
                if (line != null && line.tagName().equals("div") && !line.hasAttr("data-pdf-bookmark")) {
                    line.attr("data-pdf-bookmark", Integer.toString(Integer.parseInt(heading.substring(1)) + (options.nodeTitle() ? 1 : 0)));
                    line.attr("style", line.attr("style") + "page-break-inside:avoid;");
                }
            }
            element.removeAttr("data-pdf-heading");
        }
        for (var icon : content.select(".rich-text-attachment span"))
            icon.replaceWith(new Element("img").attr("src", PdfAttachmentIcon.URI).attr("alt", "")
                    .attr("style", "width:12px;height:12px"));
        for (var anchor : content.select(".rich-text-anchor")) anchor.text("");
        for (var link : content.select("a[href]")) {
            String href = link.attr("href");
            try {
                var uri = URI.create(href);
                if (("/nodes/" + contentId).equals(uri.getPath()) && uri.getRawFragment() != null)
                    link.attr("href", "#" + uri.getRawFragment());
                else if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null)
                    link.removeAttr("href");
            } catch (IllegalArgumentException invalid) { link.removeAttr("href"); }
        }
        long totalImageBytes = 0;
        for (var image : content.select("img")) {
            String src = image.attr("src");
            if (!src.startsWith("data:image/png;base64,")) {
                image.replaceWith(new TextNode("[Image not embedded]"));
                continue;
            }
            byte[] bytes = Base64.getDecoder().decode(src.substring("data:image/png;base64,".length()));
            totalImageBytes += bytes.length;
            if (totalImageBytes > limits.getMaxImageBytes()) throw tooLarge();
            int[] size = imageSize(bytes);
            // Fix explicit dimensions before layout: max-width alone is insufficient in this renderer.
            double width = options.paper().equals("A4") ? 793.7 : 816;
            double height = options.paper().equals("A4") ? 1122.5 : 1056;
            if (options.orientation().equals("landscape")) { double swap = width; width = height; height = swap; }
            double scale = Math.min(1, Math.min((width - 144) / size[0], (height - 160) / size[1]));
            if (!options.colors()) {
                try {
                    var bitmap = ImageIO.read(new ByteArrayInputStream(bytes));
                    var gray = new java.awt.image.BufferedImage(size[0], size[1], java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < size[1]; y++) for (int x = 0; x < size[0]; x++) {
                        int pixel = bitmap.getRGB(x, y);
                        int value = (int)(.299 * ((pixel >> 16) & 255) + .587 * ((pixel >> 8) & 255) + .114 * (pixel & 255));
                        gray.setRGB(x, y, (pixel & 0xff000000) | (value << 16) | (value << 8) | value);
                    }
                    var output = new java.io.ByteArrayOutputStream();
                    ImageIO.write(gray, "png", output);
                    image.attr("src", "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray()));
                } catch (java.io.IOException error) { throw new IllegalArgumentException("PDF image conversion failed", error); }
            }
            image.attr("style", "width:" + Math.max(1, (int)(size[0] * scale)) + "px;height:"
                    + Math.max(1, (int)(size[1] * scale)) + "px;");
        }
        // Explicit breaks preserve stored newlines even where CSS pre-wrap differs from browsers.
        for (var text : new java.util.ArrayList<>(content.body().select("*").stream()
                .flatMap(e -> e.textNodes().stream()).toList())) {
            if (!text.getWholeText().contains("\n")) continue;
            String[] lines = text.getWholeText().split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) text.before(new Element("br"));
                text.before(new TextNode(lines[i]));
            }
            text.remove();
        }
        page.body().appendChildren(new java.util.ArrayList<>(content.body().childNodes()));
        page.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml).prettyPrint(false);
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(page.outerHtml())));
        } catch (Exception error) { throw new IllegalArgumentException("Invalid PDF XHTML", error); }
    }

    private String fontFaces() {
        var css = new StringBuilder();
        for (String family : new String[]{"Sans", "Mono"}) {
            for (String style : new String[]{"Regular", "Bold", "Italic", "BoldItalic"}) {
                css.append("@font-face { font-family:'Liberation ").append(family)
                        .append("'; src:url('sc-font:Liberation").append(family).append('-').append(style)
                        .append(".ttf'); font-weight:").append(style.contains("Bold") ? "bold" : "normal")
                        .append("; font-style:").append(style.contains("Italic") ? "italic" : "normal")
                        .append("; -fs-pdf-font-embed:embed; -fs-pdf-font-encoding:Identity-H; }");
            }
        }
        return css.toString();
    }

    private int[] imageSize(byte[] bytes) {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Unreadable PDF image");
            var reader = readers.next();
            try {
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long)width * height > limits.getMaxImagePixels()) throw tooLarge();
                return new int[]{width, height};
            } finally { reader.dispose(); }
        } catch (java.io.IOException error) { throw new IllegalArgumentException("Unreadable PDF image", error); }
    }
    private org.springframework.web.server.ResponseStatusException tooLarge() {
        return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
    }
}

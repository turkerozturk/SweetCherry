package com.turkerozturk.richtext.experimental;

import java.net.URI;
import java.util.Map;

/** Safe text/inline-format preview; widget layout and paragraph layout are not implemented yet. */
public final class RichTextHtmlRenderer {
    /** Escapes content and applies only explicitly supported formatting; raw attributes never become HTML. */
    public String render(RichTextDocument document) {
        return "<div class=\"rich-text-preview\" style=\"white-space:pre-wrap;overflow-wrap:anywhere\">"
                + renderContent(document) + "</div>";
    }

    /** Returns formatted inline text for composition with independently rendered object slots. */
    public String renderContent(RichTextDocument document) {
        return renderContent(document, null);
    }

    /** Resolves only HTTP(S), numeric node links and encoded anchor fragments. */
    public String renderContent(RichTextDocument document, String tenantView) {
        var html = new StringBuilder();
        for (var run : document.runs()) {
            var attributes = run.attributes();
            var css = new StringBuilder();
            if ("heavy".equals(attributes.get("weight"))) css.append("font-weight:bold;");
            if ("italic".equals(attributes.get("style"))) css.append("font-style:italic;");
            if ("monospace".equals(attributes.get("family"))) css.append("font-family:monospace;");
            boolean underline = "single".equals(attributes.get("underline"));
            boolean strike = "true".equals(attributes.get("strikethrough"));
            if (underline || strike) css.append("text-decoration:").append(underline ? "underline " : "")
                    .append(strike ? "line-through" : "").append(';');
            appendColor(css, attributes, "foreground", "color");
            appendColor(css, attributes, "background", "background-color");
            String size = switch (attributes.getOrDefault("scale", "")) {
                case "h1" -> "2em"; case "h2" -> "1.5em"; case "h3" -> "1.17em";
                case "h4" -> "1em"; case "h5" -> ".83em"; case "h6" -> ".67em";
                case "small", "sub", "sup" -> ".83em"; default -> "";
            };
            if (!size.isEmpty()) css.append("font-size:").append(size).append(';');
            if ("sub".equals(attributes.get("scale"))) css.append("vertical-align:sub;");
            if ("sup".equals(attributes.get("scale"))) css.append("vertical-align:super;");
            String href = linkHref(attributes.get("link"), tenantView);
            if (href != null) html.append("<a href=\"").append(escape(href))
                    .append("\" target=\"_blank\" rel=\"noopener noreferrer\">");
            if (href != null) css.append("color:#3584e4;");
            html.append("<span style=\"").append(css).append("\">").append(escape(run.text())).append("</span>");
            if (href != null) html.append("</a>");
        }
        return html.toString();
    }

    /** Converts 8-bit or 16-bit RGB components to CSS without accepting arbitrary CSS input. */
    private void appendColor(StringBuilder css, Map<String, String> attributes, String key, String property) {
        String color = attributes.getOrDefault(key, "");
        if (color.matches("#[0-9a-fA-F]{12}")) {
            color = "#" + color.substring(1, 3) + color.substring(5, 7) + color.substring(9, 11);
        }
        if (color.matches("#[0-9a-fA-F]{6}")) css.append(property).append(':').append(color).append(';');
    }

    private String linkHref(String link, String tenantView) {
        String external = externalHref(link);
        if (external != null) return external;
        if (link == null || !link.startsWith("node ") || tenantView == null) return null;
        String[] parts = link.split(" ", 3);
        if (parts.length < 2 || !parts[1].matches("[1-9][0-9]*")) return null;
        String url = "/nodes/" + parts[1] + "?_tenantView=" + encode(tenantView);
        return parts.length == 3 ? url + "#" + encode(parts[2]) : url;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Internal/file links remain in the model; previews activate only absolute HTTP(S) links. */
    private String externalHref(String link) {
        if (link == null || !link.startsWith("webs ")) return null;
        try {
            var uri = URI.create(link.substring(5));
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null ? uri.toString() : null;
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

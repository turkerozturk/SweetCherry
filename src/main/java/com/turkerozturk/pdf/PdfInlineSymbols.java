package com.turkerozturk.pdf;

import com.turkerozturk.symbols.InlineSymbols;
import java.util.*;
import org.w3c.dom.*;

/** Final, offline presentation pass. No HTML parsing, scripts, URLs or user-supplied font paths. */
final class PdfInlineSymbols {
    private PdfInlineSymbols() {}
    static void apply(Document document) {
        var bodies = document.getElementsByTagName("body"); if (bodies.getLength() == 0) return;
        var texts = new ArrayList<Text>(); collect(bodies.item(0), texts);
        // Editing may store one identical-format span per character. Merge only adjacent
        // text runs with exactly the same parent style; do not cross links or blocks.
        Text previous = null, first = null;
        for (Text text : texts) {
            if (!skip(text.getParentNode()) && previous != null && adjacent(previous, text)) {
                first.appendData(text.getData()); text.setData("");
            } else first = text;
            previous = text;
        }
        var headings=document.getElementsByTagName("*");
        for(int i=0;i<headings.getLength();i++) {
            Element item=(Element)headings.item(i);
            if(item.hasAttribute("data-pdf-bookmark")&&!item.hasAttribute("data-pdf-bookmark-name"))
                item.setAttribute("data-pdf-bookmark-name",InlineSymbols.plain(item.getTextContent()));
        }
        int count = 0;
        boolean monochrome = "true".equals(document.getDocumentElement().getAttribute("data-symbols-monochrome"));
        var images = new HashMap<String,String>();
        var fontFamilies = new LinkedHashSet<String>();
        for (Text text : texts) {
            if (skip(text.getParentNode())) continue;
            String value = text.getData(); var matches = InlineSymbols.matches(value); if (matches.isEmpty()) continue;
            int at = 0;
            for (var match : matches) {
                if (++count > 5000) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "Too many inline symbols");
                text.getParentNode().insertBefore(document.createTextNode(value.substring(at, match.start())), text);
                var icon = match.icon(); Element element;
                if (icon.kind().equals("ctb")) {
                    element = document.createElementNS("http://www.w3.org/1999/xhtml", "img");
                    element.setAttribute("src", images.computeIfAbsent(icon.name(), name -> image(name, monochrome)));
                    element.setAttribute("alt", "[" + icon.name() + "]");
                    element.setAttribute("style", "width:1em;height:1em;vertical-align:middle");
                } else {
                    fontFamilies.add(icon.kind());
                    element = document.createElementNS("http://www.w3.org/1999/xhtml", "span");
                    element.setAttribute("style", "font-family:'SC " + icon.kind() + "';font-weight:normal;font-style:normal");
                    element.setTextContent(icon.glyph());
                }
                element.setAttribute("class", "pdf-inline-symbol");
                text.getParentNode().insertBefore(element, text); at = match.end();
            }
            text.setData(value.substring(at));
        }
        var names = document.getElementsByTagName("*");
        for (int i=0; i<names.getLength(); i++) {
            Element item = (Element)names.item(i);
            if (item.hasAttribute("data-pdf-bookmark-name")) item.setAttribute("data-pdf-bookmark-name", InlineSymbols.plain(item.getAttribute("data-pdf-bookmark-name")));
        }
        if (fontFamilies.isEmpty()) return;
        Element style = document.createElementNS("http://www.w3.org/1999/xhtml", "style");
        var css = new StringBuilder();
        for (String family : fontFamilies) css.append("@font-face{font-family:'SC ").append(family)
                .append("';src:url('sc-symbol-font:").append(family).append(".ttf');font-weight:normal;font-style:normal;-fs-pdf-font-embed:embed;-fs-pdf-font-encoding:Identity-H;}");
        style.setTextContent(css.toString()); document.getElementsByTagName("head").item(0).appendChild(style);
    }
    private static boolean adjacent(Text a, Text b) {
        if (a.getParentNode()==b.getParentNode()) return a.getNextSibling()==b;
        if (!(a.getParentNode() instanceof Element x) || !(b.getParentNode() instanceof Element y)) return false;
        return "span".equals(x.getLocalName()) && "span".equals(y.getLocalName()) && x.getParentNode()==y.getParentNode()
                && x.getNextSibling()==y && x.getChildNodes().getLength()==1 && y.getChildNodes().getLength()==1
                && x.getAttributes().getLength()==y.getAttributes().getLength() && x.getAttributes().getLength()<=1
                && (x.getAttributes().getLength()==0 || (x.hasAttribute("style") && y.hasAttribute("style") && x.getAttribute("style").equals(y.getAttribute("style"))));
    }
    private static void collect(Node node, List<Text> result) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
            if (child instanceof Text text) result.add(text); else collect(child, result);
    }
    private static boolean skip(Node node) {
        boolean explicit = false;
        for (; node instanceof Element item; node = node.getParentNode()) {
            String tag = item.getLocalName() == null ? item.getTagName() : item.getLocalName();
            if (item.hasAttribute("data-inline-symbols")) explicit = true;
            if (item.hasAttribute("data-symbols-skip") || Set.of("pre", "code", "script", "style", "textarea").contains(tag)
                    || item.getAttribute("style").matches("(?is).*font-family[^;]*(monospace|Liberation Mono).*")) return true;
            if (tag.equals("table") && !explicit) return true;
        }
        return false;
    }
    private static String image(String name, boolean monochrome) {
        try (var input = PdfInlineSymbols.class.getResourceAsStream("/static/img/ctbicons/" + name + ".png")) {
            if (input == null) throw new IllegalStateException("Missing bundled icon");
            byte[] bytes = input.readAllBytes();
            if (monochrome) {
                var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                var gray = new java.awt.image.BufferedImage(image.getWidth(), image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                for (int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) {
                    int p=image.getRGB(x,y),v=(int)(.299*((p>>16)&255)+.587*((p>>8)&255)+.114*(p&255));
                    gray.setRGB(x,y,(p&0xff000000)|(v<<16)|(v<<8)|v);
                }
                var output = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(gray,"png",output); bytes=output.toByteArray();
            }
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (java.io.IOException error) { throw new IllegalStateException("Unreadable bundled icon", error); }
    }
}

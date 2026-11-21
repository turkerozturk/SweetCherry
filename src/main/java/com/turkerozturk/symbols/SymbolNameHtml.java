package com.turkerozturk.symbols;

import java.util.*;
import java.awt.*;
import org.jsoup.nodes.*;

/** Tiny embedded icons for node-name placeholders in scriptless, data-only report frames. */
public final class SymbolNameHtml {
    private SymbolNameHtml() {}
    private static final Map<String, String> CACHE = new LinkedHashMap<>();
    public static void apply(org.jsoup.nodes.Document document) {
        int count=0;
        for (Element name : document.select("[data-symbol-name]")) {
            for (TextNode text : new ArrayList<>(name.textNodes())) {
                String value=text.getWholeText(); int at=0;
                for (var match : InlineSymbols.matches(value)) {
                    if (++count>5000) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
                    text.before(new TextNode(value.substring(at,match.start())));
                    text.before(new Element("img").addClass("sc-name-symbol").attr("src",image(match.icon())).attr("alt","["+match.icon().name()+"]")
                            .attr("style","width:1em;height:1em;vertical-align:baseline").attr("width","16").attr("height","16"));
                    at=match.end();
                }
                text.text(value.substring(at));
            }
            name.removeAttr("data-symbol-name");
        }
    }
    private static synchronized String image(InlineSymbols.Icon icon) {
        String key=icon.kind()+":"+icon.name(); if(CACHE.containsKey(key))return CACHE.get(key);
        try {
            byte[] bytes;
            if(icon.kind().equals("ctb")) {
                try(var input=SymbolNameHtml.class.getResourceAsStream("/static/img/ctbicons/"+icon.name()+".png")){bytes=Objects.requireNonNull(input).readAllBytes();}
            } else {
                Font font;
                try(var input=SymbolNameHtml.class.getResourceAsStream("/symbols/fa-"+icon.kind()+".ttf")){font=Font.createFont(Font.TRUETYPE_FONT,Objects.requireNonNull(input)).deriveFont(48f);}
                var bitmap=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                var graphics=bitmap.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);graphics.setFont(font);graphics.setColor(new Color(0x20242a));
                    var glyph=font.createGlyphVector(graphics.getFontRenderContext(),icon.glyph());var bounds=glyph.getVisualBounds();
                    graphics.drawGlyphVector(glyph,(float)((64-bounds.getWidth())/2-bounds.getX()),(float)((64-bounds.getHeight())/2-bounds.getY()));
                } finally {graphics.dispose();}
                var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(bitmap,"png",output);bytes=output.toByteArray();
            }
            String value="data:image/png;base64,"+Base64.getEncoder().encodeToString(bytes);
            if(CACHE.size()>=256)CACHE.remove(CACHE.keySet().iterator().next());CACHE.put(key,value);return value;
        } catch(java.io.IOException|FontFormatException error){throw new IllegalStateException("Unreadable local symbol",error);}
    }
}

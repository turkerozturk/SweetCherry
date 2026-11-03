package com.turkerozturk.customlists.views;

import com.turkerozturk.pdf.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.stereotype.Service;

/** Offline print adapter. Browser-only layout rules are deliberately not passed to the PDF engine. */
@Service
public class RecordViewPdf {
    private final PdfExportLimits limits;
    private final NodePdfRenderer renderer;
    public RecordViewPdf(PdfExportLimits limits, NodePdfRenderer renderer) { this.limits=limits; this.renderer=renderer; }
    private static final Set<String> PROPERTIES=Set.of("color","background-color","font-size","font-weight","font-style","text-decoration","text-align","vertical-align","line-height","width","height","max-width","min-width","margin","margin-top","margin-right","margin-bottom","margin-left","padding","padding-top","padding-right","padding-bottom","padding-left","border","border-top","border-right","border-bottom","border-left","border-collapse","border-spacing","table-layout","display","white-space","page-break-before","page-break-after","page-break-inside");
    static String declarations(String source) {
        var result=new StringBuilder();
        for(String declaration:source.split(";")) {
            String[] pair=declaration.split(":",2);if(pair.length!=2)continue;
            String key=pair[0].trim().toLowerCase(Locale.ROOT),value=pair[1].trim();
            // No escapes, functions, URLs, at-rules, generated content, positioning or custom fonts.
            if("display".equals(key)&&!Set.of("block","inline","inline-block","table","table-row","table-cell","table-header-group","table-footer-group","table-row-group","list-item","none").contains(value))continue;
            if(PROPERTIES.contains(key)&&value.matches("[#a-zA-Z0-9 .%,_-]{1,160}")&&!value.toLowerCase(Locale.ROOT).contains("expression"))result.append(key).append(':').append(value).append(';');
        }
        return result.toString();
    }
    static String stylesheet(String source) {
        source=source.replaceAll("(?s)/\\*.*?\\*/","");
        var result=new StringBuilder();int start=0;
        while(start<source.length()) {
            int open=source.indexOf('{',start);if(open<0)break;
            int close=source.indexOf('}',open+1);if(close<0)break;
            String selector=source.substring(start,open).trim();
            String block=source.substring(open+1,close);start=close+1;
            if(selector.matches("[a-zA-Z0-9_#., >+~*:-]{1,256}")&&!block.contains("{"))result.append(selector).append('{').append(declarations(block)).append('}');
        }
        return result.toString();
    }
    public byte[] export(RecordViewService.Page page) { return renderer.render(prepare(page.output().document(),page.record().getName(),page.record().getNodeId())); }
    org.w3c.dom.Document prepare(String html,String title,long recordId) {
        if(html.length()>limits.getMaxHtmlCharacters())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
        var input=Jsoup.parse(html);var css=new StringBuilder();
        for(var style:input.select("style"))css.append(stylesheet(style.data().isEmpty()?style.html():style.data()));
        input.select("style,script,link,meta,iframe,object,embed,form,input,button").remove();
        for(var element:input.body().getAllElements())element.attr("style",declarations(element.attr("style")));
        // Reuse existing bounded image validation, explicit fitting and JPEG-to-PNG conversion below.
        long totalImageBytes=0;
        for(var image:input.select("img")) {
            String src=image.attr("src");
            if(!src.matches("data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+")) { image.replaceWith(new org.jsoup.nodes.TextNode("[Image not embedded]")); continue; }
            totalImageBytes+=Base64.getDecoder().decode(src.substring(src.indexOf(',')+1)).length;
            if(totalImageBytes>limits.getMaxImageBytes())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
            if(image.attr("src").startsWith("data:image/jpeg;base64,")) {
                String png=png(image.attr("src"),limits);
                image.attr("src",png);
            }
        }
        var options=new PdfExportOptions("A4","portrait",true,false,false,false,true,false,false,false);
        String images=input.select("img").outerHtml();
        var normalizer=new NodePdfDocument(limits);
        var normalized=normalizer.prepare(title,images,recordId,options,null);
        var normalizedImages=normalized.getElementsByTagName("img");int index=0;
        for(var image:input.select("img")) {
            // NodePdfDocument rejects malformed embedded PNGs and enforces aggregate image limits.
            var safe=(org.w3c.dom.Element)normalizedImages.item(index++);
            image.attr("src",safe.getAttribute("src")).attr("style",safe.getAttribute("style"));
        }
        for(var link:input.select("a[href]")) {
            String href=link.attr("href");
            // A downloaded standalone PDF has no authenticated web origin for relative node links.
            if(!href.matches("(?i)https?://.*"))link.removeAttr("href");
        }
        var document=normalizer.prepare(title,"",recordId,options,null);
        var style=document.createElementNS("http://www.w3.org/1999/xhtml","style");
        style.setTextContent("div{white-space:normal}"+css+"\nbody{font-family:'Liberation Sans';margin:0}pre,code{font-family:'Liberation Mono';white-space:pre-wrap;word-wrap:break-word}p,td,th,div{word-wrap:break-word}table{max-width:100%;-fs-table-paginate:paginate}thead{display:table-header-group}img{max-width:100%}");
        document.getElementsByTagName("head").item(0).appendChild(style);
        var body=document.getElementsByTagName("body").item(0);
        var source=new W3CDom().fromJsoup(input).getElementsByTagName("body").item(0);
        for(var child=source.getFirstChild();child!=null;child=child.getNextSibling())body.appendChild(document.importNode(child,true));
        return document;
    }
    private static String png(String uri,PdfExportLimits limits) {
        try {
            byte[] bytes=Base64.getDecoder().decode(uri.substring("data:image/jpeg;base64,".length()));
            if(bytes.length>limits.getMaxImageBytes())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
            try(var stream=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
                var readers=javax.imageio.ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new IllegalArgumentException("Unreadable PDF image");
                var reader=readers.next();try {
                    reader.setInput(stream);long pixels=(long)reader.getWidth(0)*reader.getHeight(0);
                    if(pixels>limits.getMaxImagePixels())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
                    var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(reader.read(0),"png",output);
                    return "data:image/png;base64,"+Base64.getEncoder().encodeToString(output.toByteArray());
                } finally { reader.dispose(); }
            }
        } catch(java.io.IOException|IllegalArgumentException error) {throw new IllegalArgumentException("Unreadable PDF image",error);}
    }
}

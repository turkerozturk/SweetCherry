package com.turkerozturk.markdown;

import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** Small explicit registry: future node markup renderers can be added without changing CTB storage. */
public final class NodeMarkup {
    private static final Map<String,NodeMarkupRenderer> RENDERERS=Map.of("markdown",new MarkdownRenderer());
    private NodeMarkup(){}
    public static boolean supports(String syntax){return syntax!=null&&RENDERERS.containsKey(syntax);}
    public static String render(String syntax,String source){return render(syntax,source,"");}
    public static String render(String syntax,String source,String tenantToken){
        var renderer=RENDERERS.get(syntax);if(renderer==null)throw new IllegalArgumentException("Unsupported node markup");
        var fragment=Jsoup.parseBodyFragment(renderer.render(source));fragment.outputSettings().prettyPrint(false);
        for(var link:fragment.select("a[href]")){
            String href=link.attr("href");
            if(href.matches("/nodes/[1-9][0-9]*(?:#[^?]*)?")&&tenantToken!=null&&!tenantToken.isEmpty()){
                int hash=href.indexOf('#');String path=hash<0?href:href.substring(0,hash),anchor=hash<0?"":href.substring(hash);
                link.attr("href",path+"?_tenantView="+java.net.URLEncoder.encode(tenantToken,java.nio.charset.StandardCharsets.UTF_8)+anchor);
            }
            if(!href.startsWith("#"))link.attr("target","_blank").attr("rel","noopener noreferrer");
        }
        return fragment.body().html();
    }
    public static String forPdf(String syntax,String source){
        var fragment=Jsoup.parseBodyFragment(render(syntax,source));fragment.outputSettings().prettyPrint(false);
        // Use the existing PDF heading/bookmark protocol; no new renderer or resource-fetch path.
        for(var heading:fragment.select("h1,h2,h3,h4,h5,h6")){
            Element line=new Element("div");heading.before(line);line.appendChild(heading);heading.attr("data-pdf-heading",heading.tagName());
        }
        for(var cell:fragment.select("td[align],th[align]"))cell.attr("style","text-align:"+cell.attr("align"));
        for(var checkbox:fragment.select("input[type=checkbox]"))checkbox.replaceWith(new org.jsoup.nodes.TextNode(checkbox.hasAttr("checked")?"☑ ":"☐ "));
        return fragment.body().html();
    }
    public static String text(String syntax,String source){
        var fragment=Jsoup.parseBodyFragment(render(syntax,source));
        for(var link:fragment.select("a[href]"))link.appendText(" ("+link.attr("href")+")");
        for(var image:fragment.select("img"))image.replaceWith(new org.jsoup.nodes.TextNode("[Image: "+image.attr("alt")+" ("+image.attr("src")+")]"));
        for(var checkbox:fragment.select("input[type=checkbox]"))checkbox.replaceWith(new org.jsoup.nodes.TextNode(checkbox.hasAttr("checked")?"[x]":"[ ]"));
        for(var cell:fragment.select("td,th"))cell.appendText("\t");
        for(var block:fragment.select("p,div,pre,li,h1,h2,h3,h4,h5,h6,tr,blockquote"))block.appendText("\n");
        for(var br:fragment.select("br"))br.replaceWith(new org.jsoup.nodes.TextNode("\n"));
        return fragment.body().wholeText().replace('\u00a0',' ').strip();
    }
}

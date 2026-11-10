package com.turkerozturk.html;

import com.turkerozturk.markdown.NodeMarkupRenderer;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Parses HTML as data using jsoup. Never fetches resources or modifies the stored source. */
public final class HtmlNodeRenderer implements NodeMarkupRenderer {
    public static final int MAX_CHARACTERS=2_000_000;
    public static final String CSP="default-src 'none'; img-src data:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'";
    private static final Safelist ALLOWED=new Safelist().addTags(
        "article","section","header","footer","main","div","span","p","br","hr","h1","h2","h3","h4","h5","h6",
        "b","strong","i","em","u","s","del","sup","sub","small","mark","blockquote","pre","code","ul","ol","li",
        "dl","dt","dd","table","thead","tbody","tfoot","tr","th","td","caption","a","img")
        .addAttributes(":all","id","style","title","dir","lang")
        .addAttributes("a","href").addAttributes("img","src","alt","width","height")
        .addAttributes("td","colspan","rowspan").addAttributes("th","colspan","rowspan","scope")
        .addAttributes("ol","start");
    private static final Set<String> CSS=Set.of("color","background-color","font-size","font-weight","font-style","text-decoration","text-align","vertical-align","line-height","margin","margin-top","margin-right","margin-bottom","margin-left","padding","padding-top","padding-right","padding-bottom","padding-left","border","border-top","border-right","border-bottom","border-left","border-collapse","border-spacing","width","height","max-width","min-width","white-space");
    public String syntax(){return "html";}
    public String render(String source) {
        String text=Objects.requireNonNullElse(source,"");
        if(text.length()>MAX_CHARACTERS)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"HTML source is too large");
        var input=Jsoup.parse(text);
        input.select("script,style,template,noscript,iframe,object,embed,link,meta,base,form,input,button,textarea,select,svg,math").remove();
        var clean=new Cleaner(ALLOWED).clean(input);clean.outputSettings().prettyPrint(false);
        var ids=new HashMap<String,String>();
        int index=0;
        for(var element:clean.select("[id]")) {
            String old=element.id(),id="html-"+index++;ids.putIfAbsent(old,id);element.attr("id",id);
        }
        for(var element:clean.select("[style]")) {
            String style=styles(element.attr("style"));if(style.isEmpty())element.removeAttr("style");else element.attr("style",style);
        }
        for(var element:clean.select("[width],[height],[colspan],[rowspan],[start]"))
            for(String attr:List.of("width","height","colspan","rowspan","start"))
                if(element.hasAttr(attr)&&!element.attr(attr).matches("[0-9]{1,4}"))element.removeAttr(attr);
        for(var link:clean.select("a[href]")) {
            String href=link.attr("href");
            if(href.startsWith("#")) {
                String target=ids.get(href.substring(1));if(target==null)link.removeAttr("href");else link.attr("href","#"+target);
            } else if(!safeLink(href))link.removeAttr("href");
        }
        for(var image:clean.select("img[src]"))if(!image.attr("src").matches("data:image/(png|jpeg|gif|webp);base64,[A-Za-z0-9+/]+={0,2}"))image.removeAttr("src");
        return new Element("section").addClass("node-markup").html(clean.body().html()).outerHtml();
    }
    private static String styles(String source){
        var result=new StringBuilder();
        for(String declaration:source.split(";")) {
            String[] pair=declaration.split(":",2);if(pair.length!=2)continue;
            String key=pair[0].trim().toLowerCase(Locale.ROOT),value=pair[1].trim();
            // Bounded literal values only: no url(), expression(), escapes, positioning, custom properties or CSS imports.
            if(CSS.contains(key)&&value.matches("[#a-zA-Z0-9 .%,_-]{1,160}")&&!value.toLowerCase(Locale.ROOT).contains("expression"))result.append(key).append(':').append(value).append(';');
        }
        return result.toString();
    }
    private static boolean safeLink(String value){
        if(value.chars().anyMatch(c->c<=32||c==127)||value.indexOf('\\')>=0)return false;
        try {
            URI uri=URI.create(value);String scheme=uri.getScheme();
            if("http".equalsIgnoreCase(scheme)||"https".equalsIgnoreCase(scheme))return uri.getHost()!=null&&uri.getUserInfo()==null;
            if("mailto".equalsIgnoreCase(scheme))return true;
            return scheme==null&&uri.getRawAuthority()==null&&value.matches("/nodes/[1-9][0-9]*");
        }catch(IllegalArgumentException invalid){return false;}
    }
    /** Only accepts our sanitized fragment; never accepts raw CTB content. */
    public static String document(String fragment){
        var doc=Jsoup.parseBodyFragment(fragment);doc.outputSettings().prettyPrint(false);
        doc.head().empty();doc.head().appendElement("meta").attr("http-equiv","Content-Security-Policy").attr("content",CSP);
        doc.head().appendElement("meta").attr("charset","UTF-8");
        doc.head().appendElement("meta").attr("name","viewport").attr("content","width=device-width,initial-scale=1");
        doc.head().appendElement("style").text("body{font-family:system-ui,sans-serif;margin:12px;overflow-wrap:anywhere}img{max-width:100%;height:auto}pre{white-space:pre-wrap}table{border-collapse:collapse;max-width:100%}th,td{border:1px solid #aaa;padding:6px}h1,h2,h3,p{margin-top:0}");
        return "<!doctype html>"+doc.outerHtml();
    }
    public static String frame(String document){
        return new Element("iframe").addClass("html-node-frame").attr("title","HTML")
            .attr("sandbox","allow-popups allow-popups-to-escape-sandbox").attr("referrerpolicy","no-referrer")
            .attr("style","display:block;width:100%;height:60vh;min-height:18rem;border:1px solid #aaa;border-radius:.3rem;background:white")
            .attr("srcdoc",document).outerHtml();
    }
}

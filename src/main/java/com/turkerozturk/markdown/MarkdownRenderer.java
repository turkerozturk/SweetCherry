package com.turkerozturk.markdown;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.net.URI;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/** Stateless reusable parser, followed by an independent HTML/URL allowlist. Never fetches resources. */
public final class MarkdownRenderer implements NodeMarkupRenderer {
    public static final int MAX_CHARACTERS=2_000_000;
    private final Parser parser;private final HtmlRenderer renderer;
    public MarkdownRenderer(){
        var options=new MutableDataSet().set(Parser.EXTENSIONS,List.of(
            com.vladsch.flexmark.ext.tables.TablesExtension.create(),
            com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension.create(),
            com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension.create()))
            .set(HtmlRenderer.ESCAPE_HTML,true).set(HtmlRenderer.GENERATE_HEADER_ID,true).set(HtmlRenderer.RENDER_HEADER_ID,true);
        parser=Parser.builder(options).build();renderer=HtmlRenderer.builder(options).build();
    }
    public String syntax(){return "markdown";}
    public String render(String source){
        String text=Objects.requireNonNullElse(source,"");
        if(text.length()>MAX_CHARACTERS)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE,"Markdown source is too large");
        var allowed=Safelist.relaxed().addTags("del","s","input").addAttributes(":all","class","id")
                .addAttributes("input","type","checked","disabled").addAttributes("th","align").addAttributes("td","align")
                .addProtocols("img","src","data").preserveRelativeLinks(true);
        var clean=new org.jsoup.safety.Cleaner(allowed).clean(Jsoup.parseBodyFragment(renderer.render(parser.parse(text)),"https://sweetcherry.invalid/"));
        for(var image:clean.select("img[src]"))if(!safeUrl(image.attr("src"),true))image.removeAttr("src");
        for(var link:clean.select("a[href]"))if(!safeUrl(link.attr("href"),false))link.removeAttr("href");
        for(var input:clean.select("input")){if(!"checkbox".equals(input.attr("type")))input.remove();else input.attr("disabled","");}
        for(var cell:clean.select("td[align],th[align]"))if(!Set.of("left","right","center").contains(cell.attr("align")))cell.removeAttr("align");
        // Keep generated IDs away from application controls, preserving local heading links.
        var ids=new java.util.HashMap<String,String>();
        for(var element:clean.select("[id]")){String old=element.id();String id="md-"+old;ids.put(old,id);element.attr("id",id);}
        for(var link:clean.select("a[href^=#]")){String target=link.attr("href").substring(1);if(ids.containsKey(target))link.attr("href","#"+ids.get(target));}
        clean.outputSettings().prettyPrint(false);
        return new org.jsoup.nodes.Element("section").addClass("node-markup").html(clean.body().html()).outerHtml();
    }
    private static boolean safeUrl(String value,boolean image){
        if(value.chars().anyMatch(c->c<=32||c==127)||value.indexOf('\\')>=0)return false;
        if(image && value.startsWith("data:"))return value.matches("data:image/png;base64,[A-Za-z0-9+/]*={0,2}");
        try{URI uri=URI.create(value);String scheme=uri.getScheme();if(scheme==null)return uri.getRawAuthority()==null&&!value.startsWith("//");
            return (scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https"))?uri.getHost()!=null:!image&&scheme.equalsIgnoreCase("mailto");
        }catch(IllegalArgumentException invalid){return false;}
    }
}

package com.turkerozturk.richtext.editing;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeService;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A live, inert reader for explicit read-only URL notes. No note/database writes. */
@Service
public class WebBookmarkService {
    private final NodeService nodes;
    private final WebContentImportService importer;
    public WebBookmarkService(NodeService nodes,WebContentImportService importer){this.nodes=nodes;this.importer=importer;}
    public record Preview(String html) {}
    public static String destination(Node node){
        if(node==null||!node.isReadOnly()||node.getName()==null||!(node.getName().startsWith("http://")||node.getName().startsWith("https://"))
                ||node.getHasImage()||node.getHasTable()||node.getHasCodeBox()||!node.isContentEmpty())return null;
        if(Set.of("__sweetcherry_audit_v1__","__sweetcherry_reminders_v1__","__sweetcherry_trash_v1__").contains(java.util.Objects.requireNonNullElse(node.getTags(),"")))return null;
        try{PublicWebFetcher.allowedUri(node.getName());return node.getName();}catch(PublicWebFetcher.Problem invalid){return null;}
    }
    public Preview load(long occurrence){
        if(occurrence<=0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        String url=destination(nodes.findDisplayNode(occurrence));
        if(url==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return new Preview(document(importer.load(url)));
    }
    static String document(WebContentImportService.Result result){
        var body=Jsoup.parseBodyFragment(result.html());body.outputSettings().prettyPrint(false);
        body.select("script,style,link,meta,base,iframe,object,embed,svg,math,form,input,button,textarea,select,video,audio,source,template").remove();
        for(var image:body.select("img")){
            String source=result.images().get(image.attr("data-import-image"));
            if(source!=null&&source.length()<=14_000_000&&source.matches("data:image/png;base64,[A-Za-z0-9+/]+={0,2}"))image.attr("src",source);
            else image.replaceWith(new TextNode(image.attr("alt")));
        }
        for(var link:body.select("a"))link.unwrap();
        var safe=new Safelist().addTags("p","br","div","span","b","strong","i","em","u","s","del","sub","sup","pre","code","blockquote","ul","ol","li","h1","h2","h3","h4","h5","h6","table","thead","tbody","tfoot","tr","td","th","hr","img")
                .addAttributes("img","src","alt").addProtocols("img","src","data");
        String clean=Jsoup.clean(body.body().html(),"",safe,new org.jsoup.nodes.Document.OutputSettings().prettyPrint(false));
        if(clean.length()>16_000_000)throw new PublicWebFetcher.Problem("large");
        // The iframe also has an empty sandbox attribute in the trusted parent document.
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"referrer\" content=\"no-referrer\">"
                +"<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; img-src data:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'\">"
                +"<style>body{font:16px/1.5 sans-serif;margin:1rem;color:#111;background:white;overflow-wrap:anywhere}img{max-width:100%;height:auto}pre{white-space:pre-wrap}table{border-collapse:collapse;max-width:100%}td,th{border:1px solid #ccc;padding:.3rem}</style>"
                +"</head><body>"+clean+"</body></html>";
    }
}

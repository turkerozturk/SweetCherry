package com.turkerozturk.richtext.editing;

import java.util.*;
import java.util.concurrent.Semaphore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import static com.turkerozturk.richtext.editing.PublicWebFetcher.*;

/** Fetches a bounded, public, static page into an inert representation. Never writes CTB data. */
@Service
public class WebContentImportService {
    public record Result(String title,String sourceUrl,String html,Map<String,String> images,int skippedImages) { }
    private final PublicWebFetcher fetcher;
    private final EmbeddedUploadPolicy uploads;
    private final Semaphore slots=new Semaphore(2);
    public WebContentImportService(PublicWebFetcher fetcher,EmbeddedUploadPolicy uploads) {this.fetcher=fetcher;this.uploads=uploads;}
    public Result load(String url) {
        if(!slots.tryAcquire())throw new Problem("busy");
        try {return importPage(url);} catch(Problem error){throw error;}
        catch(java.io.IOException|IllegalArgumentException error){throw new Problem("network");}
        finally {slots.release();}
    }
    private Result importPage(String url) throws java.io.IOException {
        long deadline=System.nanoTime()+30_000_000_000L;
        var budget=new Budget();
        var page=fetcher.fetch(url,2_000_000,deadline,budget);
        if(!Set.of("text/html","application/xhtml+xml").contains(page.type()))throw new Problem("type");
        var doc=Jsoup.parse(new java.io.ByteArrayInputStream(page.bytes()),page.charset(),page.uri().toString());
        // Ignore a remote <base>: references belong to the page actually fetched.
        doc.select("base").remove();doc.setBaseUri(page.uri().toString());
        Element content=doc.selectFirst("article");if(content==null)content=doc.selectFirst("main, [role=main]");if(content==null)content=doc.body();
        content.select("script,style,noscript,iframe,object,embed,svg,math,template,form,nav,header,footer,aside,[hidden],[aria-hidden=true]").remove();
        int skipped=0,total=0,occurrences=0;var images=new LinkedHashMap<String,String>();var cached=new HashMap<String,String>();
        for(var image:content.select("img")) {
            image.removeAttr("data-import-image");
            String source=image.attr("src");if(source.isBlank())source=image.attr("data-src");
            if(source.isBlank())source=image.attr("data-original");
            String absolute="";
            try {
                if(occurrences>=8 || source.isBlank())throw new Problem("large");
                absolute=page.uri().resolve(source).toString();
                String key=cached.get(absolute);
                if(key==null) {
                    if(images.size()>=8 || total>=10_000_000)throw new Problem("large");
                    int limit=Math.min(uploads.fileBytes(),10_000_000-total);
                    var resource=fetcher.fetch(absolute,limit,deadline,budget);
                    if(!Set.of("image/png","image/jpeg").contains(resource.type()))throw new Problem("type");
                    String raw="data:"+resource.type()+";base64,"+Base64.getEncoder().encodeToString(resource.bytes());
                    var json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("new-image:web",raw));
                    byte[] png=PendingRichTextImages.decode(json,limit).get("new-image:web");total+=png.length;
                    key="image"+images.size();images.put(key,"data:image/png;base64,"+Base64.getEncoder().encodeToString(png));cached.put(absolute,key);
                }
                occurrences++;image.attr("data-import-image",key);
            } catch(java.io.IOException|IllegalArgumentException|Problem error) {skipped++;}
            image.removeAttr("src");image.removeAttr("srcset");image.removeAttr("data-src");image.removeAttr("data-original");
            if(!image.hasAttr("data-import-image")) {
                // No remote image is left for the browser to load implicitly.
                image.replaceWith(new org.jsoup.nodes.TextNode(image.attr("alt").isBlank()?"[image]":image.attr("alt")));
            }
        }
        for(var link:content.select("a[href]")) {
            try {link.attr("href",allowedUri(page.uri().resolve(link.attr("href")).toString()).toString());}
            catch(Problem|IllegalArgumentException error){link.removeAttr("href");}
        }
        var safe=Safelist.relaxed().addAttributes(":all","style","align").addAttributes("img","data-import-image");
        safe.removeAttributes("img","src","width","height");
        String html=Jsoup.clean(content.html(),page.uri().toString(),safe,new org.jsoup.nodes.Document.OutputSettings().prettyPrint(false));
        if(html.length()>2_000_000)throw new Problem("large");
        return new Result(doc.title().substring(0,Math.min(200,doc.title().length())),page.uri().toString(),html,Map.copyOf(images),skipped);
    }
}

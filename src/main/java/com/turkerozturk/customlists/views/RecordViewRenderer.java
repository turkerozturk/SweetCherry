package com.turkerozturk.customlists.views;

import com.turkerozturk.node.*;
import com.turkerozturk.node.Node;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.net.URI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.safety.*;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

/** A bounded text-node placeholder renderer, deliberately not a script or expression engine. */
@Component
public class RecordViewRenderer {
    public static final String CSP="default-src 'none'; img-src data:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'";
    private static final Pattern TOKEN=Pattern.compile("\\{\\{([^{}]{1,256})}}");
    private static final Safelist ALLOWED=new Safelist().addTags("article","section","header","footer","main","div","span","p","br","hr","h1","h2","h3","h4","h5","h6","b","strong","i","em","u","s","del","sup","sub","small","mark","blockquote","pre","code","ul","ol","li","dl","dt","dd","table","thead","tbody","tfoot","tr","th","td","caption","colgroup","col","a","img","style")
        .addAttributes(":all","class","id","style","title","dir","lang")
        .addAttributes("a","href").addAttributes("img","src","alt","width","height")
        .addAttributes("td","colspan","rowspan").addAttributes("th","colspan","rowspan","scope");
    private final RecordViewSettings settings;
    private final MessageSource messages;
    public record Output(String document,List<String> warnings){}
    public RecordViewRenderer(RecordViewSettings settings,MessageSource messages){this.settings=settings;this.messages=messages;}
    public Output render(String source,Node template,String viewName,ProjectNode record,String tenantView,Locale locale,Instant now){
        if(source==null)source="";
        if(source.length()>settings.getMaxDesignCharacters())throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
        var warnings=new LinkedHashSet<String>();
        var issues=new TemplateNode(template).getDefinitionIssues();
        if(!issues.isEmpty())warnings.add(msg("report.definitions",locale)+": "+String.join(", ",issues));
        var input=Jsoup.parseBodyFragment(source);input.outputSettings().prettyPrint(false);
        if(!new Cleaner(ALLOWED).isValid(input))warnings.add(msg("recordviews.sanitized",locale));
        var doc=new Cleaner(ALLOWED).clean(input);doc.outputSettings().prettyPrint(false);
        var textNodes=new ArrayList<TextNode>();
        for(Element element:doc.body().getAllElements())if(!element.tagName().equals("style"))textNodes.addAll(element.textNodes());
        var fields=new LinkedHashMap<String,TemplateField>();for(var field:record.getFields())fields.put(field.key(),field);
        long length=doc.outerHtml().length();
        for(TextNode text:textNodes){
            var matcher=TOKEN.matcher(text.getWholeText());int start=0;boolean found=false;
            while(matcher.find()) {
                found=true;text.before(new TextNode(text.getWholeText().substring(start,matcher.start())));
                String token=matcher.group(1).trim();String value=switch(token){
                    case "record.name"->record.getName();case "record.id"->Long.toString(record.getNodeId());case "record.breadcrumbs"->Objects.toString(record.getBreadcrumbsAsString(),"");
                    case "template.name"->template.getName();case "template.id"->Long.toString(template.getNodeId());case "view.name"->viewName;case "now"->settings.now(now,locale);default->null;
                };
                List<org.jsoup.nodes.Node> replacement;
                if(value!=null)replacement=List.of(new TextNode(value));
                else if(token.startsWith("field.")&&fields.containsKey(token.substring(6))){
                    var field=fields.get(token.substring(6));
                    if(field.status().equals("value"))replacement=Jsoup.parseBodyFragment(field.data().getData()).body().childNodesCopy();
                    else {
                        var state=new Element("div").addClass("record-field-state").text(msg("report.state."+field.status(),locale));
                        for(var match:field.matches())state.appendText(" ").appendElement("a").attr("href","/nodes/"+match.getNodeId()+"?_tenantView="+java.net.URLEncoder.encode(Objects.toString(tenantView,""),java.nio.charset.StandardCharsets.UTF_8)).text("["+match.getNodeId()+"]");
                        warnings.add(field.key()+": "+msg("report.state."+field.status(),locale));replacement=List.of(state);
                    }
                } else {
                    String unknown=msg("recordviews.unknown",locale)+": "+token;warnings.add(unknown);
                    replacement=List.of(new Element("span").addClass("record-field-state").text(unknown));
                }
                // Inserted note content is never rescanned as a template.
                for(var node:replacement){length+=node.outerHtml().length();if(length>settings.getMaxRenderedCharacters())throw new ResponseStatusException(PAYLOAD_TOO_LARGE);text.before(node);}
                start=matcher.end();
            }
            if(found){text.before(new TextNode(text.getWholeText().substring(start)));text.remove();}
        }
        doc=new Cleaner(ALLOWED).clean(doc);doc.outputSettings().prettyPrint(false);
        for(Element element:doc.body().getAllElements())for(var attribute:new ArrayList<>(element.attributes().asList()))if(attribute.getValue().contains("{{")){element.removeAttr(attribute.getKey());warnings.add(msg("recordviews.attributes",locale));}
        for(var link:doc.select("a[href]")) {
            if(!safeLink(link.attr("href")))link.removeAttr("href");
            else link.attr("target","_blank").attr("rel","noopener noreferrer");
        }
        for(var image:doc.select("img[src]"))if(!image.attr("src").matches("data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+")) {image.replaceWith(new TextNode("["+msg("recordviews.imageBlocked",locale)+"]"));}
        if(doc.body().text().isBlank() && doc.select("img,table,hr").isEmpty())warnings.add(msg("recordviews.emptyDesign",locale));
        doc.head().empty();doc.head().appendElement("meta").attr("charset","UTF-8");
        doc.head().appendElement("meta").attr("http-equiv","Content-Security-Policy").attr("content",CSP);
        doc.head().appendElement("meta").attr("name","viewport").attr("content","width=device-width, initial-scale=1");
        doc.head().appendElement("style").text("body{font-family:system-ui,sans-serif;margin:16px;overflow-wrap:anywhere}img{max-width:100%;height:auto}pre,code,.template-plain{white-space:pre-wrap;overflow-wrap:anywhere}table{border-collapse:collapse;max-width:100%}th,td{border:1px solid #999;padding:6px;vertical-align:top}.record-field-state{background:#fff3cd;color:#593c00;padding:8px;border-left:3px solid #b58100}");
        String document="<!DOCTYPE html>"+doc.outerHtml();
        if(document.length()>settings.getMaxRenderedCharacters())throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
        return new Output(document,List.copyOf(warnings));
    }
    private String msg(String key,Locale locale){return messages.getMessage(key,null,locale);}
    private static boolean safeLink(String href){
        try{var uri=URI.create(href);String scheme=uri.getScheme();
            if(("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme))&&uri.getHost()!=null&&uri.getUserInfo()==null)return true;
            return scheme==null&&uri.getRawAuthority()==null&&uri.getPath()!=null&&uri.getPath().matches("/nodes/[1-9][0-9]*");
        }catch(IllegalArgumentException invalid){return false;}
    }
}

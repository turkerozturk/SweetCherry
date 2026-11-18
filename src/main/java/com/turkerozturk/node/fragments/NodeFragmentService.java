package com.turkerozturk.node.fragments;

import com.turkerozturk.node.NodeService;
import com.turkerozturk.richtext.experimental.RichTextRenderingService;
import com.turkerozturk.helpers.highlighter.CodeHighLighter;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class NodeFragmentService {
    @PersistenceContext private EntityManager manager;
    private final NodeService nodes;private final RichTextRenderingService rich;private final NodeFragmentSettings settings;
    public NodeFragmentService(NodeService nodes,RichTextRenderingService rich,NodeFragmentSettings settings){this.nodes=nodes;this.rich=rich;this.settings=settings;}
    public static final class Problem extends RuntimeException {private final String code;public Problem(String code){super(code);this.code=code;}public String code(){return code;}}
    public record Fragment(String contentId,String name,String syntax,String html) { }
    public Fragment fragment(long occurrenceId,String token){
        if(occurrenceId<=0)throw new Problem("missing");
        var source=manager.unwrap(Session.class).doReturningWork(c->NodeFragmentSource.read(c,occurrenceId,settings));
        var node=nodes.findById(source.contentId());if(node==null)throw new Problem("missing");
        String text=node.getTxt()==null?"":node.getTxt();if(text.length()>settings.characters())throw new Problem("large");
        String html;
        try{
            if("custom-colors".equals(source.syntax()))html=rich.renderLive(node,token);
            else if("plain-text".equals(source.syntax()))html="<div style=\"white-space:pre-wrap;overflow-wrap:anywhere\">"+org.jsoup.nodes.Entities.escape(text)+"</div>";
            else if(com.turkerozturk.markdown.NodeMarkup.supports(source.syntax()))html=com.turkerozturk.markdown.NodeMarkup.render(source.syntax(),text,token);
            else html=CodeHighLighter.mappedhighlightLanguage(source.syntax(),text);
        }catch(IllegalArgumentException invalid){throw new Problem("invalid");}
        if(html.length()>settings.htmlCharacters())throw new Problem("large");
        html=readingHtml(html);
        return new Fragment(Long.toString(source.contentId()),source.name(),source.syntax(),html);
    }
    /** Reused content has no editor controls, scripts, frames, remote images or form actions. */
    static String readingHtml(String html){
        var document=Jsoup.parseBodyFragment(html);document.outputSettings().prettyPrint(false);
        document.select("script,style,template,noscript,iframe,object,embed,link,meta,base,form,input,button,textarea,select,svg,math,video,audio,source,picture").remove();
        for(var element:document.getAllElements())for(var attr:element.attributes().asList())
            if(attr.getKey().toLowerCase(java.util.Locale.ROOT).startsWith("on")||attr.getKey().equalsIgnoreCase("srcdoc"))element.removeAttr(attr.getKey());
        for(var image:document.select("img[src]")){
            image.removeAttr("srcset");String src=image.attr("src");if(!src.matches("/images/[1-9][0-9]*/[0-9]+(?:\\?[^#]*)?")&&!src.matches("data:image/(png|jpeg|gif|webp);base64,[A-Za-z0-9+/]+={0,2}"))image.removeAttr("src");
        }
        for(var link:document.select("a[href]")){
            String href=link.attr("href");try{var uri=java.net.URI.create(href);String scheme=uri.getScheme();if(scheme!=null&&!java.util.Set.of("http","https","mailto").contains(scheme.toLowerCase(java.util.Locale.ROOT)))link.removeAttr("href");}
            catch(IllegalArgumentException invalid){link.removeAttr("href");}
        }
        return document.body().html();
    }
}

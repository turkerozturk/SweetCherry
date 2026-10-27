package com.turkerozturk.objects;

import java.nio.charset.StandardCharsets;
import java.util.*;
import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class ObjectPagesTranslationTest {
    @Test void objectPagesTranslateHeadersAndValuesForPopulatedCollections() throws Exception {
        for(String lang:List.of("en","tr")) {
            var node=new Node();node.setNodeId(53);node.setName("<Node>");
            var id=new com.turkerozturk.codebox.CombinedId();id.setNodeId(53);id.setOffset(1);
            var box=new com.turkerozturk.codebox.CodeBox();box.setId(id);box.setNode(node);box.setJustification("left");box.setTxt("x < 2");box.setSyntax("python");box.setIsWidthPix(1);box.setDoHighlBra(0);box.setDoShowLinenum(1);
            var grid=new com.turkerozturk.grid.Grid();grid.setId(id);grid.setNode(node);grid.setJustification("left");grid.setTxt("<table><row><cell>Header</cell></row></table>");
            var image=new com.turkerozturk.image.Image();image.setNodeId(53L);image.setOffset(1);image.setNode(node);image.setJustification("left");image.setPng(new byte[]{1,2});
            var model=new HashMap<String,Object>();model.put("codeboxes",new LinkedHashSet<>(List.of(box)));model.put("grids",new LinkedHashSet<>(List.of(grid)));model.put("images",List.of(image));
            model.put("codeHighLighter",new com.turkerozturk.helpers.highlighter.CodeHighLighter());model.put("xmlHelper",new com.turkerozturk.helpers.XmlHelper());model.put("imgUtil",new com.turkerozturk.image.ImageUtil());
            for(String template:List.of("codebox","grid","image","codeboxes","grids","images")) {
                String html=render(template,lang,new HashMap<>(model));var doc=org.jsoup.Jsoup.parse(html);
                assertThat(html).doesNotContain("??objects.");
                assertThat(doc.select("th").stream().map(org.jsoup.nodes.Element::text)).contains(lang.equals("tr")?"Konum (offset)":"Offset");
                assertThat(doc.select("Node")).isEmpty();
                if(template.equals("codebox")) assertThat(doc.text()).contains(lang.equals("tr")?"Evet":"Yes",lang.equals("tr")?"Sol":"Left");
            }
        }
    }
    @Test void emptyObjectPagesStillRenderTranslatedExplanation() throws Exception {
        for(String lang:List.of("en","tr")) for(String template:List.of("codebox","grid","image")) {
            var model=new HashMap<String,Object>();model.put("codeboxes",List.of());model.put("grids",List.of());model.put("images",List.of());
            assertThat(org.jsoup.Jsoup.parse(render(template,lang,model)).text()).contains(lang.equals("tr")?"Görüntülenecek nesne yok.":"No objects to display.");
        }
    }
    private String render(String template,String lang,Map<String,Object> model) throws Exception {
        var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var response=new MockHttpServletResponse();
        request.setContextPath("/app");request.setRequestURI("/app/mindmap-export");Locale locale=Locale.forLanguageTag(lang);request.addPreferredLocale(locale);
        try(var context=new StaticWebApplicationContext()) {
            context.setServletContext(servlet);context.refresh();servlet.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,context);
            var properties=new Properties();try(var in=getClass().getResourceAsStream("/messages_"+lang+".properties")){properties.load(new java.io.InputStreamReader(in,StandardCharsets.UTF_8));}
            properties.stringPropertyNames().forEach(key->context.getStaticMessageSource().addMessage(key,locale,properties.getProperty(key)));
            model.put("thymeleafRequestContext",new SpringWebMvcThymeleafRequestContext(new RequestContext(request,response,servlet,model),request));
            var engine=new SpringTemplateEngine();var resolver=new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);engine.setMessageSource(context);
            return engine.process(template,new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale,model));
        }
    }
}

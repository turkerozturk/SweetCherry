package com.turkerozturk.quickmindmap;

import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class QuickMapTemplateTest {
    static String render(String template, Locale locale, Map<String,Object> model) throws Exception {
        var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);
        var messages=new StaticMessageSource();var values=new Properties();
        try(var input=Files.newBufferedReader(Path.of("src/main/resources/messages_"+locale.getLanguage()+".properties"))){values.load(input);}
        values.forEach((key,value)->messages.addMessage(key.toString(),locale,value.toString()));engine.setTemplateEngineMessageSource(messages);
        var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);
        var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);
        context.setVariables(model);return engine.process(template,context);
    }
    @Test void mapControlsAndDynamicTooltipsUseTheSelectedLanguage() throws Exception {
        var model=new HashMap<String,Object>();
        model.put("rootNodeName","Literal <title>");model.put("rootNodeId",11L);model.put("rootParentNodeId",0L);
        model.put("maximumLevel",3);model.put("icons",true);model.put("colors",true);model.put("foldChat",true);
        model.put("markmapCanAdd",false);model.put("markmapDataJson","{}");model.put("mermaidDefinition","mindmap\n root((Test))");
        model.put("mermaidColorCss","");model.put("iconNodes",List.of());model.put("navigationNodes",List.of());
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))) {
            for(String page:List.of("markmap-quick-mind-map","quick-mind-map")) {
                String html=render(page,locale,model);var doc=org.jsoup.Jsoup.parse(html);
                assertThat(doc.selectFirst("html").attr("lang")).isEqualTo(locale.getLanguage());
                assertThat(html).doesNotContain("??quickmap.");
                assertThat(doc.selectFirst("label[for=level]").text()).isEqualTo(locale.getLanguage().equals("en")?"Level":"Seviye");
                assertThat(doc.selectFirst("button[type=submit]").text()).isEqualTo(locale.getLanguage().equals("en")?"Apply":"Uygula");
                assertThat(tools.jackson.databind.json.JsonMapper.builder().build().readValue(html.split("const openContentLabel = ")[1].split(";")[0],String.class)).isEqualTo(locale.getLanguage().equals("en")?"Open node content in a new tab":"Düğüm içeriğini yeni sekmede aç");
                if(page.startsWith("markmap")) {
                    assertThat(doc.selectFirst("#markmapCanvasToggle").attr("aria-pressed")).isEqualTo("false");
                    assertThat(doc.selectFirst("main").classNames()).doesNotContain("markmap-page-only");
                }
                model.put("rootNodeId",0L);
                assertThat(org.jsoup.Jsoup.parse(render(page,locale,model)).selectFirst(".badge").text())
                        .isEqualTo(locale.getLanguage().equals("en")?"All top-level nodes":"Tüm üst düğümler");
                model.put("rootNodeId",11L);
            }
        }
    }
    @Test void quickContentHeaderEmptyStateAndImageTooltipAreLocalized() throws Exception {
        var node=new Node();node.setName("A <literal> title");node.setSyntax("plain-text");node.setTxt("Plain content");
        var model=new HashMap<String,Object>();model.put("node",node);model.put("requestedNodeId",11L);model.put("contentNodeId",10L);
        model.put("isAlias",true);model.put("hasEmptyContent",true);model.put("syntaxHighlightingEnabled",false);
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))) {
            String html=render("node/quick-node-content",locale,model);var doc=org.jsoup.Jsoup.parse(html);
            assertThat(doc.selectFirst("html").attr("lang")).isEqualTo(locale.getLanguage());
            assertThat(doc.selectFirst("h1").text()).isEqualTo(node.getName());
            assertThat(doc.selectFirst("button").text()).isEqualTo(locale.getLanguage().equals("en")?"Close":"Kapat");
            assertThat(doc.selectFirst(".quick-node-content").text()).isEqualTo(locale.getLanguage().equals("en")?"This node has no content.":"Bu düğümün içeriği boş.");
            assertThat(tools.jackson.databind.json.JsonMapper.builder().build().readValue(html.split("const openImageLabel = ")[1].split(";")[0],String.class)).isEqualTo(locale.getLanguage().equals("en")?"Open original image in a new tab":"Görseli gerçek boyutta yeni sekmede aç");
            assertThat(html).doesNotContain("??quickcontent.");
        }
    }
}

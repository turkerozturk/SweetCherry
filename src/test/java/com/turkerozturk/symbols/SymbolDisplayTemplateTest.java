package com.turkerozturk.symbols;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class SymbolDisplayTemplateTest {
    @Test void standaloneMapsLoadLocalDisplayAssetsOnceAndKeepNamesEscapedBeforeLayout() {
        var model=new HashMap<String,Object>();model.put("rootNodeName","Name {{fa-solid fa-image}} <img onerror=bad>");model.put("rootNodeId",11L);model.put("rootParentNodeId",0L);
        model.put("maximumLevel",3);model.put("icons",true);model.put("colors",true);model.put("foldChat",true);model.put("markmapCanAdd",false);
        model.put("markmapDataJson","{\"content\":\"Name {{fa-solid fa-image}}\",\"children\":[]}");model.put("mermaidDefinition","mindmap\n root((Test))");
        model.put("mermaidColorCss","");model.put("iconNodes",List.of());model.put("navigationNodes",List.of());
        var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
        var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
        for(String language:List.of("en","tr"))for(String page:List.of("quick-mind-map","markmap-quick-mind-map")) {
            var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setContextPath("/app");request.setRequestURI("/app/"+page);
            var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),Locale.forLanguageTag(language));context.setVariables(model);
            String html=engine.process(page,context);var doc=org.jsoup.Jsoup.parse(html);
            assertThat(doc.select("script[src='/app/js/symbols/catalog.js']")).hasSize(1);
            assertThat(doc.select("script[src='/app/js/symbols/inlineSymbols.js']")).hasSize(1);
            assertThat(doc.select("script[src='/app/js/symbols/images.js']")).hasSize(1);
            assertThat(doc.select("dialog#symbol-picker")).isEmpty();
            assertThat(doc.select("img[onerror]")).isEmpty();assertThat(doc.select("[data-inline-symbols]").text()).contains("Name {{fa-solid fa-image}}");
            assertThat(doc.select("title[data-symbols-native]")).hasSize(1);
            assertThat(doc.select("script[data-ctb-base]").eachAttr("data-ctb-base")).containsOnly("/app/img/ctbicons/");
            assertThat(html).doesNotContain("??symbols.");
        }
    }
}

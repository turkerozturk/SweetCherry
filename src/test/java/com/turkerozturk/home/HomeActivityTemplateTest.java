package com.turkerozturk.home;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.*;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class HomeActivityTemplateTest {
    @Test void homeButtonsAndActivityRenderEscapedStyledOwnOccurrencePathsInBothLanguages() throws Exception {
        for (String lang : List.of("tr", "en")) {
            Locale locale=Locale.forLanguageTag(lang);var engine=new SpringTemplateEngine();
            var files=new ClassLoaderTemplateResolver();files.setPrefix("templates/");files.setSuffix(".html");files.setOrder(2);
            engine.setTemplateResolver(files);
            var messages=new StaticMessageSource();var properties=new Properties();
            try(var in=Files.newBufferedReader(Path.of("src/main/resources/messages_"+lang+".properties"))){properties.load(in);}
            properties.forEach((k,v)->messages.addMessage(k.toString(),locale,v.toString()));engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);
            var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);
            context.setVariable("currentTenantName","Database");context.setVariable("currentDatabaseIcon","ct_db");context.setVariable("tenantViewToken","token");context.setVariable("canDeleteNode",false);
            context.setVariable("homeModifiedEnabled",true);context.setVariable("homeCreatedEnabled",true);
            context.setVariable("homeModified",List.of(new HomeActivityService.Entry(11,"<Shared>","ct_db",true,"#ff0000","2026-11-02 12:34:56",false,List.of(new HomeActivityService.Crumb(2,"Parent"),new HomeActivityService.Crumb(11,"<Shared>")))));
            context.setVariable("homeCreated",List.of());
            String html=engine.process("node/rootWelcomeFragment",context);
            var doc=org.jsoup.Jsoup.parse(html);
            assertThat(doc.select("a.btn")).hasSize(3);
            var link=doc.selectFirst("a[href^=/nodes/11]");assertThat(link.text()).isEqualTo("<Shared>");
            assertThat(link.attr("href")).contains("_tenantView=token");assertThat(link.attr("style")).contains("#ff0000","bold");
            assertThat(doc.select("time").text()).isEqualTo("2026-11-02 12:34:56");
            assertThat(html).doesNotContain("??home.activity", "<Shared>");
            assertThat(doc.select("h2.h5")).hasSize(2);
        }
    }
}

package com.turkerozturk.icons;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;
class IconTemplateTest {
    @Test void bothLanguagesEscapeSearchAndKeepSortFilterLinksAndStableIndices(){
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);engine.setTemplateEngineMessageSource(IconCatalogueTest.messages());
            var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),locale);
            var controller=new IconController(IconCatalogueTest.messages());var data=controller.listing(new IconController.Options("index","desc","cat","animals"),locale);context.setVariable("icons",data);
            var doc=org.jsoup.Jsoup.parse(engine.process("icons",context));assertThat(doc.select("#icon-table tbody tr")).hasSize(1);
            assertThat(doc.select("select[data-icon-category][name=category]")).hasSize(1);
            assertThat(doc.select("script[src='/js/icons/iconFilters.js']")).hasSize(1);
            assertThat(doc.select("#icon-table tbody td").first().text()).isEqualTo("212");
            assertThat(doc.select("th[aria-sort=descending]")).hasSize(1);assertThat(doc.select("th a")).allMatch(a->a.attr("href").contains("category=animals")&&a.attr("href").contains("q=cat"));
            assertThat(doc.select("img").attr("src")).endsWith("/img/icons/ct_cat.svg");assertThat(doc.text()).doesNotContain("??");
            context.setVariable("icons",controller.listing(new IconController.Options("name","asc","<script>unsafe()</script>\"&",""),locale));String html=engine.process("icons",context);
            assertThat(html).doesNotContain("<script>unsafe()","??");assertThat(org.jsoup.Jsoup.parse(html).select("#icon-search").val()).isEqualTo("<script>unsafe()</script>\"&");
        }
    }
}

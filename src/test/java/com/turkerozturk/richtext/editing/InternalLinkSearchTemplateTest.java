package com.turkerozturk.richtext.editing;

import java.util.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class InternalLinkSearchTemplateTest {
    @Test void internalLinkDialogHasSeparateContainsQueryAndLabelInBothLanguages()throws Exception{
        String source;try(var in=getClass().getResourceAsStream("/templates/node/richtext-edit.html")){source=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        String fragment=source.substring(source.indexOf("<dialog id=\"richInternalDialog\""));fragment=fragment.substring(0,fragment.indexOf("</dialog>")+9);
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
            var engine=new SpringTemplateEngine();engine.setTemplateResolver(new StringTemplateResolver());var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setContextPath("/app");request.setRequestURI("/app/editor");
            var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);
            var html=engine.process(fragment,context);var doc=org.jsoup.Jsoup.parse(html);
            assertThat(html).doesNotContain("??");assertThat(doc.selectFirst("#richInternalDialog").attr("data-search-url")).isEqualTo("/app/nodes/quick/search");
            assertThat(doc.selectFirst("#richInternalQuery").attr("maxlength")).isEqualTo("160");assertThat(doc.selectFirst("#richInternalText").attr("maxlength")).isEqualTo("10000");
            assertThat(doc.select("#richInternalSearch")).isEmpty();assertThat(doc.selectFirst("#richInternalSearchStatus").attr("aria-live")).isEqualTo("polite");
        }
    }
}

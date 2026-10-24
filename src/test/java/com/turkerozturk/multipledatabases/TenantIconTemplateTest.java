package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class TenantIconTemplateTest {
    @Test void selectionListRendersIconInsideEachButton()throws Exception {
        String template;try(var in=getClass().getResourceAsStream("/templates/common/tenantFragment.html")){template=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        template=template.substring(template.indexOf("<details"),template.indexOf("</details>")+10);
        for(String language:List.of("en","tr")) {
            var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var response=new MockHttpServletResponse();
            Locale locale=Locale.forLanguageTag(language);request.addPreferredLocale(locale);
            try(var context=new StaticWebApplicationContext()) {
                context.setServletContext(servlet);context.refresh();servlet.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,context);
                var properties=new Properties();try(var in=getClass().getResourceAsStream(language.equals("tr")?"/messages_tr.properties":"/messages.properties")){properties.load(new java.io.InputStreamReader(in,StandardCharsets.UTF_8));}
                properties.stringPropertyNames().forEach(key->context.getStaticMessageSource().addMessage(key,locale,properties.getProperty(key)));
                Map<String,Object> model=new HashMap<>();model.put("databaseIcons",Map.of("Test","cherry_blue","Other","ct_db"));model.put("tenantNames",List.of("Test","Other"));model.put("currentTenantName","Test");model.put("currentDatabaseIcon","cherry_blue");
                model.put("tenant","<target>");model.put("token","token");model.put("tenantViewToken","view");
                model.put("thymeleafRequestContext",new SpringWebMvcThymeleafRequestContext(new RequestContext(request,response,servlet,model),request));
                var engine=new SpringTemplateEngine();engine.setTemplateResolver(new StringTemplateResolver());engine.setMessageSource(context);
                String html=engine.process(template,new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale,model));
                var doc=org.jsoup.Jsoup.parse(html);
                assertThat(doc.selectFirst("summary img").attr("src")).isEqualTo("/img/ctbicons/cherry_blue.png");
                assertThat(doc.selectFirst("button[value=Test] img").attr("src")).isEqualTo("/img/ctbicons/cherry_blue.png");
                assertThat(doc.selectFirst("button[value=Other] img").attr("src")).isEqualTo("/img/ctbicons/ct_db.png");
            }
        }
    }
}

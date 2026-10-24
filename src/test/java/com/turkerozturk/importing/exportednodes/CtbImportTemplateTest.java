package com.turkerozturk.importing.exportednodes;

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

class CtbImportTemplateTest {
    @Test void confirmationRendersCountsAndEscapesNamesInBothLanguages()throws Exception {
        String template;try(var in=getClass().getResourceAsStream("/templates/ctbImportPreview.html")){template=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        for(String language:List.of("en","tr")) {
            var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var response=new MockHttpServletResponse();
            Locale locale=Locale.forLanguageTag(language);request.addPreferredLocale(locale);
            try(var context=new StaticWebApplicationContext()) {
                context.setServletContext(servlet);context.refresh();servlet.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,context);
                var properties=new Properties();try(var in=getClass().getResourceAsStream(language.equals("tr")?"/messages_tr.properties":"/messages.properties")){properties.load(new java.io.InputStreamReader(in,StandardCharsets.UTF_8));}
                properties.stringPropertyNames().forEach(key->context.getStaticMessageSource().addMessage(key,locale,properties.getProperty(key)));
                Map<String,Object> model=new HashMap<>();model.put("preview",new CtbImportService.Preview("<demo>.ctb","hash",Map.of("node",2,"children",3)));
                model.put("tenant","<target>");model.put("token","token");model.put("tenantViewToken","view");
                model.put("thymeleafRequestContext",new SpringWebMvcThymeleafRequestContext(new RequestContext(request,response,servlet,model),request));
                var engine=new SpringTemplateEngine();engine.setTemplateResolver(new StringTemplateResolver());engine.setMessageSource(context);
                String html=engine.process(template,new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale,model));
                var doc=org.jsoup.Jsoup.parse(html);
                assertThat(doc.select("tbody tr")).hasSize(2);
                assertThat(doc.selectFirst("input[name=token]").val()).isEqualTo("token");
                assertThat(doc.selectFirst("input[name=_tenantView]").val()).isEqualTo("view");
                assertThat(html).contains("&lt;demo&gt;.ctb","&lt;target&gt;").doesNotContain("??ctbImport.");
            }
        }
    }
}

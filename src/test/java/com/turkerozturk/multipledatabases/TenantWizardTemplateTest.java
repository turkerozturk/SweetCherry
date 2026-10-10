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

class TenantWizardTemplateTest {
    @Test void rendersBoundEditFormInBothLanguagesWithoutPasswordDisclosure()throws Exception {
        String template;
        try(var in=getClass().getResourceAsStream("/templates/tenantWizard.html")){
            template=new String(in.readAllBytes(),StandardCharsets.UTF_8).replace("    <div th:replace=\"~{common/backupWarningFragment :: backupWarning}\"></div>","");
        }
        for(String language:List.of("en","tr")) {
            var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var response=new MockHttpServletResponse();
            Locale locale=Locale.forLanguageTag(language);request.addPreferredLocale(locale);
            try(var context=new StaticWebApplicationContext()) {
                context.setServletContext(servlet);context.refresh();servlet.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,context);
                var properties=new Properties();try(var in=getClass().getResourceAsStream(language.equals("tr")?"/messages_tr.properties":"/messages.properties")){
                    properties.load(new java.io.InputStreamReader(in,StandardCharsets.UTF_8));
                }
                properties.stringPropertyNames().forEach(key->context.getStaticMessageSource().addMessage(key,locale,properties.getProperty(key)));
                var form=new TenantWizardForm();form.tenant="Existing";form.name="<Türkçe>";form.path="C:/notlar/çığ.ctb";form.password="never-show-secret";
                Map<String,Object> model=new HashMap<>();model.put("wizard",form);model.put("wizardError","wizard.stale");
                model.put("thymeleafRequestContext",new SpringWebMvcThymeleafRequestContext(new RequestContext(request,response,servlet,model),request));
                var engine=new SpringTemplateEngine();engine.setTemplateResolver(new StringTemplateResolver());engine.setMessageSource(context);
                String html=engine.process(template,new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale,model));
                var document=org.jsoup.Jsoup.parse(html);
                assertThat(document.selectFirst("input[name=name]").val()).isEqualTo("<Türkçe>");
                assertThat(document.selectFirst("input[name=path]").val()).isEqualTo("C:/notlar/çığ.ctb");
                assertThat(html).contains("/tenants/wizard").doesNotContain("never-show-secret","??wizard.");
            }
        }
    }
}

package com.turkerozturk.node.navigation;

import java.nio.charset.StandardCharsets;
import java.util.*;
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

class NavigationPolishTemplateTest {
    @Test void goDialogAndDeleteResultRenderInBothLanguagesWithContextSafeUrls() throws Exception {
        for(String language:List.of("en","tr")) {
            var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var response=new MockHttpServletResponse();
            request.setContextPath("/app");request.setRequestURI("/app/exportedFiles");
            Locale locale=Locale.forLanguageTag(language);request.addPreferredLocale(locale);
            try(var context=new StaticWebApplicationContext()) {
                context.setServletContext(servlet);context.refresh();servlet.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,context);
                var properties=new Properties();try(var in=getClass().getResourceAsStream("/messages_"+language+".properties")){properties.load(new java.io.InputStreamReader(in,StandardCharsets.UTF_8));}
                properties.stringPropertyNames().forEach(key->context.getStaticMessageSource().addMessage(key,locale,properties.getProperty(key)));
                Map<String,Object> model=new HashMap<>();model.put("currentTenantName","Read only");model.put("tenantViewToken","token");
                model.put("deleteMessage","exportedFiles.deleteSuccess");model.put("deleteSucceeded",true);model.put("filename","<unsafe>.ctb");
                model.put("thymeleafRequestContext",new SpringWebMvcThymeleafRequestContext(new RequestContext(request,response,servlet,model),request));
                var engine=new SpringTemplateEngine();var resolver=new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);engine.setMessageSource(context);
                var web=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale,model);
                String html=engine.process("node/nodeGoFragment",web);var doc=org.jsoup.Jsoup.parse(html);
                assertThat(doc.selectFirst("form").attr("action")).isEqualTo("/app/nodes/go-to");
                assertThat(doc.selectFirst("form").attr("data-bookmarks-url")).isEqualTo("/app/nodes/go-to/bookmarks");
                assertThat(doc.selectFirst("h2").text()).isEqualTo(language.equals("tr")?"Düğüm ID’sine git":"Go to Node ID");
                assertThat(doc.selectFirst("[type=submit]").text()).isEqualTo(language.equals("tr")?"Git":"Go");
                assertThat(doc.selectFirst("input[name=_tenantView]").val()).isEqualTo("token");
                assertThat(html).doesNotContain("??node.");
                html=engine.process("exportedFileResult",web);doc=org.jsoup.Jsoup.parse(html);
                assertThat(doc.selectFirst("a[href]").attr("href")).isEqualTo("/app/exportedFiles");
                assertThat(doc.select("unsafe")).isEmpty();
                assertThat(doc.text()).contains(language.equals("tr")?"Dosya başarıyla silindi.":"File deleted successfully.");
            }
        }
    }
}

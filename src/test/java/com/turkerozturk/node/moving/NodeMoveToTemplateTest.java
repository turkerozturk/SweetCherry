package com.turkerozturk.node.moving;

import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeMoveToTemplateTest {
    @Test void bothReadersRenderTranslatedDialogAndContextAwareReturnUrl() throws Exception {
        for(String language:List.of("en","tr"))for(String view:List.of("reader","tree")) {
            var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var response=new MockHttpServletResponse();
            request.setContextPath("/app");request.setRequestURI("/app/tree");
            Locale locale=Locale.forLanguageTag(language);request.addPreferredLocale(locale);
            try(var context=new StaticWebApplicationContext()) {
                context.setServletContext(servlet);context.refresh();servlet.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,context);
                var properties=new Properties();try(var in=getClass().getResourceAsStream("/messages_"+language+".properties")){properties.load(new java.io.InputStreamReader(in,StandardCharsets.UTF_8));}
                properties.stringPropertyNames().forEach(key->context.getStaticMessageSource().addMessage(key,locale,properties.getProperty(key)));
                var node=mock(Node.class);when(node.getName()).thenReturn("<unsafe> & title");
                Map<String,Object> model=new HashMap<>();model.put("node",node);model.put("requestedNodeId",9L);model.put("canDeleteNode",true);model.put("view",view);model.put("tenantViewToken","token");
                model.put("thymeleafRequestContext",new SpringWebMvcThymeleafRequestContext(new RequestContext(request,response,servlet,model),request));
                var engine=new SpringTemplateEngine();var resolver=new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);engine.setMessageSource(context);
                String html=engine.process("node/nodeMoveToFragment",new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale,model));
                var doc=org.jsoup.Jsoup.parse(html);
                assertThat(doc.selectFirst("form").attr("action")).isEqualTo("/app/nodes/move/9/to");
                assertThat(doc.selectFirst("input[name=returnUrl]").val()).isEqualTo(view.equals("tree")?"/app/tree?nodeId=9&_tenantView=token":"/app/nodes/9?_tenantView=token");
                assertThat(doc.selectFirst("input[name=_tenantView]").val()).isEqualTo("token");
                assertThat(doc.selectFirst("label").attr("for")).isEqualTo(doc.selectFirst("input[name=parentId]").id());
                assertThat(doc.select("unsafe")).isEmpty();
                assertThat(doc.selectFirst("[data-move-to-bookmarks]").parent().hasClass("input-group")).isTrue();
                assertThat(doc.selectFirst("form").attr("data-bookmarks-url")).isEqualTo("/app/nodes/move/9/bookmarks");
                assertThat(doc.selectFirst("form").attr("data-icon-base")).isEqualTo("/app/img/ctbicons/");
                assertThat(html).doesNotContain("??node.moveTo.");
            }
        }
    }
}

package com.turkerozturk.richtext.editing;

import com.turkerozturk.node.Node;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebBookmarkTemplateTest {
 @Test void desktopAndMobileUseAnOpaqueSandboxAndAnExplicitExternalIconInBothLanguages(){
  for(String language:List.of("en","tr")){
   var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
   var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
   var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);
   var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),Locale.forLanguageTag(language));
   var node=mock(Node.class);when(node.getNodeId()).thenReturn(9L);when(node.getName()).thenReturn("https://example.org/?a=1&b=2#part");when(node.getSyntax()).thenReturn("plain-text");when(node.isContentEmpty()).thenReturn(true);when(node.getBreadcrumbs()).thenReturn(new LinkedHashMap<>());
   when(node.getNodeIcon()).thenReturn(com.turkerozturk.helpers.NodeIcon.OTHER);
   context.setVariable("node",node);context.setVariable("requestedNodeId",9L);context.setVariable("tenantViewToken","current");context.setVariable("webBookmarkUrl",node.getName());context.setVariable("canDeleteNode",false);context.setVariable("isSharedNode",false);context.setVariable("childNodes",List.of());context.setVariable("tenantNames",List.of());context.setVariable("_csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","csrf"));
   for(String template:List.of("node/treeContentFragment","node/mobileReader")){
    var html=engine.process(template,context);var page=org.jsoup.Jsoup.parse(html);var frame=page.selectFirst("iframe[data-web-bookmark-frame]");
    assertThat(frame).isNotNull();assertThat(frame.hasAttr("sandbox")).isTrue();assertThat(frame.attr("sandbox")).isEmpty();assertThat(frame.hasAttr("src")).isFalse();assertThat(frame.attr("referrerpolicy")).isEqualTo("no-referrer");
    assertThat(page.selectFirst("[data-web-bookmark]").attr("data-endpoint")).isEqualTo("/nodes/web-bookmarks/9");
    assertThat(page.select(".node-empty-content")).isEmpty();
    var source=page.selectFirst(".node-web-bookmark-open");assertThat(source.attr("href")).isEqualTo(node.getName());assertThat(source.attr("target")).isEqualTo("_blank");assertThat(source.attr("rel")).contains("noopener","noreferrer");assertThat(source.selectFirst("img").attr("src")).endsWith("/ct_link_website.png");
    assertThat(source.parent().children().last()).isSameAs(source);assertThat(html).doesNotContain("??webBookmark.");
   }
   context.setVariable("webBookmarkUrl",null);var normal=org.jsoup.Jsoup.parse(engine.process("node/treeContentFragment",context));assertThat(normal.select("[data-web-bookmark],.node-web-bookmark-open")).isEmpty();assertThat(normal.select(".node-empty-content")).hasSize(1);
  }
 }
}

package com.turkerozturk.node.moving;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class SortChildrenTemplateTest {
 @Test void submenuHasSixLocalizedOrdersContextAwareEndpointsAndPostCsrfForm(){
  for(String language:List.of("en","tr"))for(String path:List.of("","/app")){
   var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
   var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
   var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setContextPath(path);request.setRequestURI(path+"/tree");
   var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),Locale.forLanguageTag(language));context.setVariable("tenantViewToken","current");
   String html=engine.process("common/sortChildren",context);var doc=org.jsoup.Jsoup.parse(html);assertThat(html).doesNotContain("??sortChildren.");
   assertThat(doc.selectFirst("[data-sort-children]").attr("data-base")).isEqualTo(path+"/nodes/children/");assertThat(doc.selectFirst("[data-sort-children]").attr("data-token")).isEqualTo("current");
   assertThat(doc.select("details button[data-sort-key]").eachAttr("data-sort-key")).containsExactly("CREATED","MODIFIED","NAME","CREATED","MODIFIED","NAME");
   assertThat(doc.select("details button[data-sort-key]").eachAttr("data-sort-order")).containsExactly("DESC","DESC","DESC","ASC","ASC","ASC");
   assertThat(doc.select("details button").get(0).text()).contains("↓");assertThat(doc.select("details button").get(3).text()).contains("↑");
   assertThat(doc.selectFirst("form[data-sort-csrf]").attr("method")).isEqualTo("post");assertThat(doc.selectFirst("form[data-sort-csrf]").attr("action")).isEqualTo(path+"/nodes/children/1/sort");
  }
 }
}

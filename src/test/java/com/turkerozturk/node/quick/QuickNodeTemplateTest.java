package com.turkerozturk.node.quick;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.*;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class QuickNodeTemplateTest {
 @Test void bothLanguagesRenderNamedControlsWithoutRawMessageKeys() throws Exception {
  for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))) {
   var engine=new SpringTemplateEngine();engine.setTemplateResolver(new StringTemplateResolver());
   var messages=new StaticMessageSource();var values=new Properties();
   try(var input=Files.newBufferedReader(Path.of("src/main/resources/messages_"+locale.getLanguage()+".properties"))){values.load(input);}
   values.forEach((k,v)->messages.addMessage(k.toString(),locale,v.toString()));engine.setTemplateEngineMessageSource(messages);
   var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var response=new MockHttpServletResponse();
   var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale);
   context.setVariable("currentTenantName","Demo");context.setVariable("tenantViewToken","current");
   context.setVariable("_csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","csrf-value"));
   String html=engine.process(Files.readString(Path.of("src/main/resources/templates/node/quickNodesFragment.html")),context);
   assertThat(html).contains("quick-switch-dialog","quick-note-dialog","csrf-value","maxlength=\"200000\"","value=\"current\"")
    .doesNotContain("??quick.");
  }
 }
}

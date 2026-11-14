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
class BatchNodeTemplateTest {
 @Test void dialogControlsAreEscapedAndLocalizedInBothLanguages(){
  for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);
   var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
   var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);
   context.setVariable("tenantViewToken","current");context.setVariable("searchTags",List.of("<img onerror=evil()>"));
   var html=engine.process("node/batchNodes",context);var doc=org.jsoup.Jsoup.parse(html);
   assertThat(html).doesNotContain("??","<img onerror=");assertThat(doc.select("input[name=_tenantView]").val()).isEqualTo("current");assertThat(doc.select("datalist option").val()).isEqualTo("<img onerror=evil()>");
   assertThat(doc.select("[data-batch-save]").text()).isEqualTo(locale.equals(Locale.ENGLISH)?"Save":"Kaydet");assertThat(doc.select("[data-batch-cancel]").text()).isEqualTo(locale.equals(Locale.ENGLISH)?"Cancel":"İptal");
   assertThat(doc.select("button").not("[type]")).isEmpty();assertThat(doc.select("form").attr("method")).isEqualTo("post");
  }
 }
}

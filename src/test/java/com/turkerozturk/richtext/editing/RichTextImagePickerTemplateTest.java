package com.turkerozturk.richtext.editing;
import java.util.*;
import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;
class RichTextImagePickerTemplateTest {
 @Test void pickerAcceptsMultipleImagesAndExplainsTheBehaviorInBothLanguages(){
  for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
   var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);
   var node=new Node();node.setNodeId(3);node.setName("<unsafe>");node.setSyntax("custom-colors");context.setVariable("tenantViewToken","current");context.setVariable("editor",Map.of("node",node,"document",Map.of("runs",List.of()),"revision","r","objects",Map.of(),"maxEmbeddedFileBytes",9000000));
   String html=engine.process("node/richtext-edit",context);var doc=org.jsoup.Jsoup.parse(html);assertThat(html).doesNotContain("??","<unsafe>");
   assertThat(doc.selectFirst("#richImageFile").hasAttr("multiple")).isTrue();assertThat(doc.select("#richImageFile").attr("accept")).isEqualTo("image/png,image/jpeg");assertThat(doc.selectFirst("#richFilePicker").hasAttr("multiple")).isFalse();
   assertThat(doc.select("#richAddImage").attr("aria-describedby")).isEqualTo("richImageHelp");assertThat(doc.select("#richImageHelp").text()).contains(locale.equals(Locale.ENGLISH)?"one or more":"Bir veya daha fazla");assertThat(doc.select("#richImageStatus").attr("data-loading")).isNotBlank();
  }
 }
}

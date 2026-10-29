package com.turkerozturk.markdown;
import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.*;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.util.*;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
class MarkdownTemplateTest {
 @Test void markdownSourceEditorRendersTranslatedToolsAndExistingRecoveryWithoutChangingPlainEditor()throws Exception{
  var node=new Node();node.setNodeId(1);node.setName("Name");node.setSyntax("markdown");node.setTxt("# <source>");
  for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   String html=render("node/nodeContentEdit.html",Map.of("node",node,"editorRecoveryScope","scope"),locale);
   assertThat(html).contains("markdownTools","markdownPreviewPane","markdownEditor.js","editorRecovery","plainTextEditor.js","# &lt;source&gt;").doesNotContain("??markdown.","??richtext.");
  }
  node.setSyntax("plain-text");String plain=render("node/nodeContentEdit.html",Map.of("node",node,"editorRecoveryScope","scope"),Locale.ENGLISH);assertThat(plain).doesNotContain("id=\"markdownTools\"","markdownEditor.js");
 }
 private static String render(String file,Map<String,Object> model,Locale locale)throws Exception {
  var engine=new SpringTemplateEngine();var fragments=new ClassLoaderTemplateResolver();fragments.setPrefix("templates/");fragments.setSuffix(".html");fragments.setResolvablePatterns(Set.of("common/*"));fragments.setOrder(1);fragments.setCheckExistence(true);engine.addTemplateResolver(fragments);
  var strings=new StringTemplateResolver();strings.setOrder(2);strings.setTemplateMode("HTML");engine.addTemplateResolver(strings);
  var messages=new StaticMessageSource();var properties=new Properties();try(var in=Files.newBufferedReader(Path.of("src/main/resources/messages_"+locale.getLanguage()+".properties"))){properties.load(in);}
  properties.forEach((key,value)->messages.addMessage(key.toString(),locale,value.toString()));engine.setTemplateEngineMessageSource(messages);
  var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setRequestURI("/nodes/tags");var response=new MockHttpServletResponse();
  var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale);context.setVariables(model);context.setVariable("tenantViewToken","v");
  return engine.process(Files.readString(Path.of("src/main/resources/templates/"+file)),context);
 }
}

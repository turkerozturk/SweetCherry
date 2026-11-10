package com.turkerozturk.html;
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
class HtmlTemplateTest {
 @Test void htmlSourceEditorRendersTranslatedToolsAndExistingRecoveryWithoutChangingPlainEditor()throws Exception{
  var node=new Node();node.setNodeId(1);node.setName("Name");node.setSyntax("html");node.setTxt("<p>Source</p>");
  for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   String html=render("node/nodeContentEdit.html",Map.of("node",node,"editorRecoveryScope","scope"),locale);
   assertThat(html).contains("htmlTools","htmlPreviewPane","htmlEditor.js","sandbox=\"allow-popups allow-popups-to-escape-sandbox\"","editorRecovery","plainTextEditor.js","&lt;p&gt;Source&lt;/p&gt;").doesNotContain("??html.","??richtext.");
   assertThat(org.jsoup.Jsoup.parse(html).getElementById("htmlPreviewToggle").text()).isEqualTo(locale.getLanguage().equals("tr")?"HTML önizleme":"HTML preview");
  }
  node.setSyntax("plain-text");String plain=render("node/nodeContentEdit.html",Map.of("node",node,"editorRecoveryScope","scope"),Locale.ENGLISH);assertThat(plain).doesNotContain("id=\"htmlTools\"","htmlEditor.js");
 }

 @Test void propertiesShortcutAndReaderMetadataRenderInBothLanguages()throws Exception{
  var service=org.mockito.Mockito.mock(com.turkerozturk.node.properties.NodePropertiesService.class);
  org.mockito.Mockito.when(service.writable()).thenReturn(true);
  var node=new Node();node.setNodeId(1);node.setName("HTML");node.setSyntax("html");node.parseNodeIconAndReadOnlyFlag();
  org.mockito.Mockito.when(service.realNode(1)).thenReturn(node);
  for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   for(boolean locked:List.of(false,true)){
    node.setIsReadOnly16bit(locked?1:0);
    var model=new org.springframework.ui.ExtendedModelMap();new com.turkerozturk.node.properties.NodePropertiesController(service).edit(1,model);
    var doc=org.jsoup.Jsoup.parse(render("node/nodeProperties.html",model.asMap(),locale));
    var shortcut=doc.getElementById("propertyHtml");assertThat(shortcut.hasAttr("disabled")).isEqualTo(locked);
    assertThat(shortcut.attr("title")).doesNotContain("??");
    assertThat(doc.select("#codeSyntax option[value=html]")).hasSize(1);
   }
   assertThat(render("node/nodeMetaDataFragment.html",Map.of("node",node),locale)).contains("HTML").doesNotContain("??html.");
  }
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

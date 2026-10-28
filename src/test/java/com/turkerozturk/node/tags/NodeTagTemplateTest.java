package com.turkerozturk.node.tags;
import com.turkerozturk.node.properties.*;
import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.*;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.util.*;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class NodeTagTemplateTest {
 @Test void propertiesRenderFreeTagsAndExistingTypeControlsInBothLanguages()throws Exception{
  var service=mock(NodePropertiesService.class);when(service.writable()).thenReturn(true);
  var node=new Node();node.setNodeId(2);node.setName("Name");node.setTags("<x> Türkçe");node.setSyntax("plain-text");when(service.realNode(2)).thenReturn(node);
  var model=new ExtendedModelMap();new NodePropertiesController(service).edit(2,model);
  for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))) {
   String html=render("node/nodeProperties.html",model,locale);
   assertThat(html).contains("name=\"tags\"","&lt;x&gt; Türkçe","nodeTagsChoose","col-md-6","form-control-sm","typePlainText","name=\"_tenantView\"").doesNotContain("??tags.");
  }
 }
 @Test void viewEscapesTagsAndUsesSharedIdForNavigationButMasterForProperties()throws Exception{
  var options=new NodeTagService.Options(List.of("C++","one,two"),"any",1,1);
  var row=new NodeTagService.Row(11,2,"<Name>","ct_code",true,"#3584E4",true,true,"Parent [1] / <Name> [11]",List.of("C++","one,two","<tag>"));
  var model=new ExtendedModelMap();model.addAttribute("tagView",new NodeTagService.View(List.of("C++","one,two"),List.of(row),1,2,2,options));model.addAttribute("tagChoices",List.of("C++","one,two","<tag>"));model.addAttribute("canEditTags",true);
  String html=render("node/tags.html",model,Locale.ENGLISH);
  assertThat(html).contains("/nodes/11?_tenantView=v","/nodes/properties/2?_tenantView=v","&lt;Name&gt;","&lt;tag&gt;","Parent [1] / &lt;Name&gt; [11]","Original properties").doesNotContain("<tag>");
  String href=org.jsoup.Jsoup.parse(html).select("nav a").last().attr("href");
  var selected=java.util.Arrays.stream(java.net.URI.create(href).getRawQuery().split("&")).filter(value->value.startsWith("tag=")).map(value->java.net.URLDecoder.decode(value.substring(4),java.nio.charset.StandardCharsets.UTF_8)).toList();
  assertThat(selected).containsExactly("C++","one,two");
  var unfiltered=new NodeTagService.View(List.of("C++","one,two"),List.of(row),1,2,2,new NodeTagService.Options(List.of(),"any",1,1));
  model.addAttribute("tagView",unfiltered);
  String unfilteredNext=org.jsoup.Jsoup.parse(render("node/tags.html",model,Locale.ENGLISH)).select("nav a").last().attr("href");
  assertThat(unfilteredNext).doesNotContain("tag=");
  model.addAttribute("canEditTags",false);assertThat(render("node/tags.html",model,Locale.ENGLISH)).doesNotContain("/nodes/properties/2");
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

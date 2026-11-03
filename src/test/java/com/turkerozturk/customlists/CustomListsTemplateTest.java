package com.turkerozturk.customlists;
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
class CustomListsTemplateTest {
 @Test void helpRoutesUsePackagedTemplateNames(){
  var controller=new com.turkerozturk.help.HelpController();org.springframework.test.util.ReflectionTestUtils.setField(controller,"customListsSettings",new CustomListsSettings());
  assertThat(controller.helpTasks(new ExtendedModelMap())).isEqualTo("help/cherrytemplatetasks");
  assertThat(controller.helpTemplateNode(new ExtendedModelMap())).isEqualTo("help/cherrytemplatenode");
 }
 @Test void setupAndHelpRenderConfiguredNamesAndActionsInBothLanguages()throws Exception {
  var names=new CustomListsSettings.Names("My <lists>","my-prefix");
  var match=new CustomListsService.Match(1,names.main(),"plain-text",false);
  for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   var model=new ExtendedModelMap();model.addAttribute("customListNames",names);model.addAttribute("customListsAdmin",true);
   model.addAttribute("setup",new CustomListsService.View(names,List.of(match),List.of(new CustomListsService.Line(1,"projects","My projects","my-prefix-projects","missing",List.of())),"ready",true));
   String html=render("customlists/cherrytemplatetasks.html",model,locale);
   assertThat(html).contains("My &lt;lists&gt;","my-prefix-projects","/cherrytemplatetasks/create","name=\"_tenantView\"","data-confirm-message").doesNotContain("??customsetup.");
   model.addAttribute("customListsAdmin",false);assertThat(render("customlists/cherrytemplatetasks.html",model,locale)).doesNotContain("/cherrytemplatetasks/create");
   for(String file:List.of("cherrytemplatetasks","cherrytemplatenode"))assertThat(render("help/"+file+".html",model,locale)).contains("my-prefix").doesNotContain("??customsetup.");
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

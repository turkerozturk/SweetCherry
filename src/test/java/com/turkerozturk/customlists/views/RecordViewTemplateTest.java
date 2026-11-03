package com.turkerozturk.customlists.views;
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
class RecordViewTemplateTest {
 @Test void catalogAndSandboxedFrameRenderInBothLanguages()throws Exception {
  for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   var catalog=new RecordViewService.Catalog(1,"<template>","ready",List.of(new RecordViewService.Choice(10,10,"_views",true)),List.of(new RecordViewService.Choice(11,11,"Design",true)),List.of(new RecordViewService.RecordChoice(2,"Name")),true);
   var model=new ExtendedModelMap();model.addAttribute("layouts",catalog);model.addAttribute("layoutAdmin",true);model.addAttribute("selectedRecord",2L);
   var html=render("customlists/record-views.html",model,locale);assertThat(html).contains("&lt;template&gt;","/cherrytemplatenode/1/views/example","name=\"_tenantView\"","data-confirm-message","/nodes/content/11").doesNotContain("??recordviews.");
   var example=new RecordViewService.Catalog(1,"template","ready",catalog.folders(),List.of(new RecordViewService.Choice(11,11,"Example layout",true)),catalog.records(),true);model.addAttribute("layouts",example);assertThat(render("customlists/record-views.html",model,locale)).doesNotContain("/cherrytemplatenode/1/views/example");
   var page=new RecordViewService.Page(catalog,catalog.views().get(0),RecordViewRendererTest.record(),new RecordViewRenderer.Output("<h1>&lt;safe&gt;</h1>",List.of("<notice>")));model.addAttribute("recordView",page);
   html=render("customlists/record-view.html",model,locale);var frame=org.jsoup.Jsoup.parse(html).selectFirst("iframe");assertThat(frame.attr("sandbox")).isEqualTo("allow-popups allow-popups-to-escape-sandbox");assertThat(frame.attr("sandbox")).doesNotContain("allow-scripts","allow-same-origin","allow-forms");assertThat(frame.attr("srcdoc")).isEqualTo("<h1>&lt;safe&gt;</h1>");assertThat(html).contains("&lt;notice&gt;").doesNotContain("??recordviews.");
   model.addAttribute("layoutAdmin",false);assertThat(render("customlists/record-view.html",model,locale)).doesNotContain("/nodes/content/11");
  }
 }
 private static String render(String file,Map<String,Object> model,Locale locale)throws Exception {
  var engine=new SpringTemplateEngine();var fragments=new ClassLoaderTemplateResolver();fragments.setPrefix("templates/");fragments.setSuffix(".html");fragments.setResolvablePatterns(Set.of("common/*","customlists/*","help/*"));fragments.setOrder(1);fragments.setCheckExistence(true);engine.addTemplateResolver(fragments);
  var strings=new StringTemplateResolver();strings.setOrder(2);strings.setTemplateMode("HTML");engine.addTemplateResolver(strings);
  var messages=new StaticMessageSource();var properties=new Properties();try(var in=Files.newBufferedReader(Path.of("src/main/resources/messages_"+locale.getLanguage()+".properties"))){properties.load(in);}
  properties.forEach((key,value)->messages.addMessage(key.toString(),locale,value.toString()));engine.setTemplateEngineMessageSource(messages);
  var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setRequestURI("/nodes/tags");var response=new MockHttpServletResponse();
  var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale);context.setVariables(model);context.setVariable("tenantViewToken","v");
  return engine.process(Files.readString(Path.of("src/main/resources/templates/"+file)),context);
 }
}

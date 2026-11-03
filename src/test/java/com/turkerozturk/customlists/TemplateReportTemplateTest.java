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
class TemplateReportTemplateTest {
 @Test void bothHtmlLayoutsRenderAllStatusesAndEscapeUserTextInBothLanguages()throws Exception {
  for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   var template=TemplateReportTest.report();template.setName("<template>");template.getProjectNodes().get(0).setName("<record>");
   for(boolean grid:List.of(false,true)) {
    var model=new ExtendedModelMap();model.addAttribute("cherryTemplateNode",template);model.addAttribute("reportGrid",grid);
    String html=render(grid?"cherrytemplate-type-1.html":"cherrytemplatenode.html",model,locale);
    assertThat(html).contains("&lt;template&gt;","&lt;record&gt;","data-state=\"missing\"","data-state=\"empty\"","data-state=\"ambiguous\"","[5]","[6]","data-report-search","data-report-incomplete","data-report-today").doesNotContain("??report.","<record>","<template>");
    assertThat(html.indexOf("Zed")).isLessThan(html.lastIndexOf("Blank"));
   }
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

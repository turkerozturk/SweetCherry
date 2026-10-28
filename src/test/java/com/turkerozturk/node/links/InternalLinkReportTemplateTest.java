package com.turkerozturk.node.links;

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
class InternalLinkReportTemplateTest {
 @Test void rendersTranslatedEscapedRowsMissingTargetsAndBreadcrumbs()throws Exception {
  var source=new InternalLinkReportService.Place(10,"<Source>","ct_home",true,"#3584E4",true,true,"Parent [4] / <Source> [10]");
  var target=new InternalLinkReportService.Place(11,"Target","ct_code",false,"#000000",true,true,"Other [5] / Target [11]");
  var rows=List.of(new InternalLinkReportService.Row(source,"text",0,"<label>","node 11 a b","11",target,"a b","#a%20b","ok"),
   new InternalLinkReportService.Row(source,"object",4,"file.png","node 99","99",null,"","","missingNode"));
  var report=new InternalLinkReportService.Report(rows,2,1,1,1,0,0,2,0,List.of(),new InternalLinkReportService.Options("","all",50,1));
  for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))) {
   String html=render(report,locale);
   assertThat(html).contains("&lt;Source&gt;","&lt;label&gt;","Parent [4] / &lt;Source&gt; [10]","/nodes/11?_tenantView=v#a%20b","[99]","data-label=").doesNotContain("??internalReport", "<Source>","/nodes/99?");
   assertThat(html).contains(locale.getLanguage().equals("tr")?"İç Bağlantılar":"Internal Links");
  }
 }
 @Test void rendersEmptyAndOutOfRangeStates()throws Exception {
  var report=new InternalLinkReportService.Report(List.of(),0,0,0,0,0,0,0,0,List.of(),new InternalLinkReportService.Options("","all",50,3));
  assertThat(render(report,Locale.ENGLISH)).contains("No internal links match", "Return to first page");
 }
 private static String render(InternalLinkReportService.Report report,Locale locale)throws Exception {
  var engine=new SpringTemplateEngine();var fragments=new ClassLoaderTemplateResolver();fragments.setPrefix("templates/");fragments.setSuffix(".html");fragments.setResolvablePatterns(Set.of("node/*"));fragments.setOrder(1);fragments.setCheckExistence(true);engine.addTemplateResolver(fragments);
  var strings=new StringTemplateResolver();strings.setOrder(2);strings.setTemplateMode("HTML");engine.addTemplateResolver(strings);
  var messages=new StaticMessageSource();var properties=new Properties();try(var in=Files.newBufferedReader(Path.of("src/main/resources/messages_"+locale.getLanguage()+".properties"))){properties.load(in);}
  properties.forEach((key,value)->messages.addMessage(key.toString(),locale,value.toString()));engine.setTemplateEngineMessageSource(messages);
  var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setRequestURI("/nodes/internal-links");var response=new MockHttpServletResponse();
  var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale);context.setVariable("report",report);context.setVariable("tenantViewToken","v");
  return engine.process(Files.readString(Path.of("src/main/resources/templates/node/internal-links.html")),context);
 }
}

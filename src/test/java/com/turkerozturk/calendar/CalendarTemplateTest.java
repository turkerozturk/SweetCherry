package com.turkerozturk.calendar;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;
class CalendarTemplateTest {
 @Test void monthAndWeekRenderEscapedTitlesTagsDatesAndProtectedDialogInBothLanguages()throws Exception {
  for(String mode:List.of("month","week"))for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))) {
   var entry=new CalendarService.Entry("11","1","<script>Long title</script>","ct_code",true,"#3584E4",true,false,1793193300L,123,"2026-10-28T14:15","14:15",List.of("one,two"));
   var days=new ArrayList<CalendarService.Day>();LocalDate date=LocalDate.of(2026,10,26);
   for(int i=0;i<(mode.equals("month")?42:7);i++)days.add(new CalendarService.Day(date.plusDays(i),date.plusDays(i).toString(),"Mon",true,i==0,i==0?List.of(entry):List.of()));
   var hours=new ArrayList<CalendarService.Hour>();for(int i=0;i<24;i++){var cells=new ArrayList<List<CalendarService.Entry>>();for(int d=0;d<7;d++)cells.add(i==14&&d==0?List.of(entry):List.of());hours.add(new CalendarService.Hour("14:00",cells));}
   var view=new CalendarService.View(mode,date,date.minusMonths(1),date.plusMonths(1),true,true,"October 2026","Europe/Istanbul",days,hours,1,false,List.of("one,two"),"any");
   String html=render(Map.of("calendar",view,"calendarEditable",true,"calendarTags",List.of("one,two","<tag>"),"invalidCalendarDate",false),locale);
   assertThat(html).contains("calendarCreatedForm","name=\"_tenantView\"","&lt;script&gt;Long title&lt;/script&gt;","&lt;tag&gt;","data-id=\"11\"","data-content-id=\"1\"","Europe/Istanbul").doesNotContain("??calendar.","<script>Long title</script>");
   var document=org.jsoup.Jsoup.parse(html);assertThat(document.select(".cal-entry")).isNotEmpty();assertThat(document.select("#calendarDialog")).hasSize(1);
  }
 }
 private String render(Map<String,Object> model,Locale locale)throws Exception {
  var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);
  var messages=new StaticMessageSource();var properties=new Properties();try(var in=Files.newBufferedReader(Path.of("src/main/resources/messages_"+locale.getLanguage()+".properties"))){properties.load(in);}properties.forEach((key,value)->messages.addMessage(key.toString(),locale,value.toString()));engine.setTemplateEngineMessageSource(messages);
  var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setRequestURI("/nodes/calendar/month");var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);context.setVariables(model);context.setVariable("tenantViewToken","v");return engine.process("calendar/calendar",context);
 }
}

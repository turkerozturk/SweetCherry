package com.turkerozturk.node.reminders;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class ReminderTemplateTest {
 static final String ID="11111111-1111-4111-8111-111111111111";
 private String render(String template,Locale locale,Map<String,Object> variables){
  var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
  var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
  var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var model=new HashMap<String,Object>(variables);model.put("tenantViewToken","token");
  var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale,model);return engine.process(template,context);
 }
 @Test void listEscapesNotesAndTargetNamesAndHidesWriteFormsForReaders(){
  var row=new ReminderService.Row(ID,1,11,"<Node>",true,"<script>alert(1)</script>","2026-11-17T13:00:00Z","Europe/Istanbul","upcoming");
  var view=new ReminderService.View(List.of(row),1,0,0,0,1,"revision","2026-11-17T10:00:00Z",new ReminderService.Options("","pending",50,1));
  for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr")))for(boolean writable:List.of(true,false)){
   String html=render("node/reminders",locale,Map.of("reminders",view,"canEditReminders",writable));var doc=org.jsoup.Jsoup.parse(html);
   assertThat(html).contains("&lt;Node&gt;","&lt;script&gt;","Europe/Istanbul","_tenantView=token").doesNotContain("??");assertThat(doc.select("script")).isEmpty();
   assertThat(doc.select("a[href*=/nodes/11]")).hasSize(1);assertThat(doc.select("form[method=post]")).hasSize(writable?2:0);
   if(writable){assertThat(doc.select("form[method=post] input[name=revision]").eachAttr("value")).containsOnly("revision");assertThat(doc.select("form[method=post] input[name=state]").eachAttr("value")).containsExactly("done","cancelled");}
  }
 }
 @Test void editorPreservesTypedFieldsMissingTargetDisablesFormAndErrorPagesAreLocalized(){
  for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   var editor=new ReminderService.Editor(ID,11,1,"<Node>",false,"2026-11-17T16:00","Europe/Istanbul","<typed>","pending","revision");
   var html=render("node/reminder-edit",locale,Map.of("editor",editor,"reminderError","invalidTime"));var doc=org.jsoup.Jsoup.parse(html);
   assertThat(doc.selectFirst("fieldset").hasAttr("disabled")).isTrue();assertThat(doc.selectFirst("textarea").text()).isEqualTo("<typed>");assertThat(html).doesNotContain("??","<typed>");
   assertThat(render("node/reminder-error",locale,Map.of("reminderError","storeInvalid"))).contains("REMINDERS").doesNotContain("??");
  }
 }
 @Test void emptyAndOutsidePagesOfferReturnNavigationAndManagedNodeHidesJson(){
  var empty=new ReminderService.View(List.of(),0,0,0,0,0,"r","now",new ReminderService.Options("","pending",50,4));
  assertThat(render("node/reminders",Locale.ENGLISH,Map.of("reminders",empty,"canEditReminders",false))).contains("No reminders match","Return to first page");
  var node=new com.turkerozturk.node.Node();node.setSyntax("plain-text");node.setTxt("secret-json");
  assertThat(render("node/nodeContentFragment",Locale.ENGLISH,Map.of("node",node,"reminderHelper",true))).contains("Use the Reminders page").doesNotContain("secret-json");
 }
 @Test void annualFieldsOccurrenceTokensAndAlarmWidgetRenderInBothLanguages(){
  for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   var editor=new ReminderService.Editor(ID,11,1,"Node",true,"2027-02-28T10:00","UTC","Birthday","pending","revision","yearly","mar1","2024-02-29T10:00",ID);
   var form=org.jsoup.Jsoup.parse(render("node/reminder-edit",locale,Map.of("editor",editor)));
   assertThat(form.selectFirst("select[name=repeat] option[selected]").val()).isEqualTo("yearly");assertThat(form.selectFirst("select[name=leapDay] option[selected]").val()).isEqualTo("mar1");
   var row=new ReminderService.Row(ID,1,11,"Node",true,"Birthday","2027-02-28T10:00:00Z","UTC","upcoming","yearly","2026-02-28T10:00:00Z","","",ID);
   var view=new ReminderService.View(List.of(row),1,0,0,0,1,"revision","now",new ReminderService.Options("","pending",50,1));
   String list=render("node/reminders",locale,Map.of("reminders",view,"canEditReminders",true));assertThat(list).contains(locale.getLanguage().equals("tr")?"Bu oluşumu tamamla":"Complete this occurrence").doesNotContain("??");
   assertThat(org.jsoup.Jsoup.parse(list).select("input[name=occurrence]").eachAttr("value")).containsOnly(ID);
   var widget=org.jsoup.Jsoup.parse(render("node/reminder-alarms",locale,Map.of("currentTenantName","Notes")));
   assertThat(widget.selectFirst("#reminderAlarms").attr("data-token")).isEqualTo("token");assertThat(widget.selectFirst("form").attr("action")).isEqualTo("/nodes/reminders/alarms/0");
   assertThat(render("node/reminder-alarms",locale,Map.of())).doesNotContain("id=\"reminderAlarms\"");
  }
 }

}

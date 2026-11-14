package com.turkerozturk.node.audit;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;
class AuditTemplateTest {
 @Test void historyAndBeforeAfterPagesAreLocalizedAndDoNotInterpretOldHtmlAsMarkup(){
  for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);
   var source=new AuditSql.TextState(2,"before","html","<Name>",false,"<script>unsafe()</script>","Old text",List.of(),List.of(),List.of());var event=new AuditSql.Event(1,UUID.randomUUID().toString(),"2026-11-14T12:00:00Z","<admin>","content.edit",List.of(2L),List.of(new AuditSql.Change(2,Map.of("name","<before>"),Map.of("name","<after>"))),List.of(),List.of(),List.of(source),List.of("image:0"));var index=new AuditSql.Index(event.id(),event.time(),event.actor(),event.action(),"Note 2",List.of(2L),true,event,List.of());
   context.setVariable("tenantViewToken","current");context.setVariable("history",new AuditSql.History(99,List.of("2026-11-14"),List.of(index),1));context.setVariable("auditDay","2026-11-14");context.setVariable("auditNode",2L);context.setVariable("auditPage",0);context.setVariable("auditEnabled",false);context.setVariable("event",event);context.setVariable("nodeChanges",event.nodes());context.setVariable("treeChanges",event.tree());context.setVariable("bookmarkChanges",event.bookmarks());context.setVariable("metadataLimited",false);context.setVariable("sources",List.of(new AuditController.SourceView(source,"Old text",source.rawInline(),false,false,"[]",false)));context.setVariable("auditError","audit.invalid");
   for(String template:List.of("node/audit","node/auditEvent","node/auditError")){String html=engine.process(template,context);assertThat(html).doesNotContain("??","<script>unsafe()","<admin>","<before>","<after>");assertThat(html).contains("_tenantView=current");if(template.equals("node/auditEvent")){assertThat(html).contains("&lt;script&gt;unsafe()&lt;/script&gt;");assertThat(org.jsoup.Jsoup.parse(html).select("a[href*=source]")).hasSize(2);}}
  }
 }
}

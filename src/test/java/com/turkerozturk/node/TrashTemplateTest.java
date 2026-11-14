package com.turkerozturk.node;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;
class TrashTemplateTest {
 @Test void recoveryAndDeletePagesAreLocalizedAndEscapeNamesAndMetadata(){
  for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
   var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);
   var e=new TrashSql.Entry(100,2,"<script>bad()</script>","<Parent>",1,"2026-11-14T10:00:00Z","<Admin>",3,2,false,"revision");
   context.setVariable("tenantViewToken","current");context.setVariable("trash",new TrashSql.Listing(99,List.of(e)));context.setVariable("trashEnabled",true);context.setVariable("trashPage",0);context.setVariable("trashPages",1);context.setVariable("trashTotal",1);context.setVariable("entry",e);context.setVariable("trashError","trash.changed");context.setVariable("purgeRevision","payload-revision");
   var node=new Node();node.setName(e.name());context.setVariable("node",node);context.setVariable("sharedNode",false);context.setVariable("selectedNodeId",2);context.setVariable("deleteView","tree");context.setVariable("trashMode",true);context.setVariable("trashPreview",new TrashSql.Preview("revision",e.name(),2,3,2));
   for(String template:List.of("node/trash","node/trashEntry","node/trashError","node/trashPurge","node/nodeDelete")){
    String html=engine.process(template,context);assertThat(html).doesNotContain("??","<script>bad()","<Parent>","<Admin>");
    var doc=org.jsoup.Jsoup.parse(html);assertThat(doc.select("form")).allSatisfy(f->{assertThat(f.attr("method")).isEqualTo("post");assertThat(f.select("input[name=_tenantView]").val()).isEqualTo("current");});
    if(template.equals("node/trashEntry")){assertThat(doc.select("option[value=original]")).isEmpty();assertThat(doc.select("input[name=confirmed]")).isEmpty();assertThat(doc.select("input[name=revision]")).hasSize(1);}
    if(template.equals("node/trashPurge")){assertThat(doc.select("input[name=confirmed][required]")).hasSize(1);assertThat(doc.select("input[name=revision]").val()).isEqualTo("payload-revision");}
    if(template.equals("node/nodeDelete")){assertThat(doc.select("input[name=trashMode]").val()).isEqualTo("true");assertThat(doc.select("input[name=revision]").val()).isEqualTo("revision");}
   }
  }
 }
}

package com.turkerozturk.node.links;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class LinkGraphTemplateTest {
    static String render(String template, Locale locale, Map<String,Object> model) throws Exception {
        var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);
        var messages=new StaticMessageSource();var values=new Properties();
        try(var input=Files.newBufferedReader(Path.of("src/main/resources/messages_"+locale.getLanguage()+".properties"))){values.load(input);}
        values.forEach((key,value)->messages.addMessage(key.toString(),locale,value.toString()));engine.setTemplateEngineMessageSource(messages);
        var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);
        var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);
        context.setVariables(model);return engine.process(template,context);
    }

 @Test void pageHasOfflineScriptsLocalizedControlsAndSandboxedPreview()throws Exception{
  for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   var html=render("node/link-graph",locale,Map.of("graphNodeId","9007199254740993","tenantViewToken","<token>"));
   var doc=org.jsoup.Jsoup.parse(html);assertThat(html).doesNotContain("??graph.","https://unpkg");
   assertThat(doc.selectFirst("#linkGraphPage").attr("data-focus")).isEqualTo("9007199254740993");
   assertThat(doc.selectFirst("#linkGraphPage").attr("data-token")).isEqualTo("<token>");
   assertThat(doc.selectFirst("#graphPreview").attr("sandbox")).doesNotContain("allow-scripts","allow-same-origin");
   assertThat(doc.select("script[src]").eachAttr("src")).containsExactly("/js/thirdparty/cytoscape/cytoscape.min.js","/js/node/linkGraph.js");
   assertThat(doc.selectFirst("#graphHierarchy").hasAttr("checked")).isFalse();
  }
 }
}

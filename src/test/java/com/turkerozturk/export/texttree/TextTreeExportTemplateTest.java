package com.turkerozturk.export.texttree;

import com.turkerozturk.node.Node;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.springframework.context.support.StaticMessageSource;
import static org.assertj.core.api.Assertions.*;
class TextTreeExportTemplateTest {
    @Test void bothLanguagesAndReadersKeepExportsInsideOneMenuWithTenantTokensAndCtbConfirmation()throws Exception{
        for(String language:List.of("en","tr"))for(String view:List.of("tree","reader")){
            var node=new Node();node.setNodeId(11);node.setName("<script>unsafe</script>");node.setSyntax("plain-text");
            Map<String,Object> model=new HashMap<>();model.put("node",node);model.put("requestedNodeId",11L);model.put("view",view);model.put("tenantViewToken","current");model.put("canDeleteNode",false);
            String html=render("node/nodeToolbarFragment",language,model);var doc=org.jsoup.Jsoup.parse(html);var menu=doc.select("[data-node-export-menu]");assertThat(menu).hasSize(1);
            assertThat(menu.select("a[href*=nodes/export/text/11]").attr("href")).contains("_tenantView=current");
            assertThat(menu.select("a[href*=nodes/pdf/11]")).hasSize(2);assertThat(menu.select("a[href*=mindmap-export]")).hasSize(1);
            assertThat(menu.select("form[data-confirm-message]")).hasSize(4);assertThat(menu.select("form input[name=_tenantView]").eachAttr("value")).containsOnly("current");
            assertThat(doc.select("a[href*=markmap-quick-mind-map]")).hasSize(1);assertThat(html).doesNotContain("??toolbar.","??textTree.");
            var page=new HashMap<String,Object>();page.put("nodeId",11);page.put("rootName","<unsafe>");page.put("limits",new TextTreeExportLimits());page.put("includeDescendants",true);page.put("tenantViewToken","current");page.put("textTreeError","textTree.error.limit");
            var output=render("node/text-tree-export",language,page);var form=org.jsoup.Jsoup.parse(output).select("form");
            assertThat(form.attr("action")).isEqualTo("/nodes/export/text/11");assertThat(form.select("input[name=includeDescendants][checked]")).hasSize(1);
            assertThat(output).contains("&lt;unsafe&gt;","myapp.text-tree-export").doesNotContain("??textTree.","<unsafe>");
        }
    }
    private String render(String template,String language,Map<String,Object> model)throws Exception{
        var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
        var messages=new StaticMessageSource();var properties=new Properties();try(var in=getClass().getResourceAsStream("/messages_"+language+".properties")){properties.load(new java.io.InputStreamReader(in,StandardCharsets.UTF_8));}
        var locale=Locale.forLanguageTag(language);properties.forEach((k,v)->messages.addMessage(k.toString(),locale,v.toString()));engine.setMessageSource(messages);
        var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setRequestURI("/tree");var response=new MockHttpServletResponse();return engine.process(template,new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale,model));
    }
}

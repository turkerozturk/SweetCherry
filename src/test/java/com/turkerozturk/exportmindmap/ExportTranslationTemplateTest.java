package com.turkerozturk.exportmindmap;

import java.nio.charset.StandardCharsets;
import java.util.*;
import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class ExportTranslationTemplateTest {
    @Test void metadataUsesTranslatedBooleanAndCategoryAndRetainsRichFlagOnlyInTemplateComment() throws Exception {
        for(String lang:List.of("en","tr")) for(String syntax:List.of("custom-colors","plain-text","python")) for(boolean real:new boolean[]{false,true}) {
            var node=new Node();node.setNodeId(2);node.setName("<unsafe>");node.setSyntax(syntax);node.setMasterNode(real);
            node.parseNodeIconAndReadOnlyFlag();
            String html=render("node/nodeMetaDataFragment",lang,new HashMap<>(Map.of("node",node)));
            var doc=org.jsoup.Jsoup.parse(html);
            assertThat(doc.select("tbody tr").get(0).selectFirst("td").text()).isEqualTo(lang.equals("tr")?(real?"Evet":"Hayır"):(real?"Yes":"No"));
            String category=syntax.equals("custom-colors")?(lang.equals("tr")?"Zengin Metin":"Rich Text"):
                syntax.equals("plain-text")?(lang.equals("tr")?"Düz Metin":"Plain Text"):(lang.equals("tr")?"Kod":"Code");
            assertThat(doc.select("tbody tr").stream().filter(row->row.selectFirst("th").text().equals(lang.equals("tr")?"Sentaks":"Syntax")).findFirst().orElseThrow().selectFirst("td").text()).isEqualTo(category);
            assertThat(doc.select("th").stream().map(org.jsoup.nodes.Element::text)).doesNotContain(lang.equals("tr")?"Zengin Metin Mi":"Is Rich Text");
            assertThat(html).doesNotContain("??node.","${node.isRichText}","<unsafe>");
        }
        try(var in=getClass().getResourceAsStream("/templates/node/nodeMetaDataFragment.html")) {
            String source=new String(in.readAllBytes(),StandardCharsets.UTF_8);
            assertThat(source).contains("<!--/*", "${node.isRichText}", "*/-->");
        }
    }
    @Test void resultStatesAndUncheckedContentOptionRenderInBothLanguages() throws Exception {
        for(String lang:List.of("en","tr")) {
            var model=new HashMap<String,Object>();model.put("exportStatus","success");model.put("exportCount",2);model.put("exportFilename","<unsafe>.ctb");
            String html=render("expo",lang,model);var doc=org.jsoup.Jsoup.parse(html);
            assertThat(doc.text()).contains(lang.equals("tr")?"Aktarılan düğüm sayısı: 2":"Exported node count: 2");
            assertThat(doc.select("unsafe")).isEmpty();
            assertThat(doc.selectFirst("a").attr("href")).isEqualTo("/app/exportedFiles");
            for(String status:List.of("failed","none")) {
                model.put("exportStatus",status);html=render("expo",lang,model);
                assertThat(html).doesNotContain("??export.");
                assertThat(org.jsoup.Jsoup.parse(html).selectFirst("[role=status]").text()).isNotBlank();
            }
            html=render("mindmap-export",lang,new HashMap<>());doc=org.jsoup.Jsoup.parse(html);
            var checkbox=doc.selectFirst("input[name=includeContent]");assertThat(checkbox).isNotNull();assertThat(checkbox.hasAttr("checked")).isFalse();
            assertThat(html).doesNotContain("??mindmap.");
        }
    }
    private String render(String template,String lang,Map<String,Object> model) throws Exception {
        var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var response=new MockHttpServletResponse();
        request.setContextPath("/app");request.setRequestURI("/app/mindmap-export");Locale locale=Locale.forLanguageTag(lang);request.addPreferredLocale(locale);
        try(var context=new StaticWebApplicationContext()) {
            context.setServletContext(servlet);context.refresh();servlet.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,context);
            var properties=new Properties();try(var in=getClass().getResourceAsStream("/messages_"+lang+".properties")){properties.load(new java.io.InputStreamReader(in,StandardCharsets.UTF_8));}
            properties.stringPropertyNames().forEach(key->context.getStaticMessageSource().addMessage(key,locale,properties.getProperty(key)));
            model.put("thymeleafRequestContext",new SpringWebMvcThymeleafRequestContext(new RequestContext(request,response,servlet,model),request));
            var engine=new SpringTemplateEngine();var resolver=new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);engine.setMessageSource(context);
            return engine.process(template,new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale,model));
        }
    }
}

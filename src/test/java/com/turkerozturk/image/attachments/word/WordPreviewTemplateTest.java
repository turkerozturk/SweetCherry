package com.turkerozturk.image.attachments.word;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import com.turkerozturk.image.attachments.AttachmentBrowseService;
import static org.assertj.core.api.Assertions.*;

class WordPreviewTemplateTest {
    @Test void bothLanguagesEscapeDocumentContentAndOfferOnlyReadOnlyControls() {
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))) {
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);
            var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),locale);
            var run=new WordPreviewService.Run("<img src=x onerror=alert(1)>",true,true,true);
            context.setVariable("word",new WordPreviewService.Preview("<file>.docx","<node>",List.of(new WordPreviewService.Block("heading","center",List.of(run),List.of()),new WordPreviewService.Block("table","left",List.of(),List.of(List.of("<script>literal()</script>")))),true,true));
            context.setVariable("nodeId",1L);context.setVariable("offset",3);context.setVariable("tenantViewToken","current");
            String html=engine.process("attachment/wordContent",context);
            assertThat(html).doesNotContain("??","<img src=x","<file>","<script>literal()").contains("&lt;img","&lt;file&gt;.docx","_tenantView=current","/attachments/1/3/download","wp-bold","wp-italic","wp-underline","&lt;script&gt;literal()");
            var dom=org.jsoup.Jsoup.parse(html);assertThat(dom.select("input,textarea,[contenteditable],a[href^=javascript]")).isEmpty();
            context.removeVariable("word");context.setVariable("wordError","encrypted");assertThat(engine.process("attachment/wordContent",context)).doesNotContain("??").contains("role=\"alert\"");
            context.setVariable("attachments",new AttachmentBrowseService.Page(List.of(new AttachmentBrowseService.Item(1,3,"node","report.docx",100,3000)),1,new AttachmentBrowseService.Options(1,"","newest")));
            for(String template:List.of("attachments","attachmentsMobile")) {
                var list=org.jsoup.Jsoup.parse(engine.process(template,context));
                assertThat(list.select("a[href*='/attachments/1/3/word']")).hasSize(1);
                assertThat(list.select("a[href*='/attachments/1/3/preview']")).hasSize(1);
                assertThat(list.select("a[href*='/attachments/1/3/download']")).hasSize(1);
            }
        }
    }
    @Test void legacyRendererAddsLocalizedWordPreviewAndKeepsDownload() throws Exception {
        try {
            for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))) {
                org.springframework.context.i18n.LocaleContextHolder.setLocale(locale);
                var image=new com.turkerozturk.image.Image();image.setNodeId(53L);image.setOffset(2);image.setPng(new byte[1]);image.setFileName("<file>.docx");
                var map=new com.turkerozturk.yenixmlparser.ExampleMap<Object>();map.put(2L,new com.turkerozturk.image.Attachment(image));
                String html=com.turkerozturk.yenixmlparser.YeniXMLTransformer.parse(map,53L);
                assertThat(html).contains("/download/53/2","/attachments/53/2/word","/attachments/53/2/preview","&lt;file&gt;.docx",WordAttachmentLinks.label()).doesNotContain("<file>");
            }
        } finally { org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext(); }
    }
}

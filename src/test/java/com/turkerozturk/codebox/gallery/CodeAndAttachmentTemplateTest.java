package com.turkerozturk.codebox.gallery;
import java.util.*;
import com.turkerozturk.image.attachments.AttachmentBrowseService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;
class CodeAndAttachmentTemplateTest {
    @Test void bothLanguagesEscapeCodeAndNamesAndPreserveFiltersAndSourceTokens(){
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),locale);
            var item=new CodeGalleryService.Item(1,0,"<node>",100,"javascript",40,"<script>literal()</script>","javascript",true);
            context.setVariable("gallery",new CodeGalleryService.Page(List.of(item),13,12,List.of("javascript"),new CodeGalleryService.Options(1,"<query>","javascript","newest")));context.setVariable("codeSettings",CodeGalleryServiceTest.settings());context.setVariable("tenantViewToken","current");context.setVariable("syntaxHighlightingEnabled",true);
            String html=engine.process("gallery/codeboxes",context);assertThat(html).doesNotContain("??","<script>literal()","<node>").contains("&lt;script&gt;literal()","_tenantView=current","--cg-font:14px");
            context.setVariable("attachments",new AttachmentBrowseService.Page(List.of(new AttachmentBrowseService.Item(1,0,"<node>","<file>.pdf",100,4000)),51,new AttachmentBrowseService.Options(1,"report","size-desc")));context.setVariable("viewMode","mobile");
            for(String template:List.of("attachments","attachmentsMobile")){String rendered=engine.process(template,context);assertThat(rendered).doesNotContain("??","<file>","<node>").contains("&lt;file&gt;.pdf","_tenantView=current","sort=size-desc","q=report");assertThat(org.jsoup.Jsoup.parse(rendered).select("a[href*='/attachments/1/0/download']")).hasSize(1);}
        }
    }
}

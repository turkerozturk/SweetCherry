package com.turkerozturk.image.attachments.preview;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class FilePreviewTemplateTest {
    @Test void bothLanguagesEscapeEveryViewAndPreserveTokensAndHexPagination()throws Exception{
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
            var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),locale);
            context.setVariable("nodeId",1L);context.setVariable("offset",3);context.setVariable("tenantViewToken","current");context.setVariable("encodings",FilePreviewService.ENCODINGS);
            for(String mode:List.of("text","hex","archive")){
                var view=new FilePreviewService.Preview("<file>.txt","<node>",300,mode,"UTF-8","xml","<script>literal()</script>",List.of(new FilePreviewService.HexRow("00000000","3C 3E","<>")),List.of(new FilePreviewService.Entry("../<img src=x>","symlink",1,1,"date","FF","8","<script>note</script>","../../target",true)),true,true,true,2,3,128,128);
                context.setVariable("file",view);String html=engine.process("attachment/fileContent",context);var doc=org.jsoup.Jsoup.parse(html);
                assertThat(html).doesNotContain("??","<file>","<node>","<script>literal()","<img src=x>","<script>note").contains("&lt;file&gt;","_tenantView=current");
                assertThat(doc.select("script,iframe,[contenteditable],form[method=post],a[href^=javascript]")).isEmpty();
                assertThat(doc.selectFirst("form").attr("method")).isEqualTo("get");
                if(mode.equals("text"))assertThat(doc.selectFirst("code").text()).isEqualTo("<script>literal()</script>");
                if(mode.equals("hex"))assertThat(doc.select("nav a").eachAttr("href")).allSatisfy(href->assertThat(href).contains("mode=hex","encoding=UTF-8","_tenantView=current"));
                if(mode.equals("archive"))assertThat(doc.select("tbody a")).isEmpty();
            }
            context.removeVariable("file");context.setVariable("fileError","invalid");var error=engine.process("attachment/fileContent",context);assertThat(error).doesNotContain("??").contains("role=\"alert\"","mode=hex","mode=text");
        }
    }
}

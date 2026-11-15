package com.turkerozturk.grid.gallery;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;
class TableGalleryTemplateTest {
    @Test void localizesMetadataAndUsesOnlyBoundedNumericCssAndProtectedDataLinks(){
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);
            var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),locale);
            var item=new TableGalleryService.Item(1,0,"<script>unsafe()</script>",100,40,400,1000);
            context.setVariable("gallery",new TableGalleryService.Page(List.of(item),14,12,new TableGalleryService.Options(1,"<query>","newest")));
            context.setVariable("tableSettings",TableGalleryServiceTest.settings());context.setVariable("tenantViewToken","current");
            String html=engine.process("gallery/tables",context);assertThat(html).doesNotContain("??","<script>unsafe()");assertThat(html).contains("&lt;script&gt;unsafe()","--tg-width:160px","_tenantView=current");
            var doc=org.jsoup.Jsoup.parse(html);assertThat(doc.select(".table-card")).hasSize(1);assertThat(doc.select("[data-full]").attr("data-full")).contains("full=true","_tenantView=current");
            assertThat(doc.select("[data-preview]").attr("data-preview")).contains("_tenantView=current");
        }
    }
}

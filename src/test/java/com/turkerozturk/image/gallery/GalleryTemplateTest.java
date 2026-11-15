package com.turkerozturk.image.gallery;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class GalleryTemplateTest {
    @Test void galleryEscapesMetadataAndLocalizesControlsWhileProtectingEveryImageRequest(){
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);
            var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),locale);
            var item=new GalleryService.Item(1,0,"<img onerror=evil>",100,"<file>.png","javascript:evil()",0,1000);
            context.setVariable("gallery",new GalleryService.Page(List.of(item),40,new GalleryService.Options(1,"<query>","newest")));context.setVariable("tenantViewToken","current");
            String html=engine.process("gallery/gallery",context);assertThat(html).doesNotContain("??","<img onerror=evil>","href=\"javascript:");
            assertThat(html).contains("&lt;file&gt;.png","&lt;img onerror=evil&gt;","javascript:evil()","_tenantView=current");
            var doc=org.jsoup.Jsoup.parse(html);assertThat(doc.select(".gallery-card")).hasSize(1);assertThat(doc.select("img[src] ")).isEmpty();
            assertThat(doc.select("[data-thumbnail]").attr("data-thumbnail")).contains("_tenantView=current");
            assertThat(doc.select("[data-large]").attr("data-large")).contains("large=true","_tenantView=current");
            assertThat(doc.select("#gallery-viewer")).hasSize(1);
        }
    }
}

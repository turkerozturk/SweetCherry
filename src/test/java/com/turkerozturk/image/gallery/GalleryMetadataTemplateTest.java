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

class GalleryMetadataTemplateTest {
 @Test void metadataFieldsAreLazyAndLocalizedInBothLanguages(){
  for(String language:List.of("en","tr")){
   var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
   var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
   var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),Locale.forLanguageTag(language));
   context.setVariable("tenantViewToken","current");context.setVariable("galleryCanAnnotate",false);
   context.setVariable("gallery",new ImageAnnotationTemplateTest.Gallery(List.of(new ImageAnnotationTemplateTest.Tile(42,7,"Photo","photo.png","",0,100)),1,new ImageAnnotationTemplateTest.Options(1,"","newest",""),List.of()));
   String html=engine.process("gallery/gallery",context);var doc=org.jsoup.Jsoup.parse(html);assertThat(html).doesNotContain("??gallery.");
   assertThat(doc.select(".gallery-info [data-image-info]").eachAttr("data-image-info")).containsExactly("dimensions","megapixels","format","depth","alpha");
   assertThat(doc.select(".gallery-info [data-image-info]").eachText()).containsOnly("—");assertThat(doc.select("img[src]")).isEmpty();
   assertThat(doc.selectFirst("#image-gallery").attr("data-yes")).isEqualTo(language.equals("tr")?"Evet":"Yes");
  }
 }
}

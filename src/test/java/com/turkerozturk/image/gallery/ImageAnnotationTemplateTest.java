package com.turkerozturk.image.gallery;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class ImageAnnotationTemplateTest {
 public record Tile(long nodeId,int offset,String name,String filename,String link,long imageTime,long bytes){public String id(){return nodeId+":"+offset;}public String createdText(){return "—";}public String imageTimeText(){return "—";}}
 public record Options(int page,String search,String sort,String tag){}
 public record Gallery(List<Tile> items,long total,Options options,List<String> tags){public long pages(){return 1;}}
 @Test void galleryShowsTheEntryOnlyWhenTheConnectionIsWritableAndTheFilenameIsSupported(){
  var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
  var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
  var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),Locale.ENGLISH);
  context.setVariable("tenantViewToken","current");context.setVariable("gallery",new Gallery(List.of(new Tile(42,7,"Screenshot","test.png","",0,100)),1,new Options(1,"","newest",""),List.of()));
  context.setVariable("galleryCanAnnotate",false);assertThat(org.jsoup.Jsoup.parse(engine.process("gallery/gallery",context)).select("a[href*='/annotate']")).isEmpty();
  context.setVariable("galleryCanAnnotate",true);var doc=org.jsoup.Jsoup.parse(engine.process("gallery/gallery",context));assertThat(doc.select("a[href*='/annotate']")).hasSize(1);assertThat(doc.selectFirst("a[href*='/annotate']").attr("href")).contains("/42/7/annotate","_tenantView=current");
  context.setVariable("gallery",new Gallery(List.of(new Tile(42,7,"Photo","test.jpg","",0,100)),1,new Options(1,"","newest",""),List.of()));assertThat(org.jsoup.Jsoup.parse(engine.process("gallery/gallery",context)).select("a[href*='/annotate']")).hasSize(1);
 }
 @Test void originalDownloadPreviewAndExplicitOverwriteRenderInBothLanguagesAndContextPaths(){
  for(String language:List.of("en","tr"))for(String path:List.of("","/app")){
   var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
   var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
   var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setContextPath(path);request.setRequestURI(path+"/image-gallery/42/7/annotate");
   var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),Locale.forLanguageTag(language));
   context.setVariable("annotationNode",42L);context.setVariable("annotationOffset",7);context.setVariable("tenantViewToken","current");context.setVariable("_csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","token"));
   var html=engine.process("gallery/annotate",context);var doc=org.jsoup.Jsoup.parse(html);
   assertThat(html).doesNotContain("??annotation.");assertThat(doc.selectFirst("#image-annotation").attr("data-endpoint")).isEqualTo(path+"/image-gallery/42/7/annotation");
   assertThat(doc.selectFirst("fieldset").hasAttr("disabled")).isTrue();assertThat(doc.select("select[data-annotation-tool] option").eachAttr("value")).containsExactly("arrow","rect","ellipse","line","pen");
   assertThat(doc.selectFirst("form[data-annotation-csrf]").attr("method")).isEqualTo("post");assertThat(doc.select("dialog [data-annotation-save]")).hasSize(1);
   assertThat(doc.selectFirst("a[download]").attr("href")).contains("/42/7/original","_tenantView=current");
   assertThat(doc.select("canvas")).hasSize(1);assertThat(doc.select("img[src]")).isEmpty();
  }
 }
}

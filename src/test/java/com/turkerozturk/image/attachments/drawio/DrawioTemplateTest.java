package com.turkerozturk.image.attachments.drawio;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class DrawioTemplateTest {
 @Test void localizedControlsUseContextPathsAndOpaqueSandboxWithoutInlineDiagramMarkup(){
  for(String lang:List.of("en","tr"))for(String path:List.of("","/app")){
   var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
   var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
   var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setContextPath(path);request.setRequestURI(path+"/attachments/42/7/drawio");
   var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),Locale.forLanguageTag(lang));context.setVariable("nodeId",42L);context.setVariable("offset",7);context.setVariable("tenantViewToken","current");context.setVariable("drawioNonce","random-nonce");context.setVariable("drawioLibraries","basic,cabinets,eip");context.setVariable("drawioIconScripts",List.of("/js/drawio/icons/clip_art.js"));
   String html=engine.process("attachment/drawioContent",context);var doc=org.jsoup.Jsoup.parse(html);assertThat(html).doesNotContain("??drawio.","<mxGraphModel","allow-same-origin");
   assertThat(doc.selectFirst("iframe").attr("sandbox")).isEqualTo("allow-scripts");assertThat(doc.selectFirst("iframe").attr("data-src")).isEqualTo(path+"/attachments/drawio/frame?_tenantView=current");
   assertThat(doc.selectFirst("#drawio-preview").attr("data-endpoint")).contains(path+"/attachments/42/7/drawio/data?_tenantView=current");
   assertThat(doc.select("[data-drawio-command]")).hasSize(5);assertThat(doc.select("select[data-drawio-pages][disabled]")).hasSize(1);assertThat(doc.select("[data-drawio-fullscreen]")).hasSize(1);
   var frame=org.jsoup.Jsoup.parse(engine.process("attachment/drawioFrame",context));assertThat(frame.select("script")).hasSize(7);assertThat(frame.select("script").eachAttr("nonce")).containsOnly("random-nonce");assertThat(frame.select("script").eachAttr("src")).allMatch(src->src.startsWith(path+"/js/drawio/"));
  }
 }
}

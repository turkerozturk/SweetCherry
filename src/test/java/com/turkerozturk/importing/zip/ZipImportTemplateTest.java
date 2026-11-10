package com.turkerozturk.importing.zip;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.*;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.util.*;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
class ZipImportTemplateTest {
 @TempDir Path directory;
 @Test void bothWizardStepsRenderLocalizedAndEscapeArchiveNames()throws Exception {
  var archive=directory.resolve("example.zip");
  try(var out=new java.util.zip.ZipOutputStream(Files.newOutputStream(archive))){out.putNextEntry(new java.util.zip.ZipEntry("<b>name</b>.md".replace("/","_")));out.write("# Example".getBytes(java.nio.charset.StandardCharsets.UTF_8));out.closeEntry();}
  var data=ZipImportData.read(archive,ZipImportTest.ALL,100);
  for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
   var model=new HashMap<String,Object>();model.put("zipUploadBytes",1000000);model.put("zipAttachmentBytes",100);model.put("currentTenantName","Demo");
   String html=render("zip-import.html",model,locale);assertThat(html).contains("/zip-import/preview","name=\"charset\"","name=\"attachments\"").doesNotContain("??zipImport.");
   model.put("preview",data);model.put("token","preview-token");html=render("zip-import.html",model,locale);
   assertThat(html).contains("/zip-import/confirm","/zip-import/cancel","preview-token","&lt;b&gt;name&lt;_b&gt;.md","markdown").doesNotContain("??zipImport.","<b>name");
  }
 }
 private static String render(String file,Map<String,Object> model,Locale locale)throws Exception {
  var engine=new SpringTemplateEngine();var fragments=new ClassLoaderTemplateResolver();fragments.setPrefix("templates/");fragments.setSuffix(".html");fragments.setResolvablePatterns(Set.of("common/*"));fragments.setOrder(1);fragments.setCheckExistence(true);engine.addTemplateResolver(fragments);
  var strings=new StringTemplateResolver();strings.setOrder(2);strings.setTemplateMode("HTML");engine.addTemplateResolver(strings);
  var messages=new StaticMessageSource();var properties=new Properties();try(var in=Files.newBufferedReader(Path.of("src/main/resources/messages_"+locale.getLanguage()+".properties"))){properties.load(in);}
  properties.forEach((key,value)->messages.addMessage(key.toString(),locale,value.toString()));engine.setTemplateEngineMessageSource(messages);
  var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setRequestURI("/nodes/tags");var response=new MockHttpServletResponse();
  var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale);context.setVariables(model);context.setVariable("tenantViewToken","v");
  return engine.process(Files.readString(Path.of("src/main/resources/templates/"+file)),context);
 }
}

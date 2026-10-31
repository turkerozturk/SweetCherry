package com.turkerozturk.bookmark;

import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.*;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.nio.file.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;

class BookmarkDatesTemplateTest {
 @Test void bothViewsShowNodeCreationSeparatelyFromModificationInBothLanguages()throws Exception{
  var node=new Node();node.setNodeId(2);node.setName("<Shared master>");node.setSyntax("plain-text");node.setCreationTimestamp(1700000000);node.setLastSaveTimestamp(1800000000);node.parseNodeIconAndReadOnlyFlag();node.parseNodeTitleColorAndBoldnessAndTextType();
  var bookmark=new Bookmark().setNodeId(11);bookmark.setNode(node);
  var model=new HashMap<String,Object>();model.put("bookmarks",List.of(bookmark));model.put("canWriteBookmarks",false);model.put("bookmarkPaths",Map.of());model.put("missingBookmarkIds",List.of());model.put("tenantViewToken","v");
  String created=new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date(node.getCreationTimestamp()*1000));
  for(String template:List.of("bookmarks","bookmarksMobile"))for(String lang:List.of("en","tr")){
   var locale=Locale.forLanguageTag(lang);var engine=new SpringTemplateEngine();var strings=new StringTemplateResolver();strings.setTemplateMode("HTML");engine.setTemplateResolver(strings);
   var messages=new StaticMessageSource();var properties=new Properties();try(var in=Files.newBufferedReader(Path.of("src/main/resources/messages_"+lang+".properties"))){properties.load(in);}properties.forEach((k,v)->messages.addMessage(k.toString(),locale,v.toString()));engine.setTemplateEngineMessageSource(messages);
   var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var response=new MockHttpServletResponse();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale);context.setVariables(model);
   String html=engine.process(Files.readString(Path.of("src/main/resources/templates/"+template+".html")),context);var doc=org.jsoup.Jsoup.parse(html);
   assertThat(doc.selectFirst(".bookmark-created-date").text()).isEqualTo(created);assertThat(doc.selectFirst(".bookmark-created-date").attr("title")).startsWith("1700000000, ");
   assertThat(html).contains(lang.equals("tr")?"Oluşturulma":"Created").doesNotContain("??bookmarks.","<Shared master>");assertThat(doc.selectFirst("a[href^=/nodes/]").attr("href")).contains("/nodes/11");
   assertThat(doc.select("thead th")).hasSize(template.equals("bookmarks")?4:2);
  }
  assertThat(node.getCreationTimestamp()).isEqualTo(1700000000);assertThat(node.getLastSaveTimestamp()).isEqualTo(1800000000);
 }
}

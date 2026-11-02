package com.turkerozturk.richtext.editing;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class RichTextCodeboxesTest {
 @Test void languagesStartWithPlainTextThenSortedAndHaveBundledIcons() {
   var languages=RichTextCodeboxes.languages();assertThat(languages.get(0).syntax()).isEqualTo("plain-text");
   assertThat(languages.subList(1,languages.size()).stream().map(RichTextCodeboxes.Language::syntax).toList()).isSorted().doesNotHaveDuplicates();
   for(var language:languages) {
     String path="static/img/ctbicons/"+language.icon()+".png";
     var resource=new org.springframework.core.io.ClassPathResource(path,RichTextCodeboxes.class.getClassLoader());
     assertThat(resource.exists()).as("Bundled codebox icon: syntax=%s, path=%s",language.syntax(),path).isTrue();
   }
 }
 @Test void missingOptionalLanguageIconUsesTheBundledGenericCodeIcon() {
   assertThat(RichTextCodeboxes.iconFor("java",icon->icon.equals("ct_java"))).isEqualTo("ct_java");
   assertThat(RichTextCodeboxes.iconFor("java",icon->icon.equals("ct_code"))).isEqualTo("ct_code");
   assertThat(RichTextCodeboxes.iconFor("unknown",icon->icon.equals("ct_code"))).isEqualTo("ct_code");
 }
 @Test void rejectsOversizedMalformedAndReservedPayloads() {
   for(String json:java.util.List.of("[]","{\"grid:1\":{\"text\":\"x\",\"syntax\":\"java\"}}","{\"new-codebox:x\":{\"text\":5,\"syntax\":\"java\"}}",
      "{\"new-codebox:x\":{\"text\":\""+"x".repeat(200001)+"\",\"syntax\":\"java\"}}"))
       assertThatThrownBy(()->RichTextCodeboxes.decode(json)).isInstanceOf(IllegalArgumentException.class);
 }
}

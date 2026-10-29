package com.turkerozturk.markdown;
import com.turkerozturk.node.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class MarkdownRenderingTest {
 @Test void rendersHeadingsTablesListsLinksAndUnicodeWithoutChangingSource(){
  String source="# Başlık\n\n**Kalın** _italic_ ~~old~~\n\n| A | B |\n|---|---|\n| çığ | öşü |\n\n- [x] Done\n- [ ] Next\n\n```markdown\n# code stays code\n```\n\n[Link](https://example.org)\n\n[Jump](#başlık)";
  String html=NodeMarkup.render("markdown",source);var doc=Jsoup.parseBodyFragment(html);
  assertThat(doc.select("h1").text()).isEqualTo("Başlık");assertThat(doc.select("strong").text()).isEqualTo("Kalın");assertThat(doc.select("table td").text()).contains("çığ","öşü");assertThat(doc.select("input[disabled]")).hasSize(2);assertThat(doc.select("input[checked]")).hasSize(1);assertThat(doc.select("pre code").text()).isEqualTo("# code stays code");
  assertThat(doc.select("a[href^=#]").attr("href")).startsWith("#md-");assertThat(doc.select("h1").attr("id")).startsWith("md-");
  var node=new Node();node.setSyntax("markdown");node.setTxt(source);new NodeContentParserService().parseNodeContent(node,null);assertThat(node.getTxt()).isEqualTo(source);assertThat(node.getTxtAsHtml()).contains("<table>");
 }
 @Test void sanitizesRawHtmlUnsafeLinksAndImagesAndKeepsSafeRelativeUrls(){
  String html=NodeMarkup.render("markdown","<script>alert(1)</script>\n\n<img src=x onerror=alert(1)>\n\n[bad](javascript:alert(1)) [data](data:text/html,evil) [internal](/nodes/12) [mail](mailto:a@example.org)\n\n![bad](data:image/svg+xml;base64,PHN2Zz4=)\n\n![safe](https://example.org/p.png)","v");var doc=Jsoup.parseBodyFragment(html);
  assertThat(doc.select("script,iframe,object,[onclick],[onerror]")).isEmpty();assertThat(doc.select("a[href^=javascript],a[href^=data],img[src^=data:image/svg]")).isEmpty();assertThat(doc.select("a[href^=/nodes/12]").attr("href")).isEqualTo("/nodes/12?_tenantView=v");assertThat(doc.select("a[href^=mailto]")).hasSize(1);assertThat(doc.select("img[src^=https]")).hasSize(1);
 }
 @Test void plainFallbackAndCodeboxesDoNotUseNodeMarkupAndHugeSourcesAreRejected(){
  assertThat(NodeMarkup.supports("plain-text")).isFalse();assertThat(NodeMarkup.supports("custom-colors")).isFalse();assertThat(NodeMarkup.supports("java")).isFalse();assertThat(NodeMarkup.supports(null)).isFalse();
  assertThatThrownBy(()->NodeMarkup.render("markdown","x".repeat(MarkdownRenderer.MAX_CHARACTERS+1))).hasMessageContaining("413");
 }
 @Test void pdfBookmarksAndTextOnlyNotesUseRenderedMarkdown(){
  String source="# Heading\n\n[Site](https://example.org)\n\n- [x] Done\n\n![Picture](https://example.org/p.png)";
  var html=Jsoup.parseBodyFragment(NodeMarkup.forPdf("markdown",source));assertThat(html.select("h1[data-pdf-heading=h1]")).hasSize(1);assertThat(html.select("input")).isEmpty();
  String notes=NodeMarkup.text("markdown",source);assertThat(notes).contains("Heading","Site (https://example.org)","[x] Done","Image: Picture").doesNotContain("# Heading","[Site](");
 }
}

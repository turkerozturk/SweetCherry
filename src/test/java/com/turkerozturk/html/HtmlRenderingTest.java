package com.turkerozturk.html;

import com.turkerozturk.markdown.NodeMarkup;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class HtmlRenderingTest {
 @Test void rendersFragmentsAndFullDocumentsWithoutChangingSource(){
  String source="<!doctype html><html><head><title>Ignored</title></head><body><h1>Title</h1><p style='color:red;text-align:center'>Hello <b>world</b></p><table><tr><td>A</td></tr></table></body></html>";
  String html=NodeMarkup.render("html",source);
  assertThat(html).contains("<h1>Title</h1>","color:red;","text-align:center;","<b>world</b>","<td>A</td>").doesNotContain("<title>","<html>");
  assertThat(source).contains("<!doctype html>","style='color:red;text-align:center'");
  var node=new com.turkerozturk.node.Node();node.setSyntax("html");node.setTxt(source);
  new com.turkerozturk.node.NodeContentParserService().parseNodeContent(node,null);
  assertThat(node.getTxt()).isEqualTo(source);assertThat(node.getTxtAsHtml()).contains("<iframe", "srcdoc=");
  assertThat(NodeMarkup.text("html",source)).contains("Title","Hello world","A").doesNotContain("<h1>","Ignored");
  assertThat(NodeMarkup.forPdf("html",source)).contains("data-pdf-heading=\"h1\"").doesNotContain("<iframe");
 }
 @Test void hostileMarkupCannotRunOrFetchResources(){
  String html=NodeMarkup.render("html","<script>alert(1)</script><style>@import 'https://tracker';</style><iframe src='https://tracker'></iframe><form><input name='password'></form><svg onload='alert(1)'></svg><p onclick='x()' style='color:red;position:fixed;background-image:url(https://tracker);width:expression(x());font-size:20px'>Safe</p><a href='javascript:alert(1)'>bad</a><a href='//tracker'>relative</a><img src='https://tracker' onerror='x()'><img src='data:image/svg+xml;base64,AAAA'>");
  assertThat(html).contains("Safe","color:red;","font-size:20px;").doesNotContain("<script","<style","<iframe","<form","<svg","onclick","onerror","javascript:","https://tracker","//tracker","position:","url(","expression(","image/svg");
 }
 @Test void linksImagesAndBrokenHtmlHavePredictableFallbacks(){
  String html=NodeMarkup.render("html","<h2 id='intro'>Intro</h2><a href='#intro'>jump</a><a href='/nodes/7'>node</a><a href='https://example.org/a'>web</a><a href='https://user:pass@example.org/'>credentials</a><img src='data:image/png;base64,AAAA' alt='sample'><p><b>unfinished","token");
  var doc=Jsoup.parseBodyFragment(html);
  assertThat(doc.selectFirst("h2").id()).isEqualTo("html-0");
  assertThat(doc.selectFirst("a").attr("href")).isEqualTo("#html-0");
  assertThat(html).contains("/nodes/7?_tenantView=token","target=\"_blank\"","noopener noreferrer","data:image/png;base64,AAAA","unfinished</b></p>").doesNotContain("user:pass");
 }
 @Test void readerUsesIsolatedFrameButExportsKeepCleanFragments(){
  var frame=Jsoup.parseBodyFragment(NodeMarkup.view("html","<h1>Reader</h1>","v")).selectFirst("iframe");
  assertThat(frame.attr("sandbox")).isEqualTo("allow-popups allow-popups-to-escape-sandbox");
  assertThat(frame.attr("srcdoc")).contains("Content-Security-Policy",HtmlNodeRenderer.CSP,"<h1>Reader</h1>");
  assertThat(NodeMarkup.view("markdown","# Reader","v")).doesNotContain("<iframe");
 }
 @Test void boundsSourceBeforeParsing(){
  assertThatThrownBy(()->NodeMarkup.render("html","x".repeat(HtmlNodeRenderer.MAX_CHARACTERS+1))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("413");
 }
}

package com.turkerozturk.customlists.views;

import com.turkerozturk.pdf.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RecordViewPdfTest {
 private final RecordViewPdf service=new RecordViewPdf(new PdfExportLimits(),new NodePdfRenderer());
 @Test void unicodeTablesImagesExternalLinksAndMultiplePages()throws Exception {
  var image=new BufferedImage(80,40,BufferedImage.TYPE_INT_RGB);var output=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"jpeg",output);
  String html="<style>h1{color:#124578}td{width:50%;padding:5px}body{background-image:url(http://127.0.0.1:9/a)}</style><h1>Çığ öşü Türkçe</h1><table><tr><td>İsim</td><td>Değer</td></tr></table><img src='data:image/jpeg;base64,"+Base64.getEncoder().encodeToString(output.toByteArray())+"'><a href='https://example.org'>Kaynak</a><a href='/nodes/12?_tenantView=secret'>İç bağlantı</a>"+"<p>Satır içeriği</p>".repeat(140);
  var document=service.prepare(html,"Kayıt",12);
  byte[] bytes=new NodePdfRenderer().render(document);
  java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"),"record-layout-qa.pdf"),bytes);
  try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)) {
   assertThat(pdf.getNumberOfPages()).isGreaterThan(1);
   String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);assertThat(text).contains("Çığ öşü Türkçe","İsim","Değer","Kaynak","İç bağlantı","1 / ");
   assertThat(pdf.getPage(0).getMediaBox().getWidth()).isBetween(594f,596f);
   assertThat(pdf.getPage(0).getResources().getXObjectNames()).isNotEmpty();
   assertThat(pdf.getPage(0).getAnnotations()).anyMatch(a->a instanceof org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink link && link.getAction() instanceof org.apache.pdfbox.pdmodel.interactive.action.PDActionURI uri && uri.getURI().equals("https://example.org"));
   assertThat(pdf.getDocumentInformation().getCreator()).isNull();
  }
 }
 @Test void excludesResourceCssAndAllowsSafeLayoutStyles() {
  assertThat(RecordViewPdf.stylesheet("@import 'file:/secret'; div{background-image:url(file:/secret);width:50%;color:red} p{font-family:evil;position:fixed;padding:4px}"))
   .doesNotContain("file:","url(","font-family","position").contains("padding:4px");
  var doc=service.prepare("<script>bad()</script><iframe src='file:/secret'></iframe><img src='file:/secret'><p style='color:red;position:fixed'>safe</p>","Test",1);
  assertThat(doc.getElementsByTagName("script").getLength()).isZero();assertThat(doc.getElementsByTagName("iframe").getLength()).isZero();
  assertThat(doc.getElementsByTagName("img").getLength()).isZero();
 }
 @Test void expandedFieldsAndTimeArePrintedWithoutReevaluatingNoteText()throws Exception {
  var output=new RecordViewRenderer(RecordViewRendererTest.settings(),RecordViewRendererTest.messages()).render("<h1>{{record.name}}</h1><p>{{now}}</p><div>{{field.a}}</div>",RecordViewRendererTest.node(1,"template","a"),"Design",RecordViewRendererTest.record(),"secret",java.util.Locale.ENGLISH,RecordViewRendererTest.NOW);
  try(var pdf=org.apache.pdfbox.Loader.loadPDF(new NodePdfRenderer().render(service.prepare(output.document(),"Test",2)))) {
   assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf)).contains("<script>name</script>","2026-11-03 10:11:12","Kept {{now}}");
  }
 }
 @Test void longCodeTokensWrapWithinPrintablePage()throws Exception {
  byte[] bytes=new NodePdfRenderer().render(service.prepare("<pre>"+"abcdef0123456789".repeat(50)+"</pre>","Code",1));
  try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)) {
   var positions=new java.util.ArrayList<Float>();
   var stripper=new org.apache.pdfbox.text.PDFTextStripper(){@Override protected void processTextPosition(org.apache.pdfbox.text.TextPosition text){positions.add(text.getXDirAdj()+text.getWidthDirAdj());super.processTextPosition(text);}};
   stripper.getText(pdf);assertThat(positions).allMatch(x->x<550);
  }
 }
 @Test void enforcesHtmlAndImagePixelLimits()throws Exception {
  var limits=new PdfExportLimits();limits.setMaxHtmlCharacters(10);
  assertThatThrownBy(()->new RecordViewPdf(limits,new NodePdfRenderer()).prepare("x".repeat(11),"Test",1)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  limits.setMaxHtmlCharacters(10000);limits.setMaxImagePixels(1);
  var image=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);var output=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"jpeg",output);
  String html="<img src='data:image/jpeg;base64,"+Base64.getEncoder().encodeToString(output.toByteArray())+"'>";
  assertThatThrownBy(()->new RecordViewPdf(limits,new NodePdfRenderer()).prepare(html,"Test",1)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
}

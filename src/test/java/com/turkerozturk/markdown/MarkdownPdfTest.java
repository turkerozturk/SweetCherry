package com.turkerozturk.markdown;
import com.turkerozturk.pdf.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class MarkdownPdfTest {
 @Test void renderedPdfHasUnicodeHeadingsTableCodeAndClickableLinks()throws Exception{
  String html=NodeMarkup.forPdf("markdown","# Türkçe başlık\n\n[External](https://example.org)\n\n[Heading](#türkçe-başlık)\n\n| A | B |\n|---|---|\n| çığ | öşü |\n\n```java\nint value = 1;\n```\n\n![Remote](http://127.0.0.1:1/never-fetch.png)");
  var dom=new NodePdfDocument().prepare("Markdown node",html,1);var bytes=new NodePdfRenderer().render(dom);
  try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)){
   String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);assertThat(text).contains("Türkçe başlık","çığ","öşü","int value = 1;","Image not embedded").doesNotContain("# Türkçe");
   assertThat(pdf.getDocumentCatalog().getDocumentOutline()).isNotNull();assertThat(pdf.getPage(0).getAnnotations().stream().filter(a->a instanceof org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink).count()).isGreaterThanOrEqualTo(2);
  }
 }
 @Test void singleAndSubtreeExportsSelectTheNodeMarkupRendererAndLeaveSourceUntouched(){
  var children=org.mockito.Mockito.mock(com.turkerozturk.children.ChildrenService.class);var nodes=org.mockito.Mockito.mock(com.turkerozturk.node.NodeService.class);var rich=org.mockito.Mockito.mock(com.turkerozturk.richtext.experimental.RichTextRenderingService.class);var renderer=org.mockito.Mockito.mock(NodePdfRenderer.class);
  var place=new com.turkerozturk.children.Children();place.setNodeId(10L);place.setMasterId(1L);place.setFatherId(0L);
  var node=new com.turkerozturk.node.Node();node.setNodeId(1);node.setName("Name");node.setSyntax("markdown");node.setTxt("# Heading\n\n**Text**");
  org.mockito.Mockito.when(children.findById(10)).thenReturn(place);org.mockito.Mockito.when(children.getChildren()).thenReturn(java.util.List.of(place));org.mockito.Mockito.when(nodes.findById(1)).thenReturn(node);org.mockito.Mockito.when(renderer.render(org.mockito.ArgumentMatchers.any())).thenReturn(new byte[]{1});
  var service=new NodePdfExportService(children,nodes,rich,renderer);service.export(10);service.exportSubtree(10,PdfExportOptions.defaults(),null,false,"");
  var documents=org.mockito.ArgumentCaptor.forClass(org.w3c.dom.Document.class);org.mockito.Mockito.verify(renderer,org.mockito.Mockito.times(2)).render(documents.capture());for(var document:documents.getAllValues())assertThat(document.getDocumentElement().getTextContent()).contains("Heading","Text").doesNotContain("# Heading","**Text**");
  org.mockito.Mockito.verifyNoInteractions(rich);assertThat(node.getTxt()).isEqualTo("# Heading\n\n**Text**");
  assertThat(new com.turkerozturk.node.DataNode(node,"Label").getData()).contains("<h1");
  assertThat(new com.turkerozturk.exportmindmap.MindMapTextContent().text(1,"markdown",node.getTxt())).contains("Heading").doesNotContain("# Heading");
 }
}

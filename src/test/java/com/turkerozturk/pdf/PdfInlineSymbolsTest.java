package com.turkerozturk.pdf;

import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import static org.assertj.core.api.Assertions.*;

class PdfInlineSymbolsTest {
    @Test void allFreeFontStylesAndLocalCtbIconsRenderOfflineAndPreserveSurroundingText() throws Exception {
        var doc=new NodePdfDocument().prepare("Heading {{ctb:ct_camera}}", "<p>Before {{fa-solid fa-image}} {{fa-regular fa-image}} {{fa-brands fa-github}} after.</p>",1);
        byte[] bytes=new NodePdfRenderer().render(doc);
        assertThat(bytes.length).isGreaterThan(10000);
        try(var pdf=Loader.loadPDF(bytes)) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(pdf)).contains("Heading","Before","after.").doesNotContain("{{","fa-solid");
            var resources=pdf.getPage(0).getResources();
            var fonts=new java.util.ArrayList<String>();for(var name:resources.getFontNames())fonts.add(resources.getFont(name).getName());
            assertThat(fonts.stream().filter(name->name.replace(" ","").contains("FontAwesome")).count()).as(fonts.toString()).isEqualTo(3);
            assertThat(resources.getXObjectNames()).isNotEmpty();
            // The real PDF renderer must paint a nonempty page, not just silently embed fonts.
            var image=new org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImage(0);
            int colored=0;for(int y=0;y<200;y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)&0xffffff)!=0xffffff)colored++;
            assertThat(colored).isGreaterThan(200);
        }
    }
    @Test void codeAndUnrelatedExpressionsStayLiteralWhileTableNodeNamesRender() {
        var doc=new NodePdfDocument().prepare("Title", "<pre>{{fa-solid fa-image}}</pre><div data-symbols-skip>{{ctb:ct_camera}}</div><table><tr><td>{{fa-solid fa-image}}</td><td data-inline-symbols>{{fa-solid fa-image}}</td></tr></table><p>{{record.name}}</p>",1);
        PdfInlineSymbols.apply(doc);
        assertThat(doc.getDocumentElement().getTextContent()).contains("{{fa-solid fa-image}}","{{ctb:ct_camera}}","{{record.name}}");
        int icons=0;var all=doc.getElementsByTagName("*");for(int i=0;i<all.getLength();i++)if(((org.w3c.dom.Element)all.item(i)).getAttribute("class").equals("pdf-inline-symbol"))icons++;
        assertThat(icons).isEqualTo(1);
    }
    @Test void identicalAdjacentTextRunsAndSubtreeTitlesAndContentsRender() throws Exception {
        var parts=java.util.List.of(new SubtreePdfDocument.Part(1,1,0,"Root {{fa-solid fa-image}}","<div><span style='font-weight:bold'>{{fa-solid </span><span style='font-weight:bold'>fa-image}}</span> preserved</div>"),new SubtreePdfDocument.Part(2,2,1,"Child {{ctb:ct_camera}}","<p>body</p>"));
        var doc=new SubtreePdfDocument().prepare(parts,PdfExportOptions.defaults(),null,true,"Contents");
        try(var pdf=Loader.loadPDF(new NodePdfRenderer().render(doc))) {
            assertThat(new PDFTextStripper().getText(pdf)).contains("Root","Child","preserved","Contents").doesNotContain("{{","fa-solid");
        }
    }
    @Test void filteredListPdfUsesTheSameIconsInsideExplicitNameCells() throws Exception {
        byte[] bytes=new com.turkerozturk.helpers.PDFFromHTMLHelper().createPdfFromHtml("<table><tr><td data-inline-symbols>Report {{fa-solid fa-image}} {{ctb:ct_camera}}</td><td>other</td></tr></table>");
        try(var pdf=Loader.loadPDF(bytes)) {
            assertThat(new PDFTextStripper().getText(pdf)).contains("Report","other").doesNotContain("{{");
        }
    }
}

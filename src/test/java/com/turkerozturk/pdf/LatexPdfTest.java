package com.turkerozturk.pdf;

import com.turkerozturk.richtext.experimental.*;
import com.turkerozturk.richtext.experimental.RichTextLayout.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class LatexPdfTest {
    private String html() throws Exception {
        String source;try(var in=getClass().getResourceAsStream("/fixtures/cherrytree-demo-formula.tex")){source=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        var ref=new EmbeddedObject(ObjectKind.ATTACHMENT,53,0);
        return new RichTextObjectHtmlRenderer(true).render(new RichTextLayout(List.of(new ObjectPart(ref))),Map.of(ref,new EmbeddedContent.Latex(source)),"");
    }
    @Test void defaultPdfUsesVectorFormAndCanProduceAVisualProof() throws Exception {
        var document=new NodePdfDocument().prepare("LaTeX demo 53",html(),53);
        byte[] bytes=new NodePdfRenderer().render(document);
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            var counts=counts(pdf.getPage(0).getResources());assertThat(counts[0]).isGreaterThan(0);assertThat(counts[1]).isZero();
            var bitmap=new org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImageWithDPI(0,96);
            int ink=0;for(int y=140;y<350;y++)for(int x=75;x<400;x++)if((bitmap.getRGB(x,y)&0xffffff)<0x888888)ink++;
            assertThat(ink).as("formula must be readable, not a tiny unscaled template").isGreaterThan(300);
        }
        String proof=System.getProperty("latex.proof");if(proof!=null)java.nio.file.Files.write(java.nio.file.Path.of(proof),bytes);
    }
    @Test void pngFallbackAndMonochromeRemainAvailable() throws Exception {
        var document=new NodePdfDocument().prepare("Fallback",html(),53);
        var images=document.getElementsByTagName("img");for(int i=0;i<images.getLength();i++)((org.w3c.dom.Element)images.item(i)).removeAttribute("data-sc-latex");
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(new NodePdfRenderer().render(document))) { assertThat(counts(pdf.getPage(0).getResources())[1]).isGreaterThan(0); }
        var options=new PdfExportOptions("A4","portrait",false,true,true,false,false,false,false,false);
        document=new NodePdfDocument().prepare("Gray",html(),53,options,null);
        assertThat(((org.w3c.dom.Element)document.getElementsByTagName("img").item(0)).hasAttribute("data-sc-latex")).isFalse();
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(new NodePdfRenderer().render(document))) { assertThat(counts(pdf.getPage(0).getResources())[1]).isGreaterThan(0); }
    }
    private int[] counts(org.apache.pdfbox.pdmodel.PDResources resources) throws Exception {
        int forms=0,images=0;
        for(var name:resources.getXObjectNames()) {
            var item=resources.getXObject(name);
            if(item instanceof org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject form) {forms++;var nested=counts(form.getResources());forms+=nested[0];images+=nested[1];}
            if(item instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject)images++;
        }
        return new int[]{forms,images};
    }
}

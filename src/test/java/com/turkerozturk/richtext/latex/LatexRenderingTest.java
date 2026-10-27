package com.turkerozturk.richtext.latex;

import com.turkerozturk.richtext.experimental.*;
import com.turkerozturk.richtext.experimental.RichTextLayout.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class LatexRenderingTest {
    static String source() throws Exception {
        try(var in=LatexRenderingTest.class.getResourceAsStream("/fixtures/cherrytree-demo-formula.tex")) {
            return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    @Test void demo53FullDocumentBecomesSharpPngWithoutNeedingSystemTex() throws Exception {
        String source=source();var rendered=LatexRenderer.render(source);
        assertThat(rendered.math()).contains("\\begin{aligned}","\\frac{1}{x}").doesNotContain("documentclass","usepackage");
        var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(rendered.png()));
        assertThat(image.getWidth()).isEqualTo(rendered.width()*2);assertThat(image.getHeight()).isEqualTo(rendered.height()*2);
        int ink=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>0)ink++;
        assertThat(ink).isGreaterThan(100);
    }
    @Test void fileNetworkDynamicAndMacroCommandsAreRejectedBeforeParsing() {
        for(String input:List.of("\\includegraphics{http://127.0.0.1/secret}","\\input{/etc/passwd}","\\jlmDynamic{network}",
                "\\newcommand{\\x}{\\x}\\x","\\begin{document}missing end","{".repeat(65)+"x"+"}".repeat(65),"x".repeat(16385)))
            assertThatThrownBy(()->LatexRenderer.render(input)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void adapterChangesOnlyTheDisplaySnapshotAndPreservesAttachmentBytes() throws Exception {
        byte[] original=source().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var image=new com.turkerozturk.image.Image();image.setFileName(LatexRenderer.FILENAME);image.setPng(original.clone());
        var result=new EmbeddedContentAdapter().fromImage(image);
        assertThat(result).isInstanceOf(EmbeddedContent.Latex.class);assertThat(image.getPng()).isEqualTo(original);
        image.setFileName("report.pdf");assertThat(new EmbeddedContentAdapter().fromImage(image)).isInstanceOf(EmbeddedContent.Attachment.class);
    }
    @Test void liveAndPdfRenderingUseLatexWithoutChangingObjectKindsAndPdfExclusionStillWorks() throws Exception {
        var ref=new EmbeddedObject(ObjectKind.ATTACHMENT,53,0);
        var layout=new RichTextLayout(List.of(new ObjectPart(ref)));
        var payload=Map.<EmbeddedObject,EmbeddedContent>of(ref,new EmbeddedContent.Latex(source()));
        assertThat(new RichTextObjectHtmlRenderer().render(layout,payload,"token"))
                .contains("class=\"rich-text-latex\"","data:image/png;base64,").doesNotContain("data-sc-latex");
        assertThat(new RichTextObjectHtmlRenderer(true).render(layout,payload,""))
                .contains("data-sc-latex=");
        assertThat(new RichTextObjectHtmlRenderer(true,new PdfObjectSelection(true,true,true,false)).render(layout,payload,""))
                .contains("&lt;latex object: 53:0&gt;").doesNotContain("data:image");
        assertThat(new RichTextObjectHtmlRenderer().render(layout,Map.of(ref,new EmbeddedContent.Latex("\\includegraphics{<bad>}")),""))
                .contains("rich-text-latex-source","&lt;bad&gt;").doesNotContain("data:image");
    }
}

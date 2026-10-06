package com.turkerozturk.pdf;

import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.children.Children;
import com.turkerozturk.node.NodeService;
import com.turkerozturk.node.Node;
import com.turkerozturk.richtext.experimental.RichTextRenderingService;

class NodePdfExportTest {
    @Test void embedsFontsPreservesTurkishStylesAndExternalLink() throws Exception {
        byte[] bytes = new NodePdfRenderer().render(new NodePdfDocument().prepare("Türkçe Başlık",
                "<div>İstanbul ı İ ş Ş ğ Ğ ç Ç ö Ö ü Ü<br/>İkinci satır</div>"
                + "<span style='font-weight:bold;font-style:italic;color:#071af8'>Biçimli metin</span>"
                + "<a href='https://example.org/'>Bağlantı</a>", 53));
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(pdf)).contains("Türkçe Başlık", "İstanbul ı İ ş Ş ğ Ğ ç Ç ö Ö ü Ü", "İkinci satır", "Biçimli metin");
            for (var name : pdf.getPage(0).getResources().getFontNames())
                assertThat(pdf.getPage(0).getResources().getFont(name).isEmbedded()).isTrue();
            assertThat(pdf.getPage(0).getAnnotations().stream().filter(PDAnnotationLink.class::isInstance)
                    .map(PDAnnotationLink.class::cast).map(PDAnnotationLink::getAction)
                    .filter(PDActionURI.class::isInstance).map(PDActionURI.class::cast).map(PDActionURI::getURI))
                    .contains("https://example.org/");
        }
    }

    @Test void rendersTwoEmbeddedImagesAndTextInOrder() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(900, 200, BufferedImage.TYPE_INT_RGB), "png", output);
        String image = "<img src='data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray()) + "'/>";
        var document = new NodePdfDocument().prepare("Images", "<div>Before</div>" + image
                + "<div>Between</div>" + image + "<div>After</div>", 53);
        try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(document))) {
            String text = new PDFTextStripper().getText(pdf);
            assertThat(text.indexOf("Before")).isLessThan(text.indexOf("Between"));
            assertThat(text.indexOf("Between")).isLessThan(text.indexOf("After"));
            assertThat(pdf.getPage(0).getResources().getXObjectNames()).isNotEmpty();
            var raster = new org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImageWithDPI(0, 96);
            int longDarkRuns = 0, run = 0;
            for (int y = 0; y < raster.getHeight(); y++) {
                boolean dark = (raster.getRGB(raster.getWidth() / 2, y) & 0xffffff) < 0x202020;
                if (dark) run++;
                else { if (run > 60) longDarkRuns++; run = 0; }
            }
            if (run > 60) longDarkRuns++;
            assertThat(longDarkRuns).as("two images occupy separate vertical areas").isEqualTo(2);
        }
    }

    @Test void removesExternalResourcesAndUnsafeLinksBeforeRendering() {
        var doc = new NodePdfDocument().prepare("Safe", "<link href='file:///secret'/><script>bad()</script>"
                + "<img src='http://127.0.0.1/private'/><a href='javascript:alert(1)'>Text</a>"
                + "<div style='background-image:url(file:///secret);text-align:center'>Aligned</div>", 53);
        assertThat(doc.getElementsByTagName("script").getLength()).isZero();
        assertThat(doc.getElementsByTagName("link").getLength()).isZero();
        assertThat(doc.getElementsByTagName("img").getLength()).isZero();
        assertThat(((org.w3c.dom.Element)doc.getElementsByTagName("a").item(0)).hasAttribute("href")).isFalse();
        assertThat(doc.getDocumentElement().getTextContent()).contains("Image not embedded", "Aligned");
    }

    @Test void resolvesSharedOccurrenceToMasterAndUsesRichParser() {
        var children = mock(ChildrenService.class); var nodes = mock(NodeService.class);
        var rich = mock(RichTextRenderingService.class); var renderer = mock(NodePdfRenderer.class);
        var occurrence = mock(Children.class); var master = mock(Node.class);
        when(children.findById(72L)).thenReturn(occurrence); when(occurrence.getMasterId()).thenReturn(53L);
        when(nodes.findById(53L)).thenReturn(master); when(master.getName()).thenReturn("Master");
        when(master.getTxt()).thenReturn("<node><rich_text>Hello</rich_text></node>");
        when(master.getSyntax()).thenReturn("custom-colors"); when(rich.renderForPdf(master, "")).thenReturn("<div>Hello</div>");
        when(renderer.render(any())).thenReturn(new byte[]{1});
        assertThat(new NodePdfExportService(children, nodes, rich, renderer).export(72).title()).isEqualTo("Master");
        verify(rich).renderForPdf(master, ""); verify(nodes, never()).findById(72L);
    }

    @Test void handlesPlainTextAndReleasesPermitAfterMissingNode() {
        var children = mock(ChildrenService.class); var nodes = mock(NodeService.class);
        var rich = mock(RichTextRenderingService.class); var renderer = mock(NodePdfRenderer.class);
        var service = new NodePdfExportService(children, nodes, rich, renderer);
        assertThatThrownBy(() -> service.export(99)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var occurrence = mock(Children.class); var node = mock(Node.class);
        when(children.findById(1L)).thenReturn(occurrence); when(nodes.findById(1L)).thenReturn(node);
        when(node.getTxt()).thenReturn("<plain>\nİkinci satır"); when(node.getName()).thenReturn("Plain");
        when(renderer.render(any())).thenAnswer(call -> {
            org.w3c.dom.Document doc = call.getArgument(0);
            assertThat(doc.getDocumentElement().getTextContent()).contains("<plain>", "İkinci satır");
            assertThat(doc.getElementsByTagName("br").getLength()).isPositive();
            return new byte[]{1};
        });
        assertThat(service.export(1).title()).isEqualTo("Plain"); verifyNoInteractions(rich);
    }
    @Test void downloadUsesUtf8PdfFilenameAndRequiresTenant() throws Exception {
        var service = mock(NodePdfExportService.class);
        when(service.export(72L)).thenReturn(new NodePdfExportService.Export("Türkçe/Not\r\n", new byte[]{1, 2}));
        var controller = new PdfFromHtmlController(null, null, null, null, null, null);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "nodePdfExportService", service);
        var response = controller.exportNodeAsPdf(72, new org.springframework.mock.web.MockHttpServletRequest());
        assertThat(response.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("attachment");
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("Türkçe_Not__.pdf");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(PdfFromHtmlController.class.getMethod("exportNodeAsPdf", long.class,
                jakarta.servlet.http.HttpServletRequest.class).isAnnotationPresent(
                        com.turkerozturk.multipledatabases.RequiresTenant.class)).isTrue();
    }

}

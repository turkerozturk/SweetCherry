package com.turkerozturk.pdf;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

class PdfExportOptionsTest {
    private PdfExportOptions options(String paper, String orientation, boolean metadata) {
        return new PdfExportOptions(paper, orientation, false, false, true, true, true, metadata, true, true);
    }
    @Test void rendersLetterLandscapeFooterOutlineAndSafeMetadata() throws Exception {
        var options = options("Letter", "landscape", true);
        var source = new PdfSourceMetadata.Source("Demo source", "jdbc:sqlite:demo.ctb");
        var doc = new NodePdfDocument().prepare("Türkçe", "<div><span data-pdf-heading='h5' style='font-size:.83em;color:#f00000'>Beşinci başlık</span></div><div>Content</div>", 53, options, source);
        try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(doc))) {
            assertThat(pdf.getPage(0).getMediaBox().getWidth()).isCloseTo(792, within(1f));
            assertThat(pdf.getPage(0).getMediaBox().getHeight()).isCloseTo(612, within(1f));
            String text = new PDFTextStripper().getText(pdf);
            assertThat(text).contains("Türkçe.pdf", "1 / 1", "Content");
            assertThat(pdf.getDocumentCatalog().getDocumentOutline().getFirstChild().getTitle()).isEqualTo("Beşinci başlık");
            assertThat(pdf.getDocumentCatalog().getDocumentOutline().getFirstChild().findDestinationPage(pdf)).isEqualTo(pdf.getPage(0));
            assertThat(pdf.getDocumentInformation().getCreator()).isEqualTo("SweetCherry");
            assertThat(pdf.getDocumentInformation().getCustomMetadataValue("SweetCherrySource")).isEqualTo("Demo source");
            assertThat(pdf.getDocumentInformation().getCustomMetadataValue("SweetCherryConnection")).isEqualTo("jdbc:sqlite:demo.ctb");
        }
    }
    @Test void disabledMetadataOutlineAndTitleLeaveNoSweetCherryFields() throws Exception {
        var options = new PdfExportOptions("A4", "portrait", true, false, false, false, false, false, true, true);
        assertThat(options.sourceName()).isFalse(); assertThat(options.sourceAddress()).isFalse();
        var doc = new NodePdfDocument().prepare("Hidden title", "<div>Text</div>", 53, options, new PdfSourceMetadata.Source("secret", "secret"));
        try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(doc))) {
            assertThat(new PDFTextStripper().getText(pdf)).doesNotContain("Hidden title");
            assertThat(pdf.getDocumentInformation().getCreator()).isNull();
            assertThat(pdf.getDocumentInformation().getTitle()).isNull();
            assertThat(pdf.getDocumentInformation().getCustomMetadataValue("SweetCherrySource")).isNull();
            assertThat(pdf.getDocumentInformation().getCustomMetadataValue("SweetCherryConnection")).isNull();
            var outline = pdf.getDocumentCatalog().getDocumentOutline();
            assertThat(outline == null || outline.getFirstChild() == null).isTrue();
        }
    }
    @Test void connectionSanitizerOmitsSecretsAndDirectories() {
        assertThat(PdfSourceMetadata.safeAddress("jdbc:mysql://user:password@example.org:3306/notes?password=secret#token"))
                .isEqualTo("jdbc:mysql://example.org:3306");
        assertThat(PdfSourceMetadata.safeAddress("jdbc:sqlite:C:/private/path/demo.ctb?password=secret")).isEqualTo("jdbc:sqlite:demo.ctb");
        assertThat(PdfSourceMetadata.safeAddress("jdbc:sqlserver:secret")).isNull();
        assertThat(PdfSourceMetadata.safeAddress(null)).isNull();
    }
    @Test void validatesOptionsAndRemembersOnlySuccessfulExportsInSession() {
        assertThatThrownBy(() -> options("unknown", "portrait", true)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var service = mock(NodePdfExportService.class); var sources = mock(PdfSourceMetadata.class);
        var controller = new NodePdfOptionsController(service, sources);
        var session = new org.springframework.mock.web.MockHttpSession();
        when(service.export(eq(53L), any(), any())).thenReturn(new NodePdfExportService.Export("Node", new byte[]{1}));
        controller.download(53, session, "Letter", "landscape", false, false, true, true, true, false, true, true);
        assertThat(NodePdfOptionsController.remembered(session).paper()).isEqualTo("Letter");
        assertThat(NodePdfOptionsController.remembered(session).metadata()).isFalse();
        var model = new org.springframework.ui.ExtendedModelMap(); when(service.title(25L)).thenReturn("Other");
        assertThat(controller.page(25, session, model)).isEqualTo("node/pdfExportOptions");
        assertThat(model.get("options")).isEqualTo(NodePdfOptionsController.remembered(session));
        assertThat(model.get("nodeId")).isEqualTo(25L);
        when(service.export(eq(53L), any(), any())).thenThrow(new IllegalStateException("test"));
        assertThatThrownBy(() -> controller.download(53, session, "A4", "portrait", true, true, true, false, false, true, false, false)).isInstanceOf(IllegalStateException.class);
        assertThat(NodePdfOptionsController.remembered(session).paper()).isEqualTo("Letter");
    }
    @Test void grayscaleConvertsEmbeddedImageAndTextColors() throws Exception {
        var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) image.setRGB(x, y, 0xff0000);
        var output = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(image, "png", output);
        String html = "<div style='color:#ff0000;background-color:#00ff00'>Color</div><img src='data:image/png;base64,"
                + java.util.Base64.getEncoder().encodeToString(output.toByteArray()) + "'/>";
        var document = new NodePdfDocument().prepare("Gray", html, 53, options("A4", "portrait", false), null);
        var element = (org.w3c.dom.Element)document.getElementsByTagName("img").item(0);
        String base64 = element.getAttribute("src").substring("data:image/png;base64,".length());
        var gray = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(base64)));
        int pixel = gray.getRGB(0, 0);
        assertThat((pixel >> 16) & 255).isEqualTo((pixel >> 8) & 255).isEqualTo(pixel & 255);
        assertThat(((org.w3c.dom.Element)document.getElementsByTagName("div").item(1)).getAttribute("style"))
                .contains("color:#111", "background-color:#fff");
    }
    @Test void outlineDestinationAndFooterFollowActualPageAndPaperSize() throws Exception {
        for (String paper : new String[]{"A4", "Letter"}) for (String orientation : new String[]{"portrait", "landscape"}) {
            var options = new PdfExportOptions(paper, orientation, true, false, true, false, true, false, false, false);
            String html = "<div><span data-pdf-heading='h1'>Start</span></div>";
            for (int i = 0; i < 100; i++) html += "<div>Line " + i + "</div>";
            html += "<div><span data-pdf-heading='h2'>End</span></div>";
            try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(new NodePdfDocument().prepare("Pages", html, 53, options, null)))) {
                assertThat(pdf.getNumberOfPages()).isGreaterThan(1);
                float width = paper.equals("A4") ? 595.28f : 612;
                float height = paper.equals("A4") ? 841.89f : 792;
                assertThat(pdf.getPage(0).getMediaBox().getWidth()).isCloseTo(orientation.equals("portrait") ? width : height, within(1f));
                var end = pdf.getDocumentCatalog().getDocumentOutline().getFirstChild().getFirstChild();
                assertThat(end.getTitle()).isEqualTo("End");
                assertThat(end.findDestinationPage(pdf)).isEqualTo(pdf.getPage(pdf.getNumberOfPages() - 1));
                var stripper = new PDFTextStripper(); stripper.setStartPage(pdf.getNumberOfPages());
                assertThat(stripper.getText(pdf)).contains(pdf.getNumberOfPages() + " / " + pdf.getNumberOfPages());
            }
        }
    }


    @org.junit.jupiter.api.Test void subtreeUsesSamePreferencesAndRemembersContentsAfterSuccess() {
        var service = org.mockito.Mockito.mock(NodePdfExportService.class);
        var sources = org.mockito.Mockito.mock(PdfSourceMetadata.class);
        var controller = new NodePdfOptionsController(service, sources);
        var session = new org.springframework.mock.web.MockHttpSession();
        org.mockito.Mockito.when(service.exportSubtree(org.mockito.ArgumentMatchers.eq(53L),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(true), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new NodePdfExportService.Export("Root", new byte[]{1}));
        controller.subtreeDownload(53, session, "A4", "portrait", true, true, true,
                false, false, false, false, false, true, java.util.Locale.ENGLISH);
        var model = new org.springframework.ui.ExtendedModelMap();
        controller.subtreePage(25, session, model);
        org.assertj.core.api.Assertions.assertThat(model).containsEntry("includeDescendants", true).containsEntry("contents", true);
        org.assertj.core.api.Assertions.assertThat(model.get("options")).isEqualTo(PdfExportOptions.defaults());
    }
}

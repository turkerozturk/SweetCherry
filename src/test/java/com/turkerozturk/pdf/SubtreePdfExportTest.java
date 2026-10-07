package com.turkerozturk.pdf;

import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeService;
import com.turkerozturk.richtext.experimental.RichTextRenderingService;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubtreePdfExportTest {
    private List<SubtreePdfDocument.Part> parts() {
        return List.of(new SubtreePdfDocument.Part(1,1,0,"Ana Düğüm","<div>Ana içerik</div>"),
                new SubtreePdfDocument.Part(2,2,1,"Çocuk","<div>Çocuk içerik</div>"),
                new SubtreePdfDocument.Part(3,3,2,"Torun","<div>Torun içerik</div>"),
                new SubtreePdfDocument.Part(72,2,1,"Paylaşımlı","<div>Çocuk içerik</div>"));
    }

    @Test void hierarchyAndContentsLinksPointToOccurrencePages() throws Exception {
        var document = new SubtreePdfDocument().prepare(parts(), PdfExportOptions.defaults(), null, true, "İçindekiler");
        try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(document))) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(5);
            var root = pdf.getDocumentCatalog().getDocumentOutline().getFirstChild();
            assertThat(root.getTitle()).isEqualTo("Ana Düğüm");
            var child = root.getFirstChild();
            assertThat(child.getTitle()).isEqualTo("Çocuk");
            assertThat(child.getFirstChild().getTitle()).isEqualTo("Torun");
            assertThat(child.getNextSibling().getTitle()).isEqualTo("Paylaşımlı");
            assertThat(pdf.getPages().indexOf(root.findDestinationPage(pdf))).isEqualTo(1);
            assertThat(pdf.getPages().indexOf(child.findDestinationPage(pdf))).isEqualTo(2);
            assertThat(pdf.getPages().indexOf(child.getFirstChild().findDestinationPage(pdf))).isEqualTo(3);
            assertThat(pdf.getPages().indexOf(child.getNextSibling().findDestinationPage(pdf))).isEqualTo(4);
            assertThat(pdf.getPage(0).getAnnotations().stream().filter(PDAnnotationLink.class::isInstance).count()).isEqualTo(4);
            assertThat(new PDFTextStripper().getText(pdf)).contains("İçindekiler", "Ana içerik", "Torun içerik");
        }
    }

    @Test void contentsAndOutlineCanBeDisabledIndependently() throws Exception {
        var options = new PdfExportOptions("Letter", "landscape", true, false, false,
                false, true, false, false, false);
        try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(
                new SubtreePdfDocument().prepare(parts(), options, null, false, "İçindekiler")))) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(4);
            var outline = pdf.getDocumentCatalog().getDocumentOutline();
            assertThat(outline == null || outline.getFirstChild() == null).isTrue();
            assertThat(new PDFTextStripper().getText(pdf)).doesNotContain("İçindekiler", "Ana Düğüm");
        }
    }

    @Test void traversalKeepsSiblingOrderAndSharedOccurrencesAreLeaves() {
        var children = mock(ChildrenService.class); var nodes = mock(NodeService.class);
        var rich = mock(RichTextRenderingService.class); var renderer = mock(NodePdfRenderer.class);
        var root = occurrence(1,0,1,0); var child = occurrence(2,1,1,0);
        var shared = occurrence(72,1,2,2); var hidden = occurrence(99,72,1,0);
        when(children.findById(1L)).thenReturn(root);
        when(children.getChildren()).thenReturn(List.of(hidden, shared, root, child));
        for (long id : new long[]{1,2}) {
            var node = mock(Node.class); when(node.getName()).thenReturn("Node " + id);
            when(node.getTxt()).thenReturn("Text " + id); when(nodes.findById(id)).thenReturn(node);
        }
        when(renderer.render(any())).thenAnswer(call -> {
            org.w3c.dom.Document doc = call.getArgument(0);
            var titles = doc.getElementsByTagName("h1");
            assertThat(titles.getLength()).isEqualTo(3);
            assertThat(((org.w3c.dom.Element)titles.item(0)).getAttribute("id")).isEqualTo("sc-node-1");
            assertThat(((org.w3c.dom.Element)titles.item(1)).getAttribute("id")).isEqualTo("sc-node-2");
            assertThat(((org.w3c.dom.Element)titles.item(2)).getAttribute("id")).isEqualTo("sc-node-72");
            return new byte[]{1};
        });
        var service = new NodePdfExportService(children, nodes, rich, renderer);
        assertThat(service.exportSubtree(1, PdfExportOptions.defaults(), null, false, "Contents").bytes()).containsExactly((byte)1);
        verify(nodes, never()).findById(99L);
        verify(nodes, times(2)).findById(2L);
    }

    @Test void malformedCycleFailsAndReleasesRenderPermit() {
        var children = mock(ChildrenService.class); var nodes = mock(NodeService.class);
        var root = occurrence(1,1,1,0); when(children.findById(1L)).thenReturn(root);
        when(children.getChildren()).thenReturn(List.of(root));
        var node = mock(Node.class); when(node.getName()).thenReturn("Root"); when(nodes.findById(1L)).thenReturn(node);
        var service = new NodePdfExportService(children, nodes, mock(RichTextRenderingService.class), mock(NodePdfRenderer.class));
        for (int i = 0; i < 2; i++) assertThatThrownBy(() -> service.exportSubtree(1,
                PdfExportOptions.defaults(), null, false, "Contents"))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
    }

    private Children occurrence(long id, long parent, long sequence, long master) {
        var value = new Children().setNodeId(id).setFatherId(parent).setSequence(sequence);
        value.setMasterId(master); return value;
    }

    @Test void configuredNodeLimitCanBeRaisedWithoutChangingExportCode() {
        var children = mock(ChildrenService.class); var nodes = mock(NodeService.class);
        var root = occurrence(1,0,1,0); var child = occurrence(2,1,1,0);
        when(children.findById(1L)).thenReturn(root);
        when(children.getChildren()).thenReturn(List.of(root, child));
        var node = mock(Node.class); when(node.getName()).thenReturn("Node");
        when(nodes.findById(anyLong())).thenReturn(node);
        var renderer = mock(NodePdfRenderer.class); when(renderer.render(any())).thenReturn(new byte[]{1});
        var service = new NodePdfExportService(children, nodes, mock(RichTextRenderingService.class), renderer);
        var limits = new PdfExportLimits(); limits.setMaxNodes(1); service.configureLimits(limits);
        assertThatThrownBy(() -> service.exportSubtree(1, PdfExportOptions.defaults(), null, false, "Contents"))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(413));
        limits.setMaxNodes(2);
        assertThat(service.exportSubtree(1, PdfExportOptions.defaults(), null, false, "Contents").bytes())
                .containsExactly((byte)1);
    }
}

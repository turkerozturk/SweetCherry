package com.turkerozturk.pdf;

import com.turkerozturk.node.*;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TemplatePdfExportTest {
    @Test
    void landscapeReportHasRealValuesWrappingAndPageTotals() throws Exception {
        TemplateNode template = mock(TemplateNode.class);
        ProjectNode project = mock(ProjectNode.class);
        DataNode data = mock(DataNode.class);
        when(template.getName()).thenReturn("Türkçe Şablon");
        when(template.getNodeId()).thenReturn(4398L);
        when(project.getName()).thenReturn("Bir İki Üç Dört Beş");
        when(project.getNodeId()).thenReturn(53L);
        when(data.getNodeId()).thenReturn(54L);
        when(project.getDataNodes()).thenReturn(Map.of("value", data));
        var labels = new LinkedHashMap<String, String>();
        labels.put("value", "Değişken Adı Üç Kelime");
        when(template.getDataLabels()).thenReturn(labels);
        when(template.getProjectNodes()).thenReturn(List.of(project));
        var document = new TemplatePdfDocument().prepare(template,
                Map.of(54L, "<div>Gerçek değer: çığ öşü &amp; &lt;örnek&gt;</div>"));
        byte[] bytes = new NodePdfRenderer().render(document);
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(pdf.getPage(0).getMediaBox().getWidth()).isGreaterThan(840);
            assertThat(pdf.getPage(0).getMediaBox().getHeight()).isLessThan(600);
            String text = new PDFTextStripper().getText(pdf).replace("\r\n", "\n").replace("\r", "\n");
            assertThat(text).contains("Türkçe Şablon", "(53)", "Gerçek değer", "çığ öşü", "1 / 1");
            assertThat(text).contains("Bir İki\nÜç Dört\nBeş");
            assertThat(text).doesNotContain("Hello World");
        }
    }

    @Test
    void valuesAreReadFromTheirNodesAndRichTextUsesPdfRenderer() {
        TemplateService templates = mock(TemplateService.class);
        NodeService nodes = mock(NodeService.class);
        var rich = mock(com.turkerozturk.richtext.experimental.RichTextRenderingService.class);
        NodePdfRenderer renderer = mock(NodePdfRenderer.class);
        TemplateNode template = mock(TemplateNode.class);
        ProjectNode project = mock(ProjectNode.class);
        DataNode data = mock(DataNode.class);
        Node node = mock(Node.class);
        when(templates.getTemplateNode(1L)).thenReturn(template);
        when(template.getName()).thenReturn("Test Şablonu");
        when(template.getProjectNodes()).thenReturn(List.of(project));
        when(template.getDataLabels()).thenReturn(Map.of("a", "A"));
        when(project.getDataNodes()).thenReturn(Map.of("a", data));
        when(data.getNodeId()).thenReturn(2L);
        when(nodes.getById(2L)).thenReturn(node);
        when(node.getSyntax()).thenReturn("custom-colors");
        when(node.getTxt()).thenReturn("<node><rich_text>Değer</rich_text></node>");
        when(rich.renderForPdf(node, "")).thenReturn("<div>Değer</div>");
        when(renderer.render(any())).thenReturn(new byte[]{1});
        assertThat(new TemplatePdfExportService(templates, nodes, rich, renderer).export(1L).bytes())
                .containsExactly((byte) 1);
        verify(rich).renderForPdf(node, "");
    }
}

package com.turkerozturk.pdf;

import com.turkerozturk.node.NodeService;
import com.turkerozturk.node.TemplateService;
import com.turkerozturk.richtext.experimental.RichTextRenderingService;
import java.util.HashMap;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplatePdfExportService {
    private final TemplateService templates;
    private final NodeService nodes;
    private final RichTextRenderingService richText;
    private final NodePdfRenderer renderer;

    public TemplatePdfExportService(TemplateService templates, NodeService nodes,
            RichTextRenderingService richText, NodePdfRenderer renderer) {
        this.templates = templates;
        this.nodes = nodes;
        this.richText = richText;
        this.renderer = renderer;
    }

    @Transactional(readOnly = true)
    public NodePdfExportService.Export export(long id) {
        var template = templates.getTemplateNode(id);
        var values = new HashMap<Long, String>();
        for (var project : template.getProjectNodes()) {
            for (var data : project.getDataNodes().values()) {
                if (values.containsKey(data.getNodeId())) continue;
                var node = nodes.getById(data.getNodeId());
                String text = node.getTxt() == null ? "" : node.getTxt();
                if (text.length() > 8 * 1024 * 1024)
                    throw new IllegalArgumentException("PDF value too large");
                String html = "custom-colors".equals(node.getSyntax()) && !text.isBlank()
                        ? richText.renderForPdf(node, "")
                        : new Element("div").text(text).outerHtml();
                values.put(data.getNodeId(), html);
            }
        }
        return new NodePdfExportService.Export(template.getName(),
                renderer.render(new TemplatePdfDocument().prepare(template, values)));
    }
}

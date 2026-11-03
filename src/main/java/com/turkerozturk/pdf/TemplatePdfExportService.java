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
    @org.springframework.beans.factory.annotation.Autowired(required=false) private org.springframework.context.MessageSource messages;

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
                var node = nodes.getById(data.getContentId()==0?data.getNodeId():data.getContentId());
                String text = node.getTxt() == null ? "" : node.getTxt();
                if (text.length() > 8 * 1024 * 1024)
                    throw new IllegalArgumentException("PDF value too large");
                String html = "custom-colors".equals(node.getSyntax()) && !text.isBlank()
                        ? richText.renderForPdf(node, "")
                        : com.turkerozturk.markdown.NodeMarkup.supports(node.getSyntax()) ? com.turkerozturk.markdown.NodeMarkup.forPdf(node.getSyntax(),text) : com.turkerozturk.node.DataNode.plainHtml(text);
                values.put(data.getNodeId(), html);
            }
        }
        return new NodePdfExportService.Export(template.getName(),
                renderer.render(messages==null?new TemplatePdfDocument().prepare(template,values):new TemplatePdfDocument().prepare(template,values,key->messages.getMessage(key,null,org.springframework.context.i18n.LocaleContextHolder.getLocale()))));
    }
}

package com.turkerozturk.pdf;

import com.turkerozturk.node.TemplateNode;
import java.util.Map;
import org.jsoup.nodes.Element;

/** A landscape report using the same fonts and offline resource policy as node PDFs. */
public final class TemplatePdfDocument {
    public org.w3c.dom.Document prepare(TemplateNode template, Map<Long, String> values) {
        var html = org.jsoup.Jsoup.parseBodyFragment("");
        html.outputSettings().prettyPrint(false);
        Element table = html.body().appendElement("table").addClass("template-report");
        Element heading = table.appendElement("thead").appendElement("tr");
        heading.appendElement("th").addClass("number").text("#");
        heading.appendElement("th").addClass("node-name").text("Node");
        heading.appendElement("th").addClass("variables").text("Variables / Values");
        Element body = table.appendElement("tbody");
        int index = 0;
        for (var project : template.getProjectNodes()) {
            Element row = body.appendElement("tr");
            row.appendElement("td").addClass("number").text(Integer.toString(++index));
            Element name = row.appendElement("td").addClass("node-name");
            twoWords(name, project.getName());
            name.appendElement("br");
            name.appendText("(" + project.getNodeId() + ")");
            Element nested = row.appendElement("td").addClass("variables")
                    .appendElement("table").addClass("variable-table").appendElement("tbody");
            for (var label : template.getDataLabels().entrySet()) {
                Element pair = nested.appendElement("tr");
                twoWords(pair.appendElement("td").addClass("variable-name"), label.getValue());
                var data = project.getDataNodes().get(label.getKey());
                pair.appendElement("td").addClass("variable-value")
                        .html(data == null ? "" : values.getOrDefault(data.getNodeId(), ""));
            }
        }
        var options = new PdfExportOptions("A4", "landscape", true, false, false,
                true, true, false, false, false);
        var document = new NodePdfDocument().prepare(template.getName(), html.body().html(),
                template.getNodeId(), options, null, template.getName());
        var style = document.createElementNS("http://www.w3.org/1999/xhtml", "style");
        style.setTextContent("""
            body { font-size: 10pt; }
            .template-report > tbody > tr { page-break-inside: avoid; }
            .template-report .number { width: 5%; }
            .template-report .node-name { width: 20%; }
            .template-report .variables { width: 75%; padding: 0; }
            .variable-table { margin: 0; width: 100%; }
            .variable-table .variable-name { width: 33.333%; }
            .variable-table .variable-value { width: 66.667%; }
            .variable-value table { width: 100%; }
            """);
        document.getElementsByTagName("head").item(0).appendChild(style);
        return document;
    }

    private static void twoWords(Element element, String text) {
        String[] words = (text == null ? "" : text.trim()).split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                if (i % 2 == 0) element.appendElement("br");
                else element.appendText(" ");
            }
            element.appendText(words[i]);
        }
    }
}

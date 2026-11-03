package com.turkerozturk.pdf;

import com.turkerozturk.node.TemplateNode;
import java.util.Map;
import org.jsoup.nodes.Element;

/** A landscape report using the same fonts and offline resource policy as node PDFs. */
public final class TemplatePdfDocument {
    public org.w3c.dom.Document prepare(TemplateNode template, Map<Long, String> values) {
        return prepare(template,values,key->switch(key){case "report.node"->"Node";case "report.values"->"Variables / Values";case "report.state.missing"->"Missing field";case "report.state.empty"->"Empty content";case "report.state.ambiguous"->"Multiple fields";default->"Invalid/repeated definitions (excluded)";});
    }
    public org.w3c.dom.Document prepare(TemplateNode template, Map<Long,String> values, java.util.function.Function<String,String> messages) {
        var html = org.jsoup.Jsoup.parseBodyFragment("");
        if(template.getDefinitionIssues()!=null && !template.getDefinitionIssues().isEmpty())html.body().appendElement("p").text(messages.apply("report.definitions")+": "+String.join(", ",template.getDefinitionIssues()));
        html.outputSettings().prettyPrint(false);
        Element table = html.body().appendElement("table").addClass("template-report");
        Element heading = table.appendElement("thead").appendElement("tr");
        heading.appendElement("th").addClass("number").text("#");
        heading.appendElement("th").addClass("node-name").text(messages.apply("report.node"));
        heading.appendElement("th").addClass("variables").text(messages.apply("report.values"));
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
                var field=com.turkerozturk.node.TemplateField.of(project,label.getKey(),label.getValue());
                Element value=pair.appendElement("td").addClass("variable-value");
                if(field.status().equals("value"))value.html(values.getOrDefault(field.data().getNodeId(), ""));
                else {
                    value.appendElement("small").text(messages.apply("report.state."+field.status()));
                    if(field.status().equals("ambiguous"))value.appendText(" "+field.matches().stream().map(d->"["+d.getNodeId()+"]").collect(java.util.stream.Collectors.joining(", ")));
                    else value.addClass("write-in").appendElement("br");
                }
            }
        }
        var options = new PdfExportOptions("A4", "landscape", true, false, false,
                true, true, false, false, false);
        var document = new NodePdfDocument().prepare(template.getName(), html.body().html(),
                template.getNodeId(), options, null, template.getName());
        // Nested value cells occupy half the landscape page, not the full node-PDF width.
        // A block image avoids inline overlap in the PDF renderer. Keep math and paperclips inline.
        var images=document.getElementsByTagName("img");
        for(int i=0;i<images.getLength();i++) {
            var image=(org.w3c.dom.Element)images.item(i);
            if(image.hasAttribute("data-sc-latex") || (image.getParentNode() instanceof org.w3c.dom.Element parent && parent.getAttribute("class").contains("rich-text-attachment")))continue;
            var width=java.util.regex.Pattern.compile("width:([0-9.]+)px").matcher(image.getAttribute("style"));
            var height=java.util.regex.Pattern.compile("height:([0-9.]+)px").matcher(image.getAttribute("style"));
            if(width.find() && height.find()) {
                double w=Double.parseDouble(width.group(1)),h=Double.parseDouble(height.group(1));
                double scale=Math.min(1,Math.min(460/w,350/h));
                image.setAttribute("style","display:block;width:"+Math.max(1,(int)(w*scale))+"px;height:"+Math.max(1,(int)(h*scale))+"px;margin:3px 0;");
            }
        }
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
            .write-in { height: 32pt; color: #555; }
            .variable-value { white-space: pre-wrap; }
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

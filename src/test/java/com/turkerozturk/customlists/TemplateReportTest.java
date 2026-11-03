package com.turkerozturk.customlists;

import com.turkerozturk.node.*;
import com.turkerozturk.children.*;
import com.turkerozturk.exportmindmap.MindMapTextContent;
import com.turkerozturk.richtext.experimental.RichTextRenderingService;
import com.turkerozturk.xlsx.TemplateXlsxDocument;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayInputStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemplateReportTest {
    static Node node(long id,String name,String txt){var n=new Node();n.setNodeId(id);n.setName(name);n.setTxt(txt);n.setSyntax("plain-text");n.setTags("report");return n;}
    @Test void orderedDefinitionsPreserveLegacyCommentsAndRejectEveryRepeatedKey(){
        var t=new TemplateNode(node(1,"report","z\tLast first\r\n#disabled\n legacy-comment\na\tAlpha\na\tOther\n\tInvalid\nb\t\n"));
        assertThat(t.getDataLabels().keySet()).containsExactly("z","b");assertThat(t.getDataLabels().get("b")).isEqualTo("b");assertThat(t.getDefinitionIssues()).containsExactly("[5] a","[6] ∅");
        assertThat(new TemplateNode(node(1,"report",null)).getDataLabels()).isEmpty();
    }
    static DataNode data(long id,String key,String raw){var d=new DataNode(node(id,key,raw),key);d.prepare(id,new org.jsoup.nodes.Element("div").text(raw).outerHtml(),raw);return d;}
    static TemplateNode report(){
        var t=new TemplateNode(node(1,"report","z\tZed\nmissing\tempty missing\nempty\tBlank\nduplicate\tDuplicate"));
        var p=new ProjectNode(node(2,"2026-11-03",""),"Root > Record");p.setDataLabels(t.getDataLabels());p.setDataNodes(new LinkedHashMap<>(Map.of("z",data(3,"z","=1+1\nTürkçe & <text>"),"empty",data(4,"empty","\u00a0 "))));
        p.setDuplicateDataNodes(Map.of("duplicate",List.of(data(5,"duplicate","First"),data(6,"duplicate","Second"))));t.setProjectNodes(List.of(p));return t;
    }
    @Test void statusesAreOrderedAndAmbiguityNeverPicksAValue(){
        var p=report().getProjectNodes().get(0);assertThat(p.getFields()).extracting(TemplateField::status).containsExactly("value","missing","empty","ambiguous");assertThat(p.isIncomplete()).isTrue();assertThat(p.getFields().get(3).data()).isNull();
    }
    @Test void serviceUsesOccurrenceChildrenAndPreservesExactTagMembership(){
        var nodes=mock(NodeService.class);var children=mock(ChildrenRepository.class);var rich=mock(RichTextRenderingService.class);var text=mock(MindMapTextContent.class);
        var template=node(1,"report","field\tLabel\nmissing");var record=node(2,"Record","");var wrong=node(8,"Wrong","");wrong.setTags("REPORT");var value=node(3,"field","real content");
        when(nodes.findDisplayNode(1)).thenReturn(template);when(nodes.getNodesByTags("report")).thenReturn(List.of(wrong,record));when(nodes.findDisplayNode(11)).thenReturn(value);when(nodes.findDisplayNode(12)).thenReturn(value);
        var a=new Children();a.setNodeId(11L);var b=new Children();b.setNodeId(12L);when(children.findByFatherIdOrderBySequenceAsc(2)).thenReturn(List.of(a,b));when(text.text(3,"plain-text","real content")).thenReturn("real content");
        var service=new TemplateService();ReflectionTestUtils.setField(service,"nodeService",nodes);ReflectionTestUtils.setField(service,"children",children);ReflectionTestUtils.setField(service,"richText",rich);ReflectionTestUtils.setField(service,"textContent",text);
        var result=service.getTemplateNode(1);assertThat(result.getProjectNodes()).hasSize(1);var p=result.getProjectNodes().get(0);assertThat(p.getDataNodes()).isEmpty();assertThat(p.getDuplicateDataNodes().get("field")).extracting(DataNode::getNodeId).containsExactly(11L,12L);assertThat(p.getDuplicateDataNodes().get("field")).extracting(DataNode::getContentId).containsExactly(3L,3L);assertThat(p.getMissingDataLabels()).containsExactly("missing");verify(children,never()).findByFatherIdOrderBySequenceAsc(8);
    }
    static ResourceBundleMessageSource messages(){var m=new ResourceBundleMessageSource();m.setBasename("messages");m.setDefaultEncoding("UTF-8");return m;}
    @Test void xlsxUsesDisplayLabelsLiteralStringsAndExplicitStatusesInBothLanguages()throws Exception {
        for(Locale locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){var m=messages();try(var book=new XSSFWorkbook(new ByteArrayInputStream(new TemplateXlsxDocument().render(report(),m,locale)))){
            var s=book.getSheetAt(0);assertThat(s.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Zed");assertThat(s.getRow(1).getCell(2).getCellType()).isEqualTo(org.apache.poi.ss.usermodel.CellType.STRING);assertThat(s.getRow(1).getCell(2).getStringCellValue()).isEqualTo("=1+1\nTürkçe & <text>");assertThat(s.getRow(1).getCell(3).getStringCellValue()).isEqualTo(m.getMessage("report.state.missing",null,locale));assertThat(s.getRow(1).getCell(4).getStringCellValue()).isEqualTo(m.getMessage("report.state.empty",null,locale));assertThat(s.getRow(1).getCell(5).getStringCellValue()).contains("[5]", "[6]").doesNotContain("First","Second");assertThat(s.getPaneInformation()).isNotNull();
        }}
    }
    @Test void pdfKeepsWriteInAreasAndOrderedAmbiguityDiagnostics()throws Exception {
        var t=report();var doc=new com.turkerozturk.pdf.TemplatePdfDocument().prepare(t,Map.of(3L,"<div>Value</div>"),k->messages().getMessage(k,null,Locale.ENGLISH));
        var transform=javax.xml.transform.TransformerFactory.newInstance().newTransformer();var out=new java.io.StringWriter();transform.transform(new javax.xml.transform.dom.DOMSource(doc),new javax.xml.transform.stream.StreamResult(out));
        String html=out.toString();assertThat(html).contains("write-in","Missing field","Empty content","Multiple matching fields","[5]","[6]").doesNotContain("First","Second");assertThat(html.indexOf("Zed")).isLessThan(html.indexOf("empty missing"));
    }
    @Test void normalPdfImagesFitNestedValuesAndDoNotOverlapInlineText()throws Exception {
        var bitmap=new java.awt.image.BufferedImage(1600,80,java.awt.image.BufferedImage.TYPE_INT_RGB);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(bitmap,"png",bytes);
        String image="<img src=\"data:image/png;base64,"+java.util.Base64.getEncoder().encodeToString(bytes.toByteArray())+"\">";
        var document=new com.turkerozturk.pdf.TemplatePdfDocument().prepare(report(),Map.of(3L,"<div>Before"+image+"After</div>"));
        var element=(org.w3c.dom.Element)document.getElementsByTagName("img").item(0);
        assertThat(element.getAttribute("style")).contains("display:block","width:460px");
    }

    @Test void plainLineBreaksSurviveHtmlSerializationAndPdfLayout()throws Exception {
        String html=DataNode.plainHtml("First line\nTürkçe second line");assertThat(html).contains("First line\nTürkçe second line");
        var doc=new com.turkerozturk.pdf.TemplatePdfDocument().prepare(report(),Map.of(3L,html));
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(new com.turkerozturk.pdf.NodePdfRenderer().render(doc))) {
            String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).replace("\r\n","\n");
            assertThat(text).contains("First line\nTürkçe second line");
        }
    }

}

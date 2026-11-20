package com.turkerozturk.image.attachments.drawio;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DrawioCompatibilityTest {
    static org.jsoup.nodes.Document graph(String xml){return org.jsoup.Jsoup.parse(xml,"",org.jsoup.parser.Parser.xmlParser());}
    @Test void upstreamTemplatesWithGeneratedRootIdsRetainEveryParentAndConnectorReference()throws Exception{
        for(String name:List.of("upstream-flowchart.xml","upstream-mindmap.xml")){
            byte[] source;try(var input=getClass().getResourceAsStream("/drawio/"+name)){assertThat(input).isNotNull();source=input.readAllBytes();}
            byte[] copy=source.clone();var preview=DrawioDocument.parse(source,"test.drawio","Reference");assertThat(preview.pages()).isNotEmpty();assertThat(source).isEqualTo(copy);
            for(var page:preview.pages()){
                var model=graph(page.xml());var ids=new HashSet<>(model.select("mxCell").eachAttr("id"));
                assertThat(model.selectFirst("mxCell[id=0]").hasAttr("parent")).isFalse();assertThat(model.selectFirst("mxCell[id=1]").attr("parent")).isEqualTo("0");
                for(var cell:model.select("mxCell"))for(String ref:List.of("parent","source","target"))if(cell.hasAttr(ref))assertThat(ids).contains(cell.attr(ref));
                assertThat(model.select("mxCell[vertex=1]")).isNotEmpty();
            }
        }
    }
    @Test void arbitraryIdsUserObjectIdsAndPortsAreMappedToSafeRenderIds(){
        String longId="part / + 图 "+"x".repeat(180);
        String source="<mxGraphModel><root><mxCell id='root/document'/><mxCell id='layer name' parent='root/document'/><object id='__proto__' label='A'><mxCell id='ignored-inner-id' vertex='1' parent='layer name'/></object><mxCell id='"+longId+"' value='B' vertex='1' parent='layer name'/><mxCell id='edge' edge='1' parent='layer name' source='__proto__' target='"+longId+"' style='sourcePort=__proto__;targetPort="+longId+";'/></root></mxGraphModel>";
        var result=graph(DrawioDocument.parse(DrawioServiceTest.bytes(source),"test.drawio","Node").pages().get(0).xml());
        var a=result.selectFirst("mxCell[value=A]");var b=result.selectFirst("mxCell[value=B]");var edge=result.selectFirst("mxCell[edge=1]");
        assertThat(edge.attr("source")).isEqualTo(a.attr("id"));assertThat(edge.attr("target")).isEqualTo(b.attr("id"));
        assertThat(edge.attr("style")).contains("sourcePort="+a.attr("id"),"targetPort="+b.attr("id"));
        assertThat(result.select("mxCell").eachAttr("id")).allMatch(id->id.matches("0|1|sc[0-9]+"));
    }
    @Test void blankPagesAndEmptyModelsDoNotPreventOtherPagesFromOpening(){
        String file="<mxfile><diagram name='Blank'/><diagram name='Empty model'><mxGraphModel><root/></mxGraphModel></diagram><diagram name='Content'>"+DrawioServiceTest.model("Visible")+"</diagram></mxfile>";
        var result=DrawioDocument.parse(DrawioServiceTest.bytes(file),"test.drawio","Node");assertThat(result.pages()).hasSize(3);assertThat(result.pages().get(0).name()).isEqualTo("Blank");
        assertThat(graph(result.pages().get(0).xml()).select("mxCell")).hasSize(2);assertThat(result.pages().get(2).xml()).contains("Visible");
        var rootOnly=DrawioDocument.parse(DrawioServiceTest.bytes("<mxGraphModel><root><mxCell id='custom-root'/></root></mxGraphModel>"),"test.drawio","Node");
        assertThat(graph(rootOnly.pages().get(0).xml()).selectFirst("mxCell[id=1]").attr("parent")).isEqualTo("0");
    }
    @Test void diagnosticsIdentifyTheFailingPageStageAndXmlLocationWithoutContentLeaks()throws Exception{
        String good="<diagram>"+DrawioServiceTest.model("Private content")+"</diagram>";
        assertThatThrownBy(()->DrawioDocument.parse(DrawioServiceTest.bytes("<mxfile>"+good+"<diagram>not base64</diagram></mxfile>"),"test.drawio","Node")).isInstanceOf(DrawioService.Problem.class).satisfies(e->{
            var problem=(DrawioService.Problem)e;assertThat(problem.code()).isEqualTo("invalid");assertThat(problem.reason()).isEqualTo("compression");assertThat(problem.page()).isEqualTo(2);assertThat(problem.getMessage()).contains("reason=compression","page=2").doesNotContain("Private content");
        });
        String damaged=DrawioServiceTest.compressed("<mxGraphModel>\n<root>");
        assertThatThrownBy(()->DrawioDocument.parse(DrawioServiceTest.bytes("<mxfile>"+good+"<diagram>"+damaged+"</diagram></mxfile>"),"test.drawio","Node")).isInstanceOf(DrawioService.Problem.class).satisfies(e->{var problem=(DrawioService.Problem)e;assertThat(problem.reason()).isEqualTo("xml");assertThat(problem.page()).isEqualTo(2);assertThat(problem.line()).isPositive();});
    }
    @Test void invalidParentReferencesAndDuplicateIdsStillFailWithSpecificReasons(){
        for(var item:Map.of("missing_parent",DrawioServiceTest.model("x").replace("parent='1'","parent='absent'"),"parent_cycle",DrawioServiceTest.model("x").replace("parent='1'","parent='2'"),"duplicate_cell",DrawioServiceTest.model("x").replace("</root>","<mxCell id='2'/></root>")).entrySet()){
            assertThatThrownBy(()->DrawioDocument.parse(DrawioServiceTest.bytes(item.getValue()),"test.drawio","Node")).isInstanceOf(DrawioService.Problem.class).satisfies(e->assertThat(((DrawioService.Problem)e).reason()).isEqualTo(item.getKey()));
        }
    }
    @Test void blankModelsStillRespectTheConfiguredOutputBudget()throws Exception{
        var settings=DrawioSettingsTest.with("pageBytes",20);
        assertThatThrownBy(()->DrawioDocument.parse(DrawioServiceTest.bytes("<mxGraphModel/>"),"test.drawio","Node",settings)).isInstanceOf(DrawioService.Problem.class).satisfies(e->assertThat(((DrawioService.Problem)e).limit()).isEqualTo("max-page-bytes"));
    }
}

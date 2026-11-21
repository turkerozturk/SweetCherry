package com.turkerozturk.image.attachments.drawio;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DrawioLinksTest {
    @Test void onlyExplicitWebAndPageLinksAreAccepted(){
        assertThat(DrawioLinks.safe("https://example.test/manual?q=one&section=2#step")).isEqualTo("https://example.test/manual?q=one&section=2#step");
        assertThat(DrawioLinks.safe("http://localhost:8090/manual")).isEqualTo("http://localhost:8090/manual");
        assertThat(DrawioLinks.safe("data:page/id,page-2")).isEqualTo("data:page/id,page-2");
        for(String link:List.of("javascript:alert(1)","JaVaScRiPt:alert(1)","data:text/html,evil","data:action/json,{}","file:///C:/secret","//example.test","/shutdownContext","mailto:user@example.test","https://user:password@example.test/","https://example.test/\n","https://example.test/%0d%0aevil","https://example.test\\@evil.test","data:page/id,","data:page/id,"+"x".repeat(256),"https:example.test","not a link"))assertThat(DrawioLinks.safe(link)).as(link).isNull();
    }
    @Test void pageIdsAndWrapperCellAndLegacyStyleLinksSurviveInInertMetadata(){
        String model="<mxGraphModel><root><mxCell id='0'/><mxCell id='1' parent='0'/><object id='a' label='Page 2' link='data:page/id,second'><mxCell vertex='1' parent='1'/></object><mxCell id='b' vertex='1' parent='1' link='https://example.test/cell'/><mxCell id='c' vertex='1' parent='1' style='link=https://example.test/style;'/><mxCell id='d' vertex='1' parent='1' scLink='javascript:evil()'/></root></mxGraphModel>";
        var preview=DrawioDocument.parse(DrawioServiceTest.bytes("<mxfile><diagram id='first' name='First'>"+model+"</diagram><diagram id='second' name='Second'>"+DrawioServiceTest.model("Two")+"</diagram></mxfile>"),"test.drawio","Note");
        assertThat(preview.pages().stream().map(DrawioService.Page::id)).containsExactly("first","second");
        var cells=org.jsoup.Jsoup.parse(preview.pages().get(0).xml(),"",org.jsoup.parser.Parser.xmlParser());assertThat(cells.select("mxCell[scLink]").eachAttr("scLink")).containsExactly("data:page/id,second","https://example.test/cell","https://example.test/style");assertThat(preview.pages().get(0).xml()).doesNotContain("javascript:");
    }
    @Test void htmlLinksRetainSafeNavigationButStripActionsTargetsAndEventHandlers(){
        String label="<a href=\"https://example.test/manual\" target=\"_top\" onclick=\"evil()\">Web</a> <a href=\"data:page/id,second\">Second</a> <a href=\"javascript:evil()\">Unsafe text</a> <a href=\"data:action/json,{}\">Action text</a>";
        var preview=DrawioDocument.parse(DrawioServiceTest.bytes(DrawioServiceTest.model(org.jsoup.nodes.Entities.escape(label))),"test.drawio","Note");
        var cell=org.jsoup.Jsoup.parse(preview.pages().get(0).xml(),"",org.jsoup.parser.Parser.xmlParser()).selectFirst("mxCell[vertex=1]");var html=org.jsoup.Jsoup.parse(cell.attr("value"));
        assertThat(html.select("a")).hasSize(2);assertThat(html.select("a").eachAttr("href")).containsExactly("https://example.test/manual","data:page/id,second");assertThat(html.select("a").eachAttr("target")).containsOnly("_blank");assertThat(html.select("a").eachAttr("rel")).containsOnly("noopener noreferrer");assertThat(html.text()).contains("Unsafe text","Action text");assertThat(cell.attr("value")).doesNotContain("onclick","javascript:","data:action","_top");
    }
    @Test void invalidPageIdsAreNotPublishedAndLegacyPageConstructionStillWorks(){
        assertThat(DrawioLinks.pageId("x".repeat(256))).isEmpty();assertThat(DrawioLinks.pageId("\tbad")).isEmpty();assertThat(new DrawioService.Page("One","<mxGraphModel/>").id()).isEmpty();
    }
    @Test void localFallbackDoesNotPermitNetworkRequestsOrUnnoncedScriptsInsideTheOpaqueFrame(){
        var controller=new DrawioController(mock(DrawioService.class),new DrawioLibraries(new MockEnvironment()));var model=new ExtendedModelMap();var response=new MockHttpServletResponse();controller.frame("current",model,response);
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");assertThat(response.getHeader("X-DNS-Prefetch-Control")).isEqualTo("off");
        Map<String,String> directives=new HashMap<>();for(String part:response.getHeader("Content-Security-Policy").split(";")){String[] entry=part.strip().split(" ",2);directives.put(entry[0],entry.length==2?entry[1]:"");}
        assertThat(directives.get("default-src")).isEqualTo("'self'");assertThat(directives.get("script-src")).matches("'nonce-[a-zA-Z0-9-]+'").doesNotContain("'self'","unsafe-inline","unsafe-eval");
        for(String type:List.of("connect-src","font-src","frame-src","child-src","worker-src","media-src","manifest-src","object-src","base-uri","form-action"))assertThat(directives.get(type)).as(type).isEqualTo("'none'");
        assertThat(directives.get("img-src")).isEqualTo("data:");assertThat(directives.get("sandbox")).contains("allow-scripts","allow-popups","allow-popups-to-escape-sandbox").doesNotContain("allow-same-origin","allow-top-navigation");
    }
}

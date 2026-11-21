package com.turkerozturk.image.attachments.drawio;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class DrawioPresentationTest {
    private String label(String html){String xml=DrawioServiceTest.model(org.jsoup.nodes.Entities.escape(html).replace("\"","&quot;"));return cell(xml).attr("value");}
    private org.jsoup.nodes.Element cell(String xml){var preview=DrawioDocument.parse(DrawioServiceTest.bytes(xml),"test.drawio","Note");return org.jsoup.Jsoup.parse(preview.pages().get(0).xml(),"",org.jsoup.parser.Parser.xmlParser()).selectFirst("mxCell[vertex=1]");}
    @Test void rgbRgbaHexAndBackgroundShorthandRetainForegroundAndHighlightColors(){
        String html="<span style='color:rgb(1, 2, 3);background-color:#123456'>one</span><span style='color:#456789;background:rgba(200, 30, 20, 0.5)'>two</span><font color='#987654'>three</font>";
        var doc=org.jsoup.Jsoup.parse(label(html));assertThat(doc.select("span").get(0).attr("style")).contains("color:rgb(1, 2, 3)","background-color:#123456");assertThat(doc.select("span").get(1).attr("style")).contains("color:#456789","background-color:rgba(200, 30, 20, 0.5)");assertThat(doc.selectFirst("font").attr("color")).isEqualTo("#987654");
        String style=cell(DrawioServiceTest.model("safe").replace("rounded=1;","fontColor=rgb(10, 20, 30);labelBackgroundColor=#123456;")).attr("style");assertThat(style).contains("fontColor=rgb(10, 20, 30)","labelBackgroundColor=#123456");
    }
    @Test void htmlImageWithoutUrlUsesAlternativeTextAndRemainsAToggleableMarker(){
        var html=org.jsoup.Jsoup.parse(label("<img alt='Network appliance' src=''>"));assertThat(html.selectFirst(".sc-drawio-placeholder").text()).contains("[Image: Network appliance]");assertThat(html.select("img")).isEmpty();
    }
    @Test void colorSupportDoesNotAdmitUrlsExpressionsOrActiveMarkup(){
        String html=label("<span style='color:expression(evil());background:url(https://example.invalid/x);font-size:100000px'>safe</span><script>evil()</script>");assertThat(html).contains("safe").doesNotContain("expression","url(","example.invalid","100000","<script");
    }
    @Test void externalImageHasFilenameMarkerAndStableMappingKeyButNoOriginalUrl(){
        String source="https://example.invalid/manual/screenshot.png?secret=hidden";String xml=DrawioServiceTest.model("Caption").replace("rounded=1;","shape=image;image="+source+";");var cell=cell(xml);
        assertThat(cell.attr("style")).contains("scImageKey="+DrawioDocument.imageKey(source));assertThat(cell.attr("value")).contains("sc-drawio-placeholder","[Image: screenshot.png]").doesNotContain("example.invalid","secret=hidden");
    }
    @Test void missingImageSourceAndEmbeddedShapeUseMeaningfulNames(){
        assertThat(cell(DrawioServiceTest.model("").replace("rounded=1;fillColor=#ffcc00;html=1;","shape=image")).attr("value")).contains("[Image: missing source]");
        var cell=cell(DrawioServiceTest.model("").replace("rounded=1;","shape=mxgraph.vendor.special_icon;image=data:image/svg+xml,untrusted;"));assertThat(cell.attr("value")).contains("[Image: mxgraph.vendor.special_icon]");assertThat(cell.attr("style")).contains("scOriginalShape=mxgraph.vendor.special_icon");
    }
}

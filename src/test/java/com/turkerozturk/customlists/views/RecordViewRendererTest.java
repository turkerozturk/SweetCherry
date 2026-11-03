package com.turkerozturk.customlists.views;

import com.turkerozturk.node.*;
import java.util.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.jsoup.Jsoup;
import static org.assertj.core.api.Assertions.*;

class RecordViewRendererTest {
    static Node node(long id,String name,String text){var node=new Node();node.setNodeId(id);node.setName(name);node.setTxt(text);node.setTags("template");node.setSyntax("plain-text");return node;}
    static ResourceBundleMessageSource messages(){var m=new ResourceBundleMessageSource();m.setBasename("messages");m.setDefaultEncoding("UTF-8");return m;}
    static ProjectNode record(){
        var project=new ProjectNode(node(2,"<script>name</script>",""),"Parent > Name");project.setDataLabels(new LinkedHashMap<>(Map.of("a","A","empty","Empty","missing","Missing","duplicate","Duplicate")));
        var data=new DataNode(node(3,"a",""),"A");data.prepare(3,"<b>Kept {{now}}</b>","Kept {{now}}");var empty=new DataNode(node(4,"empty",""),"Empty");empty.prepare(4,"","");
        project.setDataNodes(Map.of("a",data,"empty",empty));project.setDuplicateDataNodes(Map.of("duplicate",List.of(data,empty)));return project;
    }
    static RecordViewSettings settings(){var s=new RecordViewSettings();s.setTimeZone("UTC");return s;}
    static final Instant NOW=Instant.parse("2026-11-03T10:11:12Z");
    @Test void substitutesTextAndRichFieldsWithoutRecursiveEvaluation(){
        var output=new RecordViewRenderer(settings(),messages()).render("<h1>{{record.name}}</h1><p>{{record.id}} {{template.name}} {{view.name}} {{now}}</p><section>{{field.a}}</section>",node(1,"template","a"),"Design",record(),"token",Locale.ENGLISH,NOW);
        var doc=Jsoup.parse(output.document());assertThat(doc.select("h1").text()).isEqualTo("<script>name</script>");assertThat(doc.select("script")).isEmpty();assertThat(doc.select("b").text()).isEqualTo("Kept {{now}}");assertThat(doc.select("p").text()).contains("2 template Design 2026-11-03 10:11:12");
        assertThat(doc.select("meta[http-equiv=Content-Security-Policy]").attr("content")).isEqualTo(RecordViewRenderer.CSP);
    }
    @Test void stripsActiveContentAndDisablesAttributePlaceholdersAndRemoteImages(){
        String source="<script>alert(1)</script><iframe src='https://bad'></iframe><form action='/shutdownContext'><input></form><p onclick='alert(2)'>Text</p><a href='javascript:alert(3)'>Bad</a><a href='https://example.com'>Good</a><a href='/nodes/12?_tenantView=v'>Node</a><a href='/restartContext'>Action</a><img src='https://tracker.test/a.png'><div title='{{record.id}}'>{{record.id}}</div>";
        var output=new RecordViewRenderer(settings(),messages()).render(source,node(1,"template","a"),"Design",record(),"v",Locale.ENGLISH,NOW);var doc=Jsoup.parse(output.document());
        assertThat(doc.select("script,iframe,form,input,[onclick]")).isEmpty();assertThat(doc.select("img[src^=https]")).isEmpty();assertThat(doc.select("a[href]").eachAttr("href")).containsExactly("https://example.com","/nodes/12?_tenantView=v");assertThat(doc.select("a[href]").eachAttr("target")).containsOnly("_blank");assertThat(doc.select("div[title]")).isEmpty();assertThat(output.warnings()).isNotEmpty();
    }
    @Test void statusAndUnknownMarkersAreLocalizedAndNeverSelectDuplicates(){
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
            var output=new RecordViewRenderer(settings(),messages()).render("{{field.missing}}{{field.empty}}{{field.duplicate}}{{field.not-defined}}{{unknown}}",node(1,"template","a"),"Design",record(),"current",locale,NOW);
            assertThat(output.document()).contains(messages().getMessage("report.state.missing",null,locale),messages().getMessage("report.state.empty",null,locale),messages().getMessage("report.state.ambiguous",null,locale),"/nodes/3?_tenantView=current").doesNotContain("Kept {{now}}");assertThat(output.warnings()).hasSize(5);
        }
    }
    @Test void limitsRejectOversizedDesignsAndOutput(){
        var settings=settings();settings.setMaxDesignCharacters(10);var renderer=new RecordViewRenderer(settings,messages());
        assertThatThrownBy(()->renderer.render("x".repeat(11),node(1,"template","a"),"D",record(),"v",Locale.ENGLISH,NOW)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        settings.setMaxRenderedCharacters(10);assertThatThrownBy(()->renderer.render("x",node(1,"template","a"),"D",record(),"v",Locale.ENGLISH,NOW)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    @Test void datePatternAndZoneHaveValidatedDefaults(){
        var settings=settings();assertThat(settings.now(NOW,Locale.ENGLISH)).isEqualTo("2026-11-03 10:11:12");settings.setDateTimeFormat("dd.MM.yyyy HH:mm");settings.setTimeZone("Europe/Istanbul");assertThat(settings.now(NOW,Locale.ENGLISH)).isEqualTo("03.11.2026 13:11");
        assertThatThrownBy(()->settings.setDateTimeFormat("invalid [")).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->settings.setTimeZone("Invalid/Zone")).isInstanceOf(java.time.DateTimeException.class);
    }
}

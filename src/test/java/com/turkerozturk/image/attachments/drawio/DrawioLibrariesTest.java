package com.turkerozturk.image.attachments.drawio;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import com.turkerozturk.settings.editing.ApplicationSettingsService;
import static org.assertj.core.api.Assertions.*;

class DrawioLibrariesTest {
    @Test void allFamiliesAreEnabledByDefaultAndOnlyFixedLocalIconScriptsAreReturned(){
        var libraries=new DrawioLibraries(new MockEnvironment());
        assertThat(libraries.enabled()).hasSize(75).contains("cabinets","eip","infographic","flowchart","cisco","clip_art");
        assertThat(libraries.iconScripts()).hasSize(10).allMatch(s->s.startsWith("/js/drawio/icons/")&&s.endsWith(".js"));
        assertThat(DrawioLibraries.GROUPS.stream().map(DrawioLibraries::key).distinct().count()).isEqualTo(75);
    }
    @Test void disabledFamiliesAreExcludedAndAllTogglesRoundTripThroughSettingsEditor(){
        var libraries=new DrawioLibraries(new MockEnvironment().withProperty(DrawioLibraries.key("cabinets"),"false").withProperty(DrawioLibraries.key("azure2"),"false"));
        assertThat(libraries.enabled()).doesNotContain("cabinets","azure2").contains("eip");assertThat(libraries.iconScripts()).doesNotContain("/js/drawio/icons/azure2.js");
        Map<String,String> values=new LinkedHashMap<>();ApplicationSettingsService.SETTINGS.forEach(s->values.put(s.key(),s.fallback()));values.put(DrawioLibraries.key("infographic"),"false");
        assertThat(ApplicationSettingsService.update("myapp: {}\n",values)).contains("myapp.drawio-preview.shape-libraries.infographic: false");
        values.put(DrawioLibraries.key("eip"),"https://example.com");assertThatThrownBy(()->ApplicationSettingsService.update("myapp: {}\n",values)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void knownVendorPathsSurviveButExternalAndTraversalImagesRemainPlaceholders(){
        String xml="<mxGraphModel><root><mxCell id=\"0\"/><mxCell id=\"1\" parent=\"0\"/>"
            +"<mxCell id=\"2\" parent=\"1\" vertex=\"1\" style=\"shape=image;image=img/lib/azure2/compute/Virtual_Machines.svg;\"/>"
            +"<mxCell id=\"3\" parent=\"1\" vertex=\"1\" style=\"shape=image;image=https://example.com/photo.png;\"/>"
            +"</root></mxGraphModel>";
        var result=DrawioDocument.parse(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8),"test.drawio","Note");
        assertThat(result.pages().get(0).xml()).contains("image=sc-icon:azure2/compute/Virtual_Machines.svg","[Image: photo.png]").doesNotContain("https://example.com");
        assertThat(DrawioLibraries.iconPath("https://app.diagrams.net/img/lib/clip_art/a.png")).isEqualTo("clip_art/a.png");
        for(String value:List.of("http://app.diagrams.net/img/lib/a.svg","https://example.com/img/lib/a.svg","img/lib/../a.svg","img/lib/a.svg?x=1","img/lib/%2e%2e/a.svg","https://app.diagrams.net:443/img/lib/a.svg","data:image/png,test"))assertThat(DrawioLibraries.iconPath(value)).as(value).isNull();
    }
}

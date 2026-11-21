package com.turkerozturk.symbols;

import com.turkerozturk.pdf.PdfFilename;
import com.turkerozturk.export.texttree.TextTreePaths;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class InlineSymbolsTest {
    @Test void allowlistAgreesWithTheBrowserAndNeverEvaluatesTemplatesOrPaths() throws Exception {
        var items=new java.util.Properties();
        try(var input=getClass().getResourceAsStream("/symbols/catalog.properties")) {
            items.load(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)); assertThat(items.size()).isEqualTo(2530);
            for(String key:items.stringPropertyNames()) {
                var item=items.getProperty(key).split("\\|",-1);
                String marker=item[0].equals("ctb")?"ctb:"+item[1]:"fa-"+item[0]+" fa-"+item[1];
                assertThat(InlineSymbols.resolve(marker)).as(marker).isNotNull();
            }
        }
        assertThat(InlineSymbols.resolve("CTB:CT_CAMERA").name()).isEqualTo("ct_camera");
        assertThat(InlineSymbols.resolve("\u00a0fa-solid\u00a0fa-image\u00a0")).isEqualTo(InlineSymbols.resolve("fa-solid fa-image"));
        assertThat(InlineSymbols.resolve("fas fa-image")).isEqualTo(InlineSymbols.resolve("fa-solid fa-image"));
        for(String text:new String[]{"ctb:../../private","fa-solid fa-image fa-spin","record.name","fa-solid fa-invented"})assertThat(InlineSymbols.resolve(text)).isNull();
        assertThat(InlineSymbols.matches("{{{fa-solid fa-image}}} {{record.name}} {{fa-solid\nfa-image}}")).isEmpty();
    }
    @Test void onlyKnownMarkersDisappearFromFileComponentsAndEmptyNamesHaveSafeFallbacks() {
        String title="Report {{fa-solid fa-image}} {{CTB:CT_CAMERA}} 😀 {{record.name}}";
        assertThat(PdfFilename.create(title,7)).doesNotContain("fa-solid","CTB:").contains("Report","😀","{{record.name}}");
        assertThat(TextTreePaths.safe(title,7)).doesNotContain("fa-solid","CTB:").contains("Report","😀","{{record.name}}");
        assertThat(PdfFilename.create("{{fa-solid fa-image}}",7)).isEqualTo("Node-7.pdf");
        assertThat(TextTreePaths.safe("{{ctb:ct_camera}}",7)).isEqualTo("node-7");
        assertThat(TextTreePaths.safe("{{fa-solid fa-image}}CON",7)).isEqualTo("node-7-CON");
        assertThat(InlineSymbols.filename("a{{fa-solid fa-image}}b{{fa-brands fa-github}}c")).isEqualTo("abc");
        assertThat(InlineSymbols.filename("{{{fa-solid fa-image}}}")).isEqualTo("{{{fa-solid fa-image}}}");
    }
}

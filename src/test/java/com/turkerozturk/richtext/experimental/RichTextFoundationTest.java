package com.turkerozturk.richtext.experimental;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RichTextFoundationTest {
    private final RichTextXmlReader reader = new RichTextXmlReader();
    private final RichTextHtmlRenderer renderer = new RichTextHtmlRenderer();

    @Test void preservesEmptyRunsAndCountsUnicodeCharacters() {
        var document = reader.read("<node><rich_text>A😀</rich_text><rich_text/><rich_text>B</rich_text></node>");
        assertThat(document.runs()).hasSize(3);
        assertThat(document.runs().get(0).characterCount()).isEqualTo(2);
        assertThat(document.runs().get(1).text()).isEmpty();
        assertThat(document.runs().get(2).textOffset()).isEqualTo(2);
    }

    @Test void preservesUnknownAttributesAndEscapesText() {
        var document = reader.read("<node><rich_text future=\"value\">&lt;script&gt;&amp;\nnext</rich_text></node>");
        assertThat(document.runs().get(0).attributes()).containsEntry("future", "value");
        assertThat(renderer.render(document)).contains("&lt;script&gt;&amp;\nnext").doesNotContain("<script>", "future=");
    }

    @Test void combinesLinkScaleAndDecorations() {
        var document = reader.read("<node><rich_text link=\"webs https://example.org\" scale=\"h2\" underline=\"single\" strikethrough=\"true\">Title</rich_text></node>");
        assertThat(renderer.render(document)).contains("href=\"https://example.org\"", "font-size:1.5em", "text-decoration:underline line-through");
    }

    @Test void rejectsUnsafeLinksAndStyleValues() {
        var document = reader.read("<node><rich_text link=\"webs javascript:alert(1)\" foreground=\"red;position:fixed\" scale=\"script\">safe</rich_text></node>");
        assertThat(renderer.render(document)).doesNotContain("href=", "position:", "<script>");
    }

    @Test void convertsSixteenBitColors() {
        var document = reader.read("<node><rich_text foreground=\"#a5a51d1d2d2d\">color</rich_text></node>");
        assertThat(renderer.render(document)).contains("color:#a51d2d;");
    }

    @Test void rejectsDoctypeAndExternalEntities() {
        assertThatThrownBy(() -> reader.read("<!DOCTYPE node [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><node><rich_text>&x;</rich_text></node>"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsUnsupportedStructureRatherThanDroppingIt() {
        for (String xml : new String[]{"<other/>", "<node><table/></node>", "<node><rich_text><b>x</b></rich_text></node>", "<node>lost</node>"}) {
            assertThatThrownBy(() -> reader.read(xml)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void readsProjectFixtureWithoutDiscardingEmptyRuns() throws Exception {
        try (var input = getClass().getResourceAsStream("/richtext/demo-node-53.xml")) {
            assertThat(input).isNotNull();
            var document = reader.read(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            assertThat(document.runs().stream().filter(run -> run.text().isEmpty()).count()).isEqualTo(13);
            assertThat(document.runs()).anySatisfy(run -> assertThat(run.attributes()).containsEntry("scale", "h1"));
            assertThat(renderer.render(document)).contains("color:#a51d2d;", "font-family:monospace;");
        }
    }
}

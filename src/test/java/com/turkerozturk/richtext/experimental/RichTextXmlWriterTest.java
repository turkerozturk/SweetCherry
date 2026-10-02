package com.turkerozturk.richtext.experimental;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RichTextXmlWriterTest {
    private final RichTextXmlReader reader = new RichTextXmlReader();
    private final RichTextXmlWriter writer = new RichTextXmlWriter();

    private void roundTrip(String xml) {
        var original = reader.read(xml);
        String written = writer.write(original);
        assertThat(reader.read(written)).isEqualTo(original);
        assertThat(writer.write(reader.read(written))).isEqualTo(written);
    }

    @Test void preservesProjectFixtureIncludingAllEmptyRuns() throws Exception {
        try (var input = getClass().getResourceAsStream("/richtext/demo-node-53.xml")) {
            assertThat(input).isNotNull();
            String xml = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            roundTrip(xml);
            assertThat(reader.read(writer.write(reader.read(xml))).runs().stream().filter(run -> run.text().isEmpty()).count())
                    .isEqualTo(13);
        }
    }

    @Test void preservesMixedStylesAndUnknownAttributes() {
        roundTrip("<node><rich_text weight='heavy' style='italic' underline='single' strikethrough='true' scale='sup' foreground='#a5a51d1d2d2d' future='keep'>text</rich_text></node>");
    }

    @Test void escapesHtmlXmlAndPreservesUnicodeAndWhitespace() {
        var model = new RichTextDocument(List.of(new RichTextDocument.TextRun("<&>\"'😀 Türkçe\n\n  \tend\r", Map.of("future", "<&>\"'\n\t\r"), 0)));
        assertThat(reader.read(writer.write(model))).isEqualTo(model);
        assertThat(writer.write(model)).contains("&lt;", "&amp;");
    }

    @Test void preservesEmptyDocumentAndConsecutiveEmptyRuns() {
        roundTrip("<node/>");
        roundTrip("<node><rich_text/><rich_text foreground='#3584e4'/><rich_text>A</rich_text><rich_text/></node>");
    }

    @Test void preservesLinkValuesWithoutConvertingThemToHtmlUrls() {
        roundTrip("<node><rich_text link='node 53 capalink'>Anchor</rich_text><rich_text link='webs https://example.org/?a=1&amp;b=2'>Web</rich_text><rich_text link='file cGF0aA=='>File</rich_text></node>");
    }

    @Test void rejectsInvalidCharactersAndUnpairedSurrogates() {
        for (String text : new String[]{"bad\u0001", "\uD800", "\uDC00"}) {
            var model = new RichTextDocument(List.of(new RichTextDocument.TextRun(text, Map.of(), 0)));
            assertThatThrownBy(() -> writer.write(model)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void rejectsInvalidAttributeNamesValuesAndOffsets() {
        for (var run : List.of(new RichTextDocument.TextRun("A", Map.of("bad name", "value"), 0),
                new RichTextDocument.TextRun("A", Map.of("future", "\u0000"), 0),
                new RichTextDocument.TextRun("A", Map.of(), 1))) {
            assertThatThrownBy(() -> writer.write(new RichTextDocument(List.of(run)))).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void roundTripDoesNotChangeEmbeddedObjectPlacement() {
        var original = reader.read("<node><rich_text weight='heavy'>A😀B</rich_text><rich_text/></node>");
        var objects = List.of(new RichTextLayout.EmbeddedObject(RichTextLayout.ObjectKind.IMAGE, 53, 2),
                new RichTextLayout.EmbeddedObject(RichTextLayout.ObjectKind.TABLE, 53, 4));
        var builder = new RichTextLayoutBuilder();
        assertThat(builder.build(reader.read(writer.write(original)), objects)).isEqualTo(builder.build(original, objects));
    }
}

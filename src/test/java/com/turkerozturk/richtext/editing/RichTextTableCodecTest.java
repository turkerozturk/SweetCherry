package com.turkerozturk.richtext.editing;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.turkerozturk.richtext.experimental.EmbeddedContentAdapter;

class RichTextTableCodecTest {
    private final String original = "<table col_widths='120,80' future='keep'><row><cell>Body</cell><cell>B</cell></row><row><cell>Header</cell><cell>H</cell></row></table>";
    @Test void showsHeaderFirstAndWritesItLastWithAllAttributes() {
        var display = RichTextTableCodec.open(original);
        assertThat(display.get(0)).containsExactly("Header", "H");
        var stored = new EmbeddedContentAdapter().readTable(RichTextTableCodec.write(display, original));
        assertThat(stored.rows().get(0)).containsExactly("Body", "B");
        assertThat(stored.rows().get(1)).containsExactly("Header", "H");
        assertThat(stored.attributes()).containsEntry("col_widths", "120,80").containsEntry("future", "keep");
    }
    @Test void writesNewTablesWithAutoWidthsAndEscapedMultilineUnicodeText() {
        var rows = List.of(List.of("H1", "H2"), List.of("<&>😀\nText", ""));
        String xml = RichTextTableCodec.write(rows, null);
        assertThat(RichTextTableCodec.open(xml)).isEqualTo(rows);
        assertThat(new EmbeddedContentAdapter().readTable(xml).attributes()).containsEntry("col_widths", "0,0");
    }
    @Test void refusesDimensionChangesToExistingTables() {
        assertThatThrownBy(() -> RichTextTableCodec.write(List.of(List.of("One")), original)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsMalformedUnboundedAndNonTextCellData() {
        for (String json : List.of("[]", "{\"new-table:test\":[]}", "{\"new-table:test\":[[1]]}", "{\"new-table:test\":[[\"A\"],[\"B\",\"C\"]]}", "{\"image:0\":[[\"A\"]]}")) {
            assertThatThrownBy(() -> RichTextTableCodec.decode(json)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(RichTextTableCodec.decode("{}")).isEmpty();
        assertThat(RichTextTableCodec.decode("{\"new-table:test\":[[\"Header\"],[\"Body\"]]}")).containsKey("new-table:test");
    }
    @Test void rejectsUnsupportedNestedCellsAndDtds() {
        for (String xml : List.of("<!DOCTYPE table><table/>", "<table><row><cell><rich_text>A</rich_text></cell></row></table>")) {
            assertThatThrownBy(() -> RichTextTableCodec.open(xml)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void refusesInvalidXmlCharactersAndOversizedCells() {
        for (String cell : List.of("\u0000", "a".repeat(5001))) {
            assertThatThrownBy(() -> RichTextTableCodec.write(List.of(List.of(cell)), null)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

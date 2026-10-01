package com.turkerozturk.richtext.experimental;

import org.junit.jupiter.api.Test;
import com.turkerozturk.image.Image;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmbeddedContentAdapterTest {
    private final EmbeddedContentAdapter adapter = new EmbeddedContentAdapter();

    @Test void readsTableWithoutReorderingOrLosingMetadata() {
        var table = adapter.readTable("<table col_widths='0,120'><row><cell>&lt;x&gt;</cell><cell/></row><row><cell>Header</cell><cell>Two</cell></row></table>");
        assertThat(table.rows()).containsExactly(java.util.List.of("<x>", ""), java.util.List.of("Header", "Two"));
        assertThat(table.attributes()).containsEntry("col_widths", "0,120");
    }

    @Test void rejectsDoctypeAndUnsupportedTableStructure() {
        for (String xml : new String[]{"<!DOCTYPE table [<!ENTITY x 'x'>]><table/>", "<node/>", "<table><other/></table>", "<table><row><cell><b>x</b></cell></row></table>"}) {
            assertThatThrownBy(() -> adapter.readTable(xml)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void attachmentsAndAnchorsDoNotLoadBinaryContent() {
        var image = mock(Image.class);
        when(image.getFileName()).thenReturn("file.txt");
        assertThat(adapter.fromImage(image)).isEqualTo(new EmbeddedContent.Attachment("file.txt"));
        when(image.getAnchor()).thenReturn("section");
        assertThat(adapter.fromImage(image)).isEqualTo(new EmbeddedContent.Anchor("section"));
        verify(image, never()).getPng();
    }

    @Test void readsTableFixtureFromDemoNode53() throws Exception {
        try (var input = getClass().getResourceAsStream("/richtext/demo-node-53-table.xml")) {
            assertThat(input).isNotNull();
            var table = adapter.readTable(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            assertThat(table.rows()).hasSize(3);
            assertThat(table.rows().get(2).get(0)).isEqualTo("satır 1 sütun 1 başlık");
        }
    }
}

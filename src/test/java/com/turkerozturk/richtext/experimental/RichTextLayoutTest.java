package com.turkerozturk.richtext.experimental;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.turkerozturk.richtext.experimental.RichTextLayout.*;

class RichTextLayoutTest {
    private final RichTextXmlReader reader = new RichTextXmlReader();
    private final RichTextLayoutBuilder builder = new RichTextLayoutBuilder();
    private EmbeddedObject object(ObjectKind kind, int offset) { return new EmbeddedObject(kind, 53, offset); }
    private String sequence(RichTextLayout layout) {
        var text = new StringBuilder();
        for (var part : layout.parts()) {
            if (part instanceof TextPart t) text.append(t.run().text());
            else text.append('[').append(((ObjectPart) part).object().kind()).append(']');
        }
        return text.toString();
    }

    @Test void splitsTextAroundObjectInsideRun() {
        var layout = builder.build(reader.read("<node><rich_text weight='heavy'>ABC</rich_text></node>"), List.of(object(ObjectKind.IMAGE, 1)));
        assertThat(sequence(layout)).isEqualTo("A[IMAGE]BC");
        for (var part : layout.parts()) if (part instanceof TextPart text) {
            assertThat(text.run().attributes()).containsEntry("weight", "heavy");
        }
    }

    @Test void countsEarlierObjectsAndSortsReferences() {
        var layout = builder.build(reader.read("<node><rich_text>ABC</rich_text></node>"), List.of(object(ObjectKind.TABLE, 3), object(ObjectKind.IMAGE, 1)));
        assertThat(sequence(layout)).isEqualTo("A[IMAGE]B[TABLE]C");
    }

    @Test void keepsSurrogatePairsIntact() {
        var layout = builder.build(reader.read("<node><rich_text>A😀B</rich_text></node>"), List.of(object(ObjectKind.CODEBOX, 2)));
        assertThat(sequence(layout)).isEqualTo("A😀[CODEBOX]B");
    }

    @Test void allowsConsecutiveObjectsAndBothBufferEdges() {
        var layout = builder.build(reader.read("<node><rich_text>X</rich_text></node>"), List.of(object(ObjectKind.ANCHOR, 0), object(ObjectKind.ATTACHMENT, 1), object(ObjectKind.TABLE, 3)));
        assertThat(sequence(layout)).isEqualTo("[ANCHOR][ATTACHMENT]X[TABLE]");
    }

    @Test void preservesEmptyRunsAtBoundaries() {
        var layout = builder.build(reader.read("<node><rich_text/><rich_text>A</rich_text><rich_text/></node>"), List.of(object(ObjectKind.IMAGE, 0)));
        assertThat(sequence(layout)).isEqualTo("[IMAGE]A");
        assertThat(layout.parts().stream().filter(p -> p instanceof TextPart t && t.run().text().isEmpty()).count()).isEqualTo(2);
    }

    @Test void supportsObjectsWithoutText() {
        assertThat(sequence(builder.build(reader.read("<node/>"), List.of(object(ObjectKind.IMAGE, 0), object(ObjectKind.ANCHOR, 1)))))
                .isEqualTo("[IMAGE][ANCHOR]");
    }

    @Test void rejectsConflictsOutOfRangeAndMixedNodes() {
        var document = reader.read("<node><rich_text>A</rich_text></node>");
        for (var objects : List.of(List.of(object(ObjectKind.IMAGE, 0), object(ObjectKind.TABLE, 0)),
                List.of(object(ObjectKind.IMAGE, 2)), List.of(object(ObjectKind.IMAGE, 0), new EmbeddedObject(ObjectKind.TABLE, 54, 1)))) {
            assertThatThrownBy(() -> builder.build(document, objects)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void placesAllObjectsFromDemoNode53AndPreservesText() throws Exception {
        try (var input = getClass().getResourceAsStream("/richtext/demo-node-53.xml")) {
            assertThat(input).isNotNull();
            var document = reader.read(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            var objects = new java.util.ArrayList<EmbeddedObject>();
            // IMAGE is a placeholder kind here: this test checks image-table offsets, not row classification.
            for (int offset : new int[]{454, 909, 918, 927, 936, 945, 1334, 1580, 1912, 1968, 2034, 2158}) {
                objects.add(object(ObjectKind.IMAGE, offset));
            }
            objects.add(object(ObjectKind.CODEBOX, 2205));
            objects.add(object(ObjectKind.TABLE, 2253));
            var layout = builder.build(document, objects);
            assertThat(layout.parts().stream().filter(p -> p instanceof ObjectPart).count()).isEqualTo(14);
            String before = document.runs().stream().map(RichTextDocument.TextRun::text).collect(java.util.stream.Collectors.joining());
            String after = layout.parts().stream().filter(p -> p instanceof TextPart)
                    .map(p -> ((TextPart) p).run().text()).collect(java.util.stream.Collectors.joining());
            assertThat(after).isEqualTo(before);
        }
    }

}

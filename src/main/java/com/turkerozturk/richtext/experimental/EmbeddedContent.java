package com.turkerozturk.richtext.experimental;

import java.util.List;
import java.util.Map;

/** Immutable payload snapshots, separate from positions and from existing JPA entities. */
public sealed interface EmbeddedContent {
    record PngImage(byte[] bytes, String link, String justification) implements EmbeddedContent {
        public PngImage { bytes = bytes.clone(); }
        @Override public byte[] bytes() { return bytes.clone(); }
    }
    record Attachment(String filename) implements EmbeddedContent { }
    record Anchor(String name) implements EmbeddedContent { }
    record CodeBox(String text, String syntax) implements EmbeddedContent { }
    /** Rows remain in storage order: CherryTree stores its header row last. */
    record Table(List<List<String>> rows, Map<String, String> attributes) implements EmbeddedContent {
        public Table {
            rows = rows.stream().map(List::copyOf).toList();
            attributes = Map.copyOf(attributes);
        }
    }
}

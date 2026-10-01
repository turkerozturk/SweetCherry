package com.turkerozturk.richtext.experimental;

import java.util.List;
import java.util.Map;

/** Text-only XML model. Offsets exclude embedded CTB objects and count Unicode code points. */
public record RichTextDocument(List<TextRun> runs) {
    public RichTextDocument {
        runs = List.copyOf(runs);
    }

    public record TextRun(String text, Map<String, String> attributes, int textOffset) {
        public TextRun {
            attributes = Map.copyOf(attributes);
        }

        public int characterCount() {
            return text.codePointCount(0, text.length());
        }
    }
}

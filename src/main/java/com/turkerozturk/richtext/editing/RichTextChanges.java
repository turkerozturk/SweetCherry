package com.turkerozturk.richtext.editing;

import com.turkerozturk.richtext.experimental.RichTextDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class RichTextChanges {
    private RichTextChanges() { }
    private record Segment(String text, Map<String,String> attributes) { }

    static boolean sameText(RichTextDocument left, RichTextDocument right) {
        return normalized(left).equals(normalized(right));
    }
    private static List<Segment> normalized(RichTextDocument document) {
        var result = new ArrayList<Segment>();
        StringBuilder text = new StringBuilder();
        Map<String,String> attributes = null;
        for (var run : document.runs()) {
            if (run.text().isEmpty()) continue;
            if (!run.attributes().equals(attributes)) {
                if (attributes != null) result.add(new Segment(text.toString(),attributes));
                text.setLength(0); attributes = run.attributes();
            }
            text.append(run.text());
        }
        if (attributes != null) result.add(new Segment(text.toString(),attributes));
        return result;
    }
}

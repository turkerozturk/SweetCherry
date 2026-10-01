package com.turkerozturk.richtext.experimental;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.turkerozturk.richtext.experimental.RichTextLayout.*;

/** Converts CTB final-buffer offsets into text-only positions before splitting formatted runs. */
public final class RichTextLayoutBuilder {
    /** Sorts objects, validates their positions and preserves empty runs and formatting on split text. */
    public RichTextLayout build(RichTextDocument document, List<EmbeddedObject> objects) {
        var sorted = new ArrayList<>(objects);
        sorted.sort(Comparator.comparingInt(EmbeddedObject::bufferOffset));
        int textLength = 0;
        for (var run : document.runs()) {
            if (run.textOffset() != textLength) throw new IllegalArgumentException("Non-contiguous text runs");
            textLength = Math.addExact(textLength, run.characterCount());
        }
        for (int i = 0; i < sorted.size(); i++) {
            var object = sorted.get(i);
            if (i > 0 && object.bufferOffset() == sorted.get(i - 1).bufferOffset()) {
                throw new IllegalArgumentException("Conflicting objects at buffer offset " + object.bufferOffset());
            }
            int textPosition = object.bufferOffset() - i;
            if (textPosition < 0 || textPosition > textLength) {
                throw new IllegalArgumentException("Object offset is outside the text buffer");
            }
            if (i > 0 && object.nodeId() != sorted.get(0).nodeId()) {
                throw new IllegalArgumentException("Objects belong to different nodes");
            }
        }
        var parts = new ArrayList<Part>();
        int next = 0;
        for (var run : document.runs()) {
            int start = run.textOffset();
            int end = start + run.characterCount();
            int cursor = start;
            if (run.text().isEmpty()) parts.add(new TextPart(run));
            while (next < sorted.size() && sorted.get(next).bufferOffset() - next <= end) {
                int position = sorted.get(next).bufferOffset() - next;
                if (position > cursor) parts.add(new TextPart(slice(run, cursor, position)));
                parts.add(new ObjectPart(sorted.get(next++)));
                cursor = position;
            }
            if (cursor < end) parts.add(new TextPart(slice(run, cursor, end)));
        }
        while (next < sorted.size()) parts.add(new ObjectPart(sorted.get(next++)));
        return new RichTextLayout(parts);
    }

    /** Uses code-point boundaries so splitting cannot cut a UTF-16 surrogate pair in half. */
    private RichTextDocument.TextRun slice(RichTextDocument.TextRun run, int start, int end) {
        int from = run.text().offsetByCodePoints(0, start - run.textOffset());
        int to = run.text().offsetByCodePoints(0, end - run.textOffset());
        return new RichTextDocument.TextRun(run.text().substring(from, to), run.attributes(), start);
    }
}

package com.turkerozturk.richtext.editing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.turkerozturk.richtext.experimental.*;
import com.turkerozturk.richtext.experimental.RichTextLayout.*;

/** Keeps CTB objects as immutable editor slots while allowing surrounding text to change. */
public final class ProtectedRichTextCodec {
    public static final String OBJECT_ATTRIBUTE = "__sweet_object";
    public static final String OBJECT_CHARACTER = "\uFFFC";
    public record Reference(String table, int offset) {
        public Reference {
            if (!List.of("image", "grid", "codebox").contains(table) || offset < 0) throw new IllegalArgumentException("Invalid object reference");
        }
        public String key() { return table + ":" + offset; }
    }
    public record Saved(RichTextDocument text, Map<Reference, Integer> offsets) { }

    /** Inserts one protected character per final-buffer object without losing formatted text. */
    public RichTextDocument open(RichTextDocument document, List<Reference> references) {
        for (var run : document.runs()) if (run.attributes().containsKey(OBJECT_ATTRIBUTE)) throw new IllegalArgumentException("Reserved editor attribute");
        var objects = references.stream().map(ref -> new EmbeddedObject(kind(ref.table()), 1, ref.offset())).toList();
        var layout = new RichTextLayoutBuilder().build(document, objects);
        var result = new ArrayList<RichTextDocument.TextRun>();
        int offset = 0;
        for (var part : layout.parts()) {
            String text; Map<String, String> attributes;
            if (part instanceof TextPart run) { text = run.run().text(); attributes = run.run().attributes(); }
            else {
                var object = ((ObjectPart) part).object();
                String table = object.kind() == ObjectKind.TABLE ? "grid" : object.kind() == ObjectKind.CODEBOX ? "codebox" : "image";
                text = OBJECT_CHARACTER; attributes = Map.of(OBJECT_ATTRIBUTE, table + ":" + object.bufferOffset());
            }
            result.add(new RichTextDocument.TextRun(text, attributes, offset));
            offset += text.codePointCount(0, text.length());
        }
        return new RichTextDocument(result);
    }

    /** Requires every original object exactly once and in order; returns text XML and new buffer offsets. */
    public Saved save(RichTextDocument edited, List<Reference> references) {
        var expected = references.stream().sorted(java.util.Comparator.comparingInt(Reference::offset)).toList();
        var text = new ArrayList<RichTextDocument.TextRun>();
        var offsets = new LinkedHashMap<Reference, Integer>();
        int bufferOffset = 0, textOffset = 0, next = 0;
        for (var run : edited.runs()) {
            String key = run.attributes().get(OBJECT_ATTRIBUTE);
            if (key != null) {
                if (next >= expected.size() || !expected.get(next).key().equals(key)
                        || !OBJECT_CHARACTER.equals(run.text()) || run.attributes().size() != 1) {
                    throw new IllegalArgumentException("Protected object changed, removed or reordered");
                }
                offsets.put(expected.get(next++), bufferOffset++);
            } else {
                text.add(new RichTextDocument.TextRun(run.text(), run.attributes(), textOffset));
                int length = run.characterCount(); textOffset += length; bufferOffset += length;
            }
        }
        if (next != expected.size()) throw new IllegalArgumentException("Protected objects are missing");
        return new Saved(new RichTextDocument(text), Map.copyOf(offsets));
    }

    private ObjectKind kind(String table) {
        return table.equals("grid") ? ObjectKind.TABLE : table.equals("codebox") ? ObjectKind.CODEBOX : ObjectKind.IMAGE;
    }
}

package com.turkerozturk.richtext.experimental;

import java.util.List;

/** Ordered text and object slots; each object occupies one final-buffer character position. */
public record RichTextLayout(List<Part> parts) {
    public RichTextLayout { parts = List.copyOf(parts); }

    public sealed interface Part permits TextPart, ObjectPart { }
    public record TextPart(RichTextDocument.TextRun run) implements Part { }
    public record ObjectPart(EmbeddedObject object) implements Part { }

    public enum ObjectKind { IMAGE, ATTACHMENT, ANCHOR, CODEBOX, TABLE }

    /** Reference to an independently browsable CTB object, without loading or copying its payload. */
    public record EmbeddedObject(ObjectKind kind, long nodeId, int bufferOffset) {
        public EmbeddedObject {
            if (kind == null || nodeId < 1 || bufferOffset < 0) {
                throw new IllegalArgumentException("Invalid embedded-object reference");
            }
        }
    }
}

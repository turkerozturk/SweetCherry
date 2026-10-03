package com.turkerozturk.richtext.editing;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Validates explicit deletion requests against the original node's protected objects. */
final class RichTextObjectDeletions {
    private RichTextObjectDeletions() { }

    /** Only unlocked, unique references belonging to this editor revision may be deleted. */
    static Set<String> decode(String json, boolean unlocked, List<ProtectedRichTextCodec.Reference> references) {
        try {
            if (json == null || json.length() > 64000) throw new IllegalArgumentException("Invalid deletion list");
            var array = new ObjectMapper().readTree(json);
            if (array == null || !array.isArray() || array.size() > 1000) throw new IllegalArgumentException("Invalid deletion list");
            var allowed = references.stream().map(ProtectedRichTextCodec.Reference::key).collect(java.util.stream.Collectors.toSet());
            var result = new HashSet<String>();
            for (var item : array) {
                if (!item.isTextual() || !allowed.contains(item.textValue()) || !result.add(item.textValue()))
                    throw new IllegalArgumentException("Unknown or duplicate object");
            }
            if (!result.isEmpty() && !unlocked) throw new IllegalArgumentException("Object deletion is locked");
            return Set.copyOf(result);
        } catch (java.io.IOException error) { throw new IllegalArgumentException("Invalid deletion JSON", error); }
    }
}

package com.turkerozturk.richtext.editing;

import java.util.Map;
import java.util.LinkedHashMap;

/** Validates local clipboard/file images and normalizes them to CTB PNG binary payloads. */
final class PendingRichTextImages {
    private PendingRichTextImages() { }

    /** Accepts supported rasters without an image-count cap; bounds encoded bytes, pixels and time. */
    static Map<String, byte[]> decode(String json) {
        return decode(json, 9_000_000);
    }

    /** Enforces the current tenant limit on both uploaded data and the normalized PNG representation. */
    static Map<String, byte[]> decode(String json, int limit) {
        try {
            if (json == null || json.length() > EmbeddedUploadPolicy.JSON_CHARACTERS) throw new IllegalArgumentException("Image upload too large");
            var root = tools.jackson.databind.json.JsonMapper.builder().build().readTree(json);
            if (root == null || !root.isObject()) throw new IllegalArgumentException("Invalid image list");
            var result = new LinkedHashMap<String, byte[]>();
            var fields = root.properties().iterator();
            long total=0,pixels=0,deadline=System.nanoTime()+30_000_000_000L;
            while (fields.hasNext()) {
                var field = fields.next();
                if (!field.getKey().matches("new-image:[a-zA-Z0-9-]{1,64}") || !field.getValue().isTextual())
                    throw new IllegalArgumentException("Invalid image key");
                String value = field.getValue().textValue();
                var png=RasterImageNormalizer.dataUrl(value,limit);
                total+=png.bytes().length;pixels+=png.pixels();
                if(total>EmbeddedUploadPolicy.TOTAL_BYTES||pixels>RasterImageNormalizer.TOTAL_PIXELS||System.nanoTime()>deadline)throw new IllegalArgumentException("Image batch too large");
                result.put(field.getKey(),png.bytes());
            }
            return Map.copyOf(result);
        } catch (tools.jackson.core.JacksonException error) { throw new IllegalArgumentException("Invalid image upload", error); }
    }
}

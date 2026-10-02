package com.turkerozturk.richtext.editing;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Decodes attachments without treating their content as HTML, an image or a filesystem path. */
final class PendingRichTextFiles {
    record File(String name, byte[] bytes) { }
    private PendingRichTextFiles() { }

    /** Preserves every binary byte and a safe basename, rejecting invalid payloads before database writes. */
    static Map<String, File> decode(String json, int limit) {
        try {
            if (json == null || json.length() > EmbeddedUploadPolicy.JSON_CHARACTERS) throw new IllegalArgumentException("File upload too large");
            var root = new ObjectMapper().readTree(json);
            if (root == null || !root.isObject() || root.size() > 10) throw new IllegalArgumentException("Invalid file list");
            var result = new LinkedHashMap<String, File>();
            long total = 0;
            var fields = root.fields();
            while (fields.hasNext()) {
                var field = fields.next(); var object = field.getValue();
                if (!field.getKey().matches("new-file:[a-zA-Z0-9-]{1,64}") || !object.isObject() || object.size() != 2
                        || !object.path("name").isTextual() || !object.path("data").isTextual()) throw new IllegalArgumentException("Invalid file data");
                String name = object.path("name").textValue().replace('\\', '/');
                name = name.substring(name.lastIndexOf('/') + 1);
                if (name.isBlank() || name.equals(".") || name.equals("..") || name.length() > 255
                        || name.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid filename");
                byte[] bytes = Base64.getDecoder().decode(object.path("data").textValue());
                total += bytes.length;
                if (bytes.length > limit || total > EmbeddedUploadPolicy.TOTAL_BYTES) throw new IllegalArgumentException("File upload too large");
                result.put(field.getKey(), new File(name, bytes));
            }
            return Map.copyOf(result);
        } catch (java.io.IOException error) { throw new IllegalArgumentException("Invalid file upload", error); }
    }
}

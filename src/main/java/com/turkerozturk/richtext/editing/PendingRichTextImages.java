package com.turkerozturk.richtext.editing;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Validates local clipboard/file images and normalizes them to CTB PNG binary payloads. */
final class PendingRichTextImages {
    private PendingRichTextImages() { }

    /** Accepts PNG/JPEG only, limiting total encoded data, image count and decoded pixel dimensions. */
    static Map<String, byte[]> decode(String json) {
        try {
            if (json == null || json.length() > 16_000_000) throw new IllegalArgumentException("Image upload too large");
            var root = new ObjectMapper().readTree(json);
            if (root == null || !root.isObject() || root.size() > 10) throw new IllegalArgumentException("Invalid image list");
            var result = new LinkedHashMap<String, byte[]>();
            var fields = root.fields();
            long total = 0;
            while (fields.hasNext()) {
                var field = fields.next();
                if (!field.getKey().matches("new-image:[a-zA-Z0-9-]{1,64}") || !field.getValue().isTextual())
                    throw new IllegalArgumentException("Invalid image key");
                String value = field.getValue().textValue();
                if (!value.startsWith("data:image/png;base64,") && !value.startsWith("data:image/jpeg;base64,"))
                    throw new IllegalArgumentException("Only PNG/JPEG images are accepted");
                byte[] bytes = Base64.getDecoder().decode(value.substring(value.indexOf(',') + 1));
                if (bytes.length > 8_000_000) throw new IllegalArgumentException("Image upload too large");
                try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                    var readers = ImageIO.getImageReaders(stream);
                    if (!readers.hasNext()) throw new IllegalArgumentException("Invalid image data");
                    var reader = readers.next();
                    try {
                        reader.setInput(stream);
                        String format = reader.getFormatName();
                        if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) throw new IllegalArgumentException("Unsupported image format");
                        int width = reader.getWidth(0), height = reader.getHeight(0);
                        if (width < 1 || height < 1 || width > 8192 || height > 8192 || (long) width * height > 16_000_000)
                            throw new IllegalArgumentException("Image dimensions too large");
                        var output = new ByteArrayOutputStream();
                        if (!ImageIO.write(reader.read(0), "png", output)) throw new IllegalArgumentException("PNG encoding failed");
                        byte[] png = output.toByteArray(); total += png.length;
                        if (total > 12_000_000) throw new IllegalArgumentException("Image upload too large");
                        result.put(field.getKey(), png);
                    } finally { reader.dispose(); }
                }
            }
            return Map.copyOf(result);
        } catch (java.io.IOException error) { throw new IllegalArgumentException("Invalid image upload", error); }
    }
}

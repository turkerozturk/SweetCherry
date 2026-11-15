package com.turkerozturk.image.gallery;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/** Bounded raster decoding; no SVG/HTML or remote URL is interpreted. */
final class GalleryPreview {
    private GalleryPreview() { }
    static String extension(byte[] bytes) {
        if (bytes != null && bytes.length >= 12) {
            if (bytes[0] == (byte)137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71) return "png";
            if (bytes[0] == (byte)255 && bytes[1] == (byte)216 && bytes[2] == (byte)255) return "jpg";
            if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') return "gif";
            if (bytes[0] == 'B' && bytes[1] == 'M') return "bmp";
            if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[8] == 'W' && bytes[9] == 'E') return "webp";
            if (bytes[0] == 'I' && bytes[1] == 'I' || bytes[0] == 'M' && bytes[1] == 'M') return "tif";
        }
        return "bin";
    }
    static byte[] png(byte[] bytes, int edge) {
        if (bytes == null || bytes.length == 0) throw unavailable();
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw unavailable();
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 100000000L) throw unavailable();
                int sample = Math.max(1, Math.max(width, height) / edge);
                var parameters = reader.getDefaultReadParam();
                parameters.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage decoded = reader.read(0, parameters);
                double scale = Math.min(1d, (double) edge / Math.max(decoded.getWidth(), decoded.getHeight()));
                var result = new BufferedImage(Math.max(1, (int) (decoded.getWidth() * scale)),
                        Math.max(1, (int) (decoded.getHeight() * scale)), BufferedImage.TYPE_INT_ARGB);
                var g = result.createGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(decoded, 0, 0, result.getWidth(), result.getHeight(), null);
                } finally { g.dispose(); decoded.flush(); }
                try (var output = new ByteArrayOutputStream()) {
                    ImageIO.write(result, "png", output);
                    return output.toByteArray();
                } finally { result.flush(); }
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException invalid) { throw unavailable(); }
    }
    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}

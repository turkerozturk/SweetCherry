package com.turkerozturk.richtext.editing;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PendingRichTextImagesTest {
    @Test void normalizesPngAndJpegToReadablePng() throws Exception {
        var image = new java.awt.image.BufferedImage(2, 3, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (String format : java.util.List.of("png", "jpeg")) {
            var out = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(image, format, out);
            String data = "data:image/" + format + ";base64," + java.util.Base64.getEncoder().encodeToString(out.toByteArray());
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("new-image:test", data));
            byte[] png = PendingRichTextImages.decode(json).get("new-image:test");
            assertThat(png[0]).isEqualTo((byte) 137);
            var decoded = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
            assertThat(decoded.getWidth()).isEqualTo(2); assertThat(decoded.getHeight()).isEqualTo(3);
        }
    }
    @Test void rejectsRemoteSvgAndInvalidBinaryPayloads() {
        for (String value : java.util.List.of("https://example.org/a.png", "data:image/svg+xml;base64,PHN2Zy8+", "data:image/png;base64,AQID")) {
            assertThatThrownBy(() -> PendingRichTextImages.decode("{\"new-image:test\":\"" + value + "\"}")).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void rejectsInvalidShapeKeysAndOversizedRequests() {
        for (String json : java.util.List.of("[]", "null", "{\"image:0\":\"x\"}", " ".repeat(30_000_001))) {
            assertThatThrownBy(() -> PendingRichTextImages.decode(json)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(PendingRichTextImages.decode("{}")).isEmpty();
    }
    @Test void enforcesTenantLimitForImageData() throws Exception {
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
        String data = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(out.toByteArray());
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("new-image:test", data));
        assertThatThrownBy(() -> PendingRichTextImages.decode(json, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(PendingRichTextImages.decode(json, 10000)).hasSize(1);
    }
}

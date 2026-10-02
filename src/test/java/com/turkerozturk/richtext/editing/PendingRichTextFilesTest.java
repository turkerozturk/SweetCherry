package com.turkerozturk.richtext.editing;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PendingRichTextFilesTest {
    private String json(String name, byte[] bytes) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("new-file:test", java.util.Map.of("name", name, "data", java.util.Base64.getEncoder().encodeToString(bytes))));
    }
    @Test void preservesBinaryAndUnicodeBasename() throws Exception {
        byte[] original = {0, 1, (byte) 255, (byte) 137};
        var file = PendingRichTextFiles.decode(json("C:\\notes\\Türkçe dosya.pdf", original), 4).get("new-file:test");
        assertThat(file.name()).isEqualTo("Türkçe dosya.pdf"); assertThat(file.bytes()).isEqualTo(original);
    }
    @Test void enforcesTenantLimitAndAcceptsEmptyAttachments() throws Exception {
        String data = json("a.bin", new byte[]{1,2,3});
        assertThatThrownBy(() -> PendingRichTextFiles.decode(data, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThat(PendingRichTextFiles.decode(data, 3)).hasSize(1);
        assertThat(PendingRichTextFiles.decode(json("empty.bin", new byte[0]), 1).get("new-file:test").bytes()).isEmpty();
    }
    @Test void rejectsControlNamesMalformedShapesAndInvalidBase64() throws Exception {
        String badName = json("line\nname.txt", new byte[]{1});
        for (String value : java.util.List.of("[]", "{\"new-file:test\":{\"name\":\"a\",\"data\":\"!\"}}", "{\"image:0\":{}}", badName)) {
            assertThatThrownBy(() -> PendingRichTextFiles.decode(value, 100)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

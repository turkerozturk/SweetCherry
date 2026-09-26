package com.turkerozturk.upload;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceTest {
    @Test void rejectsTraversalAndUnexpectedExtensionsBeforeWriting() {
        StorageService storage = new StorageService();
        for (String name : new String[]{"../outside.txt", "..\\outside.txt", "tenant.properties", "a.txt:stream"}) {
            MockMultipartFile upload = new MockMultipartFile("dosya", name, "text/plain", new byte[]{1});
            assertThatThrownBy(() -> storage.uploadImageToFileSystem(upload))
                    .as(name).isInstanceOf(java.io.IOException.class);
        }
    }
}

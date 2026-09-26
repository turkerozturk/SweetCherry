package com.turkerozturk.upload;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UploadControllerTest {
    @Test void successfulUploadOffersReloadAndGetRedirectsToForm() throws Exception {
        StorageService storage = mock(StorageService.class);
        UploadController controller = new UploadController();
        ReflectionTestUtils.setField(controller, "storageService", storage);
        MockMultipartFile file = new MockMultipartFile("dosya", "demo.txt", "text/plain", new byte[]{1});
        when(storage.uploadImageToFileSystem(file)).thenReturn("upload process completed");
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.uploadDatabaseGet()).isEqualTo("redirect:/upload-form");
        assertThat(controller.uploadImageToFileSystem(file, model)).isEqualTo("upload");
        assertThat(model.get("uploadSucceeded")).isEqualTo(true);
    }
}

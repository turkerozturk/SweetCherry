package com.turkerozturk.image;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class ImageMetadataTest {
    @Test void anchorWithoutBinaryContentCanBeInitialized() {
        var image = new Image();
        image.setAnchor("section");
        ReflectionTestUtils.invokeMethod(image, "doAfterInitialization");
        assertThat(image.getFileSize()).isZero();
        assertThat(image.getPng()).isNull();
    }

    @Test void binaryAttachmentStillReportsActualSizeAndExtension() {
        var image = new Image();
        image.setPng(new byte[]{1, 2, 3});
        image.setFileName("notes.pdf");
        ReflectionTestUtils.invokeMethod(image, "doAfterInitialization");
        assertThat(image.getFileSize()).isEqualTo(3);
        assertThat(image.getFileExtension()).isEqualTo("pdf");
    }
}

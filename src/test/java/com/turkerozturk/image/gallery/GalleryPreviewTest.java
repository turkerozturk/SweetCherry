package com.turkerozturk.image.gallery;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import static org.assertj.core.api.Assertions.*;

class GalleryPreviewTest {
    static byte[] picture() throws Exception {
        var image=new BufferedImage(1600,800,BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,0x00FFFFFF);
        var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();
    }
    @Test void createsBoundedPngThumbnailsAndLargePreviewsWithoutChangingTheOriginal() throws Exception {
        var original=picture();var copy=original.clone();
        var thumb=ImageIO.read(new ByteArrayInputStream(GalleryPreview.png(original,320)));
        assertThat(thumb.getWidth()).isEqualTo(320);assertThat(thumb.getHeight()).isEqualTo(160);
        var large=ImageIO.read(new ByteArrayInputStream(GalleryPreview.png(original,2560)));
        assertThat(large.getWidth()).isEqualTo(1600);assertThat(original).isEqualTo(copy);
        assertThat(GalleryPreview.extension(original)).isEqualTo("png");
    }
    @Test void refusesActiveOrDamagedDataInsteadOfReturningItAsAnImage() {
        for(byte[] bytes:new byte[][]{new byte[0],"<svg onload='evil()'/>".getBytes(),"<script>evil()</script>".getBytes()})
            assertThatThrownBy(()->GalleryPreview.png(bytes,320)).hasMessageContaining("422");
    }
}

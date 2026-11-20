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
    @Test void reportsOriginalDimensionsFormatColorDepthAndAlphaWithoutThumbnailDimensions()throws Exception{
        var info=GalleryPreview.info(picture());assertThat(info.width()).isEqualTo(1600);assertThat(info.height()).isEqualTo(800);
        assertThat(info.format()).isEqualTo("PNG");assertThat(info.bits()).isEqualTo(32);assertThat(info.alpha()).isTrue();
        var rgb=new BufferedImage(80,40,BufferedImage.TYPE_INT_RGB);var out=new ByteArrayOutputStream();ImageIO.write(rgb,"jpeg",out);
        var jpeg=GalleryPreview.info(out.toByteArray());assertThat(jpeg.format()).isEqualTo("JPEG");assertThat(jpeg.bits()).isEqualTo(24);assertThat(jpeg.alpha()).isFalse();
        assertThat(jpeg.width()).isEqualTo(80);assertThat(jpeg.height()).isEqualTo(40);
    }
    @Test void missingAndDamagedMetadataAreRejected(){
        assertThatThrownBy(()->GalleryPreview.info(null)).hasMessageContaining("422");
        assertThatThrownBy(()->GalleryPreview.info("<svg onload='evil()'/>".getBytes())).hasMessageContaining("422");
    }
    @Test void refusesActiveOrDamagedDataInsteadOfReturningItAsAnImage() {
        for(byte[] bytes:new byte[][]{new byte[0],"<svg onload='evil()'/>".getBytes(),"<script>evil()</script>".getBytes()})
            assertThatThrownBy(()->GalleryPreview.png(bytes,320)).hasMessageContaining("422");
    }
}

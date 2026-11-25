package com.turkerozturk.richtext.editing;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import static org.assertj.core.api.Assertions.*;

class RasterImageNormalizerTest {
    @Test void webpLossyLosslessAndAnimationAndGifBecomeStaticPng()throws Exception {
        for(String name:List.of("lossy.webp","lossless.webp","animated.webp","animated.gif")) {
            byte[] bytes;try(var source=getClass().getResourceAsStream("/fixtures/raster-import/"+name)){bytes=source.readAllBytes();}
            var png=RasterImageNormalizer.normalize(bytes,1000000);
            assertThat(png.bytes()).startsWith((byte)137,(byte)80,(byte)78,(byte)71);
            var image=ImageIO.read(new ByteArrayInputStream(png.bytes()));
            assertThat(image.getWidth()).isEqualTo(3);assertThat(image.getHeight()).isEqualTo(2);
            if(name.startsWith("animated"))assertThat(image.getRGB(0,0)&0xffffff).isEqualTo(0xff0000);
        }
    }
    @Test void jdkRasterFormatsAndJpgMimeAliasAreAccepted()throws Exception {
        for(String format:List.of("png","jpeg","gif","bmp","tiff","wbmp")) {
            var source=new BufferedImage(2,3,format.equals("wbmp")?BufferedImage.TYPE_BYTE_BINARY:BufferedImage.TYPE_INT_RGB);
            var out=new ByteArrayOutputStream();assertThat(ImageIO.write(source,format,out)).isTrue();
            String mime=format.equals("jpeg")?"image/jpg":format.equals("wbmp")?"image/vnd.wap.wbmp":"image/"+format;
            var png=RasterImageNormalizer.dataUrl("data:"+mime+";base64,"+Base64.getEncoder().encodeToString(out.toByteArray()),1000000);
            assertThat(ImageIO.read(new ByteArrayInputStream(png.bytes())).getHeight()).isEqualTo(3);
        }
    }
    @Test void svgAvifMalformedAndOversizedPixelsAndOutputAreRejected()throws Exception {
        for(String value:List.of("https://example.org/image.webp","data:image/svg+xml;base64,PHN2Zy8+","data:image/avif;base64,AQID","data:image/webp;base64,AQID"))
            assertThatThrownBy(()->RasterImageNormalizer.dataUrl(value,1000000)).isInstanceOf(IllegalArgumentException.class);
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8193,1,BufferedImage.TYPE_INT_RGB),"png",out);
        assertThatThrownBy(()->RasterImageNormalizer.normalize(out.toByteArray(),1000000)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("dimensions");
        assertThatThrownBy(()->RasterImageNormalizer.normalize(new byte[100],10)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void convertedPngMustAlsoFitThePerFileLimit()throws Exception {
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB),"gif",out);
        byte[] gif=out.toByteArray();assertThatThrownBy(()->RasterImageNormalizer.normalize(gif,gif.length+1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void pendingImagesCanContainMoreThanTenSmallImages()throws Exception {
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"gif",out);
        var values=new LinkedHashMap<String,String>();for(int n=0;n<40;n++)values.put("new-image:i"+n,"data:image/gif;base64,"+Base64.getEncoder().encodeToString(out.toByteArray()));
        var json=tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(values);
        assertThat(PendingRichTextImages.decode(json)).hasSize(40);
    }
}

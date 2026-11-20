package com.turkerozturk.image.gallery;

import java.io.*;
import java.nio.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AnnotationRasterTest {
    static byte[] picture(String format)throws Exception{
        var image=new BufferedImage(20,10,BufferedImage.TYPE_INT_RGB);image.setRGB(0,0,0xffe01b24);
        var out=new ByteArrayOutputStream();assertThat(ImageIO.write(image,format,out)).isTrue();return out.toByteArray();
    }
    static byte[] oriented(byte[] jpeg,int orientation)throws Exception{
        var exif=ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);exif.put(new byte[]{'E','x','i','f',0,0,'I','I'});exif.putShort((short)42).putInt(8).putShort((short)1).putShort((short)0x112).putShort((short)3).putInt(1).putShort((short)orientation).putShort((short)0).putInt(0);
        var out=new ByteArrayOutputStream();out.write(jpeg,0,2);out.write(new byte[]{(byte)255,(byte)225,0,34});out.write(exif.array());out.write(jpeg,2,jpeg.length-2);return out.toByteArray();
    }
    @Test void jpegAndBmpBecomeLosslessPngAtTheirOriginalDimensions()throws Exception{
        for(String format:new String[]{"jpeg","bmp"}){
            byte[] source=picture(format),png=AnnotationRaster.png(source);assertThat(ImageAnnotationService.dimensions(png)).containsExactly(20,10);
            var original=ImageIO.read(new ByteArrayInputStream(source));var converted=ImageIO.read(new ByteArrayInputStream(png));
            for(int y=0;y<10;y++)for(int x=0;x<20;x++)assertThat(converted.getRGB(x,y)).isEqualTo(original.getRGB(x,y));
        }
    }
    @Test void allExifOrientationsAreAppliedWithoutDiscardingOrChangingPixels()throws Exception{
        byte[] jpeg=picture("jpeg");var original=ImageIO.read(new ByteArrayInputStream(jpeg));
        var originalPixels=new java.util.ArrayList<Integer>();for(int y=0;y<10;y++)for(int x=0;x<20;x++)originalPixels.add(original.getRGB(x,y));
        for(int orientation=1;orientation<=8;orientation++){
            byte[] source=oriented(jpeg,orientation);assertThat(AnnotationRaster.orientation(source)).isEqualTo(orientation);
            assertThat(AnnotationRaster.dimensions(source)).containsExactly(orientation>=5?10:20,orientation>=5?20:10);
            var output=ImageIO.read(new ByteArrayInputStream(AnnotationRaster.png(source)));var pixels=new java.util.ArrayList<Integer>();
            for(int y=0;y<output.getHeight();y++)for(int x=0;x<output.getWidth();x++)pixels.add(output.getRGB(x,y));assertThat(pixels).containsExactlyInAnyOrderElementsOf(originalPixels);
            int x=switch(orientation){case 2,3->19;case 6,7->9;default->0;};int y=switch(orientation){case 3,4->9;case 7,8->19;default->0;};assertThat(output.getRGB(x,y)).isEqualTo(original.getRGB(0,0));
        }
    }
    @Test void sourceTypesAndExtensionsAreExplicitAndMalformedExifIsBounded()throws Exception{
        assertThat(AnnotationRaster.filename("photo.JPEG")).isTrue();assertThat(AnnotationRaster.filename("photo.BMP")).isTrue();assertThat(AnnotationRaster.filename("")).isTrue();
        for(String name:new String[]{"animation.gif","image.webp","graphic.svg","file.docx"})assertThat(AnnotationRaster.filename(name)).isFalse();
        assertThat(AnnotationRaster.outputName("folder.photo.JPEG")).isEqualTo("folder.photo.png");assertThat(AnnotationRaster.outputName("image.PNG")).isEqualTo("image.PNG");
        for(int length=0;length<60;length++){byte[] bytes=new byte[length];java.util.Arrays.fill(bytes,(byte)255);if(length>1)bytes[1]=(byte)216;assertThat(AnnotationRaster.orientation(bytes)).isBetween(1,8);}
        assertThatThrownBy(()->AnnotationRaster.png(new byte[30])).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var gif=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB),"gif",gif);assertThatThrownBy(()->AnnotationRaster.png(gif.toByteArray())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}

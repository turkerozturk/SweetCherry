package com.turkerozturk.image.attachments.drawio;

import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class DrawioLocalImagesTest {
    @TempDir Path temp;
    private Path png(Path path)throws Exception{ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",path.toFile());return path;}
    private void manifest(Path root,Properties values)throws Exception{try(var writer=Files.newBufferedWriter(root.resolve("images.properties"),java.nio.charset.StandardCharsets.UTF_8)){values.store(writer,"Local raster replacements");}}
    @Test void administratorMappedPngAndJpegAreEmbeddedWithoutFetchingAnything()throws Exception{
        png(temp.resolve("server.png"));ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"jpeg",temp.resolve("photo.jpg").toFile());
        var properties=new Properties();properties.setProperty("shape.mxgraph.local.server","server.png");String key="image."+DrawioDocument.imageKey("https://example.invalid/photo.png");properties.setProperty(key,"photo.jpg");manifest(temp,properties);
        var images=DrawioLocalImages.load(temp.toString());assertThat(images).hasSize(2);assertThat(images.get("shape.mxgraph.local.server")).startsWith("data:image/png;base64,");assertThat(images.get(key)).startsWith("data:image/jpeg;base64,");
        assertThatThrownBy(()->images.put("other","value")).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void traversalAbsolutePathsSvgAndSymlinkEscapesCannotExposeOtherFiles()throws Exception{
        Path root=Files.createDirectory(temp.resolve("root")),outside=png(temp.resolve("outside.png"));Files.writeString(root.resolve("bad.svg"),"<svg onload='evil()'/>");
        var properties=new Properties();properties.setProperty("shape.traversal","../outside.png");properties.setProperty("shape.absolute",outside.toString());properties.setProperty("shape.svg","bad.svg");
        try{Files.createSymbolicLink(root.resolve("escape.png"),outside);properties.setProperty("shape.symlink","escape.png");}catch(java.io.IOException|UnsupportedOperationException unavailable){}
        manifest(root,properties);assertThat(DrawioLocalImages.load(root.toString())).isEmpty();
    }
    @Test void malformedOversizedImagesAndInvalidKeysAreRejected()throws Exception{
        Files.writeString(temp.resolve("bad.png"),"not an image");Files.write(temp.resolve("large.png"),new byte[2097153]);png(temp.resolve("valid.png"));
        var properties=new Properties();properties.setProperty("shape.bad","bad.png");properties.setProperty("shape.large","large.png");properties.setProperty("https://example.invalid","valid.png");manifest(temp,properties);
        assertThat(DrawioLocalImages.load(temp.toString())).isEmpty();
    }
    @Test void disabledMissingAndOverfullDirectoriesFailClosed()throws Exception{
        assertThat(DrawioLocalImages.load("")).isEmpty();assertThat(DrawioLocalImages.load(temp.resolve("missing").toString())).isEmpty();
        var properties=new Properties();for(int n=0;n<129;n++)properties.setProperty("shape.item"+n,"a.png");manifest(temp,properties);assertThat(DrawioLocalImages.load(temp.toString())).isEmpty();
    }
}

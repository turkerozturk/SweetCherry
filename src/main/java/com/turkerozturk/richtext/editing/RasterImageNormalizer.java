package com.turkerozturk.richtext.editing;

import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Raster data only: decode the first frame and write fresh PNG without source metadata. */
final class RasterImageNormalizer {
    static final long TOTAL_PIXELS=64_000_000L;
    private static final Semaphore DECODERS=new Semaphore(2);
    private static final Set<String> FORMATS=Set.of("png","jpeg","jpg","gif","webp","bmp","tiff","tif","wbmp");
    static final Set<String> MIME_TYPES=Set.of("image/png","image/jpeg","image/jpg","image/gif","image/webp","image/bmp","image/x-bmp","image/x-ms-bmp","image/tiff","image/x-tiff","image/vnd.wap.wbmp");
    record Png(byte[] bytes,long pixels) { }
    private RasterImageNormalizer() { }
    static Png dataUrl(String value,int limit) {
        if(value==null||value.length()>(long)(limit+2)/3*4+100)throw new IllegalArgumentException("Image upload too large");
        int comma=value.indexOf(',');
        if(comma<0||!value.startsWith("data:")||!value.substring(0,comma).endsWith(";base64")
                || !MIME_TYPES.contains(value.substring(5,comma-7).toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Unsupported raster image");
        return normalize(Base64.getDecoder().decode(value.substring(comma+1)),limit);
    }
    static Png normalize(byte[] bytes,int limit) {
        if(bytes.length==0||bytes.length>limit)throw new IllegalArgumentException("Image upload too large");
        if(!DECODERS.tryAcquire())throw new IllegalArgumentException("Image decoder busy");
        try(var stream=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext())throw new IllegalArgumentException("Invalid raster image");
            var reader=readers.next();
            try {
                reader.setInput(stream,true,true);
                if(!FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Unsupported raster image");
                int width=reader.getWidth(0),height=reader.getHeight(0);long pixels=(long)width*height;
                if(width<1||height<1||width>8192||height>8192||pixels>16_000_000)throw new IllegalArgumentException("Image dimensions too large");
                var decoded=reader.read(0);
                if(decoded==null)throw new IllegalArgumentException("Invalid raster image");
                try {
                    var output=new ByteArrayOutputStream(){
                        @Override public synchronized void write(byte[] b,int off,int len){if((long)count+len>limit)throw new IllegalArgumentException("PNG too large");super.write(b,off,len);}
                        @Override public synchronized void write(int b){if(count>=limit)throw new IllegalArgumentException("PNG too large");super.write(b);}
                    };
                    if(!ImageIO.write(decoded,"png",output))throw new IllegalArgumentException("PNG encoding failed");
                    return new Png(output.toByteArray(),pixels);
                } finally {decoded.flush();}
            } finally {reader.dispose();}
        } catch(IllegalArgumentException error){throw error;}
        catch(IOException|RuntimeException error){throw new IllegalArgumentException("Invalid raster image",error);}
        finally {DECODERS.release();}
    }
}

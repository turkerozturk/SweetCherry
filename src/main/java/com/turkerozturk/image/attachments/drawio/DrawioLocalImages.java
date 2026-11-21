package com.turkerozturk.image.attachments.drawio;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Administrator-provided, restart-loaded raster replacements. Never fetches URLs. */
final class DrawioLocalImages {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(DrawioLocalImages.class);
    static Map<String,String> load(String directory){
        if(directory==null||directory.isBlank())return Map.of();
        var result=new LinkedHashMap<String,String>();long total=0,totalPixels=0;
        try{
            Path root=Path.of(directory).toRealPath(),manifest=root.resolve("images.properties").toRealPath();
            if(!Files.isDirectory(root)||!manifest.startsWith(root)||Files.size(manifest)>65536)throw new IOException("manifest");
            var properties=new Properties();try(var reader=Files.newBufferedReader(manifest,java.nio.charset.StandardCharsets.UTF_8)){properties.load(reader);}
            if(properties.size()>128)throw new IOException("entries");
            for(String key:new TreeSet<>(properties.stringPropertyNames())){
                if(!key.matches("(?:shape\\.[A-Za-z][A-Za-z0-9_.-]{0,199}|image\\.[a-f0-9]{64})")){LOG.warn("Draw.io local image: invalid mapping key rejected");continue;}
                try{
                    String name=properties.getProperty(key).strip();Path relative=Path.of(name);
                    if(relative.isAbsolute()||!name.toLowerCase(Locale.ROOT).matches(".{1,160}\\.(png|jpe?g)"))throw new IOException("name");
                    Path file=root.resolve(relative).normalize().toRealPath();
                    if(!file.startsWith(root)||!Files.isRegularFile(file))throw new IOException("path");
                    long length=Files.size(file);if(length<1||length>2097152||total+length>8388608)throw new IOException("bytes");
                    byte[] bytes;try(var input=Files.newInputStream(file)){bytes=input.readNBytes(2097153);}if(bytes.length>2097152||total+bytes.length>8388608)throw new IOException("bytes");
                    String mime;long pixels;
                    try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
                        var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new IOException("format");var reader=readers.next();
                        try{reader.setInput(stream);String format=reader.getFormatName().toLowerCase(Locale.ROOT);mime=switch(format){case "png"->"image/png";case "jpeg","jpg"->"image/jpeg";default->throw new IOException("format");};
                            long width=reader.getWidth(0),height=reader.getHeight(0);pixels=width*height;if(width<1||height<1||pixels>16000000||totalPixels+pixels>32000000)throw new IOException("pixels");
                        }finally{reader.dispose();}
                    }
                    result.put(key,"data:"+mime+";base64,"+Base64.getEncoder().encodeToString(bytes));total+=bytes.length;totalPixels+=pixels;
                }catch(Exception rejected){LOG.warn("Draw.io local image mapping rejected: {}",key);}
            }
        }catch(Exception rejected){LOG.warn("Draw.io local image directory/manifest unavailable or invalid; replacements disabled");}
        return Collections.unmodifiableMap(result);
    }
}

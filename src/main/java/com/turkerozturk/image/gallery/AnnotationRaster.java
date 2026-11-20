package com.turkerozturk.image.gallery;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Bounded static raster decoding; annotated output is always lossless PNG. */
final class AnnotationRaster {
    private AnnotationRaster(){}
    static boolean filename(String name){return name==null||name.isEmpty()||name.toLowerCase(Locale.ROOT).matches(".*\\.(png|jpe?g|bmp)");}
    static String outputName(String name){return name==null||name.isEmpty()||name.toLowerCase(Locale.ROOT).endsWith(".png")?name:name.substring(0,name.lastIndexOf('.'))+".png";}
    static int[] dimensions(byte[] data){
        int[] size=rawDimensions(data);int orientation=orientation(data);
        return orientation>=5?new int[]{size[1],size[0]}:size;
    }
    private static int[] rawDimensions(byte[] data){
        if(data==null||data.length<24||data.length>ImageAnnotationService.MAX_BYTES)throw invalid();
        if(data[0]==(byte)137&&data[1]==80)return ImageAnnotationService.dimensions(data);
        try(var stream=new MemoryCacheImageInputStream(new ByteArrayInputStream(data))){
            var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw invalid();var reader=readers.next();
            try{reader.setInput(stream,true,true);if(!Set.of("JPEG","JPG","BMP").contains(reader.getFormatName().toUpperCase(Locale.ROOT)))throw invalid();
                int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||w>8192||h>8192||(long)w*h>12_000_000)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);return new int[]{w,h};
            }finally{reader.dispose();}
        }catch(IOException e){throw invalid();}
    }
    static byte[] png(byte[] data){
        dimensions(data);
        if(data[0]==(byte)137&&data[1]==80)return data;
        try(var stream=new MemoryCacheImageInputStream(new ByteArrayInputStream(data))){
            var reader=ImageIO.getImageReaders(stream).next();
            try{reader.setInput(stream,true,true);var source=reader.read(0);if(source==null)throw invalid();
                BufferedImage image=orient(source,orientation(data));
                try(var output=new ByteArrayOutputStream()){
                    if(!ImageIO.write(image,"png",output))throw invalid();
                    if(output.size()>ImageAnnotationService.MAX_BYTES)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
                    return output.toByteArray();
                }finally{image.flush();if(image!=source)source.flush();}
            }finally{reader.dispose();}
        }catch(IOException e){throw invalid();}
    }
    // JPEG EXIF orientation is applied before canvas editing, including mirrored orientations.
    private static BufferedImage orient(BufferedImage image,int orientation){
        if(orientation==1)return image;
        int w=image.getWidth(),h=image.getHeight();var result=new BufferedImage(orientation>=5?h:w,orientation>=5?w:h,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int dx=x,dy=y;
            switch(orientation){case 2->dx=w-1-x;case 3->{dx=w-1-x;dy=h-1-y;}case 4->dy=h-1-y;case 5->{dx=y;dy=x;}case 6->{dx=h-1-y;dy=x;}case 7->{dx=h-1-y;dy=w-1-x;}case 8->{dx=y;dy=w-1-x;}default->{}}
            result.setRGB(dx,dy,image.getRGB(x,y));
        }
        return result;
    }
    static int orientation(byte[] data){
        if(data==null||data.length<4||data[0]!=(byte)255||data[1]!=(byte)216)return 1;
        for(int at=2;at+4<=data.length;){
            if(data[at++]!=(byte)255)return 1;int marker=Byte.toUnsignedInt(data[at++]);while(marker==255&&at<data.length)marker=Byte.toUnsignedInt(data[at++]);
            if(marker==0xda||marker==0xd9||at+2>data.length)return 1;
            int length=(Byte.toUnsignedInt(data[at])<<8)|Byte.toUnsignedInt(data[at+1]);
            if(length<2||(long)at+length>data.length)return 1;
            if(marker==0xe1&&length>=16&&data[at+2]=='E'&&data[at+3]=='x'&&data[at+4]=='i'&&data[at+5]=='f'&&data[at+6]==0&&data[at+7]==0){
                int start=at+8,end=at+length;var bytes=ByteBuffer.wrap(data);boolean little=data[start]=='I'&&data[start+1]=='I';
                if(!little&&!(data[start]=='M'&&data[start+1]=='M'))return 1;bytes.order(little?ByteOrder.LITTLE_ENDIAN:ByteOrder.BIG_ENDIAN);
                if(Short.toUnsignedInt(bytes.getShort(start+2))!=42)return 1;
                long directory=(long)start+Integer.toUnsignedLong(bytes.getInt(start+4));if(directory<start+8||directory+2>end)return 1;
                int count=Short.toUnsignedInt(bytes.getShort((int)directory));
                for(long entry=directory+2;count-->0&&entry+12<=end;entry+=12){int i=(int)entry;
                    if(Short.toUnsignedInt(bytes.getShort(i))==0x112&&Short.toUnsignedInt(bytes.getShort(i+2))==3&&bytes.getInt(i+4)==1){int value=Short.toUnsignedInt(bytes.getShort(i+8));return value>=1&&value<=8?value:1;}
                }
            }
            at+=length;
        }
        return 1;
    }
    private static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);}
}

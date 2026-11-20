package com.turkerozturk.image.gallery;

import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.*;
import java.io.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @PreAuthorize("hasRole('ADMIN')")
public class ImageAnnotationService {
    public static final int MAX_BYTES=16*1024*1024;
    private static final Set<String> HELPERS=Set.of("__sweetcherry_audit_v1__","__sweetcherry_trash_v1__","__sweetcherry_reminders_v1__");
    private final NodePropertiesService properties;
    @PersistenceContext private EntityManager manager;
    public ImageAnnotationService(NodePropertiesService properties){this.properties=properties;}
    public record Snapshot(String revision,int width,int height,String png) {}
    private record Stored(byte[] png,String revision,String text,String tags,String syntax,long flags,Long modified,String filename) {}
    private void guard(){if(!properties.writable())throw problem(HttpStatus.FORBIDDEN);}
    @Transactional(readOnly=true) public Snapshot open(long node,int offset){guard();return manager.unwrap(Session.class).doReturningWork(c->snapshot(read(c,node,offset,false)));}
    @Transactional public void save(long node,int offset,String revision,byte[] png,boolean confirmed){
        guard();if(!confirmed)throw problem(HttpStatus.BAD_REQUEST);
        byte[] clean=normalize(png);
        manager.unwrap(Session.class).doWork(c->save(c,node,offset,revision,clean));manager.clear();
    }
    static Snapshot open(Connection c,long node,int offset)throws SQLException{return snapshot(read(c,node,offset,false));}
    static void save(Connection c,long node,int offset,String revision,byte[] clean)throws SQLException{
        var old=read(c,node,offset,true);
        if(!old.revision().equals(revision))throw problem(HttpStatus.CONFLICT);
        if(!Arrays.equals(AnnotationRaster.dimensions(old.png()),dimensions(clean)))throw problem(HttpStatus.UNPROCESSABLE_ENTITY);
        // Acquire a write lock and recheck the node state before touching the image row.
        String sql="UPDATE node SET ts_lastsave=? WHERE node_id=? AND is_ro=?"
                +(old.text()==null?" AND txt IS NULL":" AND txt=?")+(old.tags()==null?" AND tags IS NULL":" AND tags=?")
                +(old.syntax()==null?" AND syntax IS NULL":" AND syntax=?")+(old.modified()==null?" AND ts_lastsave IS NULL":" AND ts_lastsave=?");
        try(var q=c.prepareStatement(sql)){
            int i=1;q.setLong(i++,com.turkerozturk.node.NodeTimestamps.now());q.setLong(i++,node);q.setLong(i++,old.flags());
            if(old.text()!=null)q.setString(i++,old.text());if(old.tags()!=null)q.setString(i++,old.tags());if(old.syntax()!=null)q.setString(i++,old.syntax());if(old.modified()!=null)q.setLong(i,old.modified());
            if(q.executeUpdate()!=1)throw problem(HttpStatus.CONFLICT);
        }
        // Binary compare-and-swap also protects against external replacements at the same offset.
        try(var q=c.prepareStatement("UPDATE image SET png=?,filename=? WHERE node_id=? AND "+quote(c)+"=? AND png=?")){
            q.setBytes(1,clean);q.setString(2,AnnotationRaster.outputName(old.filename()));q.setLong(3,node);q.setInt(4,offset);q.setBytes(5,old.png());
            if(q.executeUpdate()!=1)throw problem(HttpStatus.CONFLICT);
        }
    }
    private static String quote(Connection c)throws SQLException{return com.turkerozturk.jdbc.JdbcIdentifiers.quote(c,"offset");}
    private static Stored read(Connection c,long node,int offset,boolean lock)throws SQLException{
        if(node<=0||offset<0)throw problem(HttpStatus.BAD_REQUEST);
        String bytes=c.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")?"OCTET_LENGTH(i.png)":"LENGTH(i.png)";
        String suffix=lock&&!c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")?" FOR UPDATE":"";
        String from=" FROM image i JOIN node n ON n.node_id=i.node_id JOIN children c ON c.node_id=n.node_id AND COALESCE(c.master_id,0)=0 WHERE i.node_id=? AND i."+quote(c)+"=?";
        try(var q=c.prepareStatement("SELECT n.is_ro,n.tags,"+bytes+",i.filename,i.anchor"+from+suffix)){
            q.setLong(1,node);q.setInt(2,offset);try(var r=q.executeQuery()){
                if(!r.next())throw problem(HttpStatus.NOT_FOUND);
                if((r.getLong(1)&1)!=0||HELPERS.contains(Objects.requireNonNullElse(r.getString(2),"")))throw problem(HttpStatus.FORBIDDEN);
                if(r.getLong(3)>MAX_BYTES)throw problem(HttpStatus.PAYLOAD_TOO_LARGE);
                String name=Objects.requireNonNullElse(r.getString(4),"");
                if(!Objects.requireNonNullElse(r.getString(5),"").isEmpty()||!AnnotationRaster.filename(name))throw problem(HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
        try(var q=c.prepareStatement("SELECT i.png,n.txt,n.tags,n.syntax,n.is_ro,n.ts_lastsave,i.filename,i.anchor,i.justification,i.link,i.time"+from+" AND "+bytes+"<=?"+suffix)){
            q.setLong(1,node);q.setInt(2,offset);q.setInt(3,MAX_BYTES);try(var r=q.executeQuery()){
                if(!r.next())throw problem(HttpStatus.CONFLICT);
                byte[] png=r.getBytes(1);AnnotationRaster.dimensions(png);
                if((r.getLong(5)&1)!=0||HELPERS.contains(Objects.requireNonNullElse(r.getString(3),"")))throw problem(HttpStatus.FORBIDDEN);
                String filename=Objects.requireNonNullElse(r.getString(7),"");
                if(!Objects.requireNonNullElse(r.getString(8),"").isEmpty()||!AnnotationRaster.filename(filename))throw problem(HttpStatus.UNPROCESSABLE_ENTITY);
                var digest=digest();digest.update(java.nio.ByteBuffer.allocate(4).putInt(png.length).array());digest.update(png);
                field(digest,Long.toString(node));field(digest,Integer.toString(offset));
                for(int i=2;i<=11;i++)field(digest,r.getString(i));
                long stamp=r.getLong(6);Long modified=r.wasNull()?null:stamp;
                return new Stored(png,HexFormat.of().formatHex(digest.digest()),r.getString(2),r.getString(3),r.getString(4),r.getLong(5),modified,r.getString(7));
            }
        }
    }
    private static Snapshot snapshot(Stored value){byte[] png=AnnotationRaster.png(value.png());var size=dimensions(png);return new Snapshot(value.revision(),size[0],size[1],"data:image/png;base64,"+Base64.getEncoder().encodeToString(png));}
    static int[] dimensions(byte[] png){
        if(png==null||png.length<24||png.length>MAX_BYTES||!Arrays.equals(Arrays.copyOf(png,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))throw problem(HttpStatus.UNPROCESSABLE_ENTITY);
        // Animated PNGs cannot retain their animation after flattening a canvas.
        for(int at=8;at+12<=png.length;){int length=java.nio.ByteBuffer.wrap(png,at,4).getInt();if(length<0||(long)at+12+length>png.length)throw problem(HttpStatus.UNPROCESSABLE_ENTITY);if(png[at+4]=='a'&&png[at+5]=='c'&&png[at+6]=='T'&&png[at+7]=='L')throw problem(HttpStatus.UNPROCESSABLE_ENTITY);at+=12+length;}
        try(var stream=new MemoryCacheImageInputStream(new ByteArrayInputStream(png))){
            var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw problem(HttpStatus.UNPROCESSABLE_ENTITY);var reader=readers.next();
            try{reader.setInput(stream,true,true);if(!reader.getFormatName().equalsIgnoreCase("png"))throw problem(HttpStatus.UNPROCESSABLE_ENTITY);int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||w>8192||h>8192||(long)w*h>12_000_000)throw problem(HttpStatus.PAYLOAD_TOO_LARGE);return new int[]{w,h};}finally{reader.dispose();}
        }catch(IOException invalid){throw problem(HttpStatus.UNPROCESSABLE_ENTITY);}
    }
    static byte[] normalize(byte[] png){
        dimensions(png);
        try(var in=new MemoryCacheImageInputStream(new ByteArrayInputStream(png))){var reader=ImageIO.getImageReaders(in).next();
            try{reader.setInput(in,true,true);var image=reader.read(0);if(image==null)throw problem(HttpStatus.UNPROCESSABLE_ENTITY);
                try(var out=new ByteArrayOutputStream()){if(!ImageIO.write(image,"png",out))throw problem(HttpStatus.UNPROCESSABLE_ENTITY);if(out.size()>MAX_BYTES)throw problem(HttpStatus.PAYLOAD_TOO_LARGE);return out.toByteArray();}finally{image.flush();}
            }finally{reader.dispose();}
        }catch(IOException invalid){throw problem(HttpStatus.UNPROCESSABLE_ENTITY);}
    }
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    private static void field(MessageDigest digest,String value){byte[] bytes=value==null?new byte[0]:value.getBytes(java.nio.charset.StandardCharsets.UTF_8);digest.update((byte)(value==null?0:1));digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);}
    private static ResponseStatusException problem(HttpStatus status){return new ResponseStatusException(status);}
}

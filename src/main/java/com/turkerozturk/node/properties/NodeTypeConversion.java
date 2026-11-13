package com.turkerozturk.node.properties;

import com.turkerozturk.exportmindmap.MindMapTextContent;
import com.turkerozturk.jdbc.JdbcIdentifiers;
import jakarta.persistence.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Explicit, lossy conversion. Revision covers node metadata, object metadata and binary bytes. */
@Component
public class NodeTypeConversion {
    @PersistenceContext private EntityManager entityManager;
    static final int MAX_TEXT=2_000_000, MAX_OBJECTS=2000;
    public record Preview(String revision,int objects) { }
    record Snapshot(String revision,String text,int images,int tables,int codeboxes) {
        int count(){return images+tables+codeboxes;}
    }
    Preview preview(long id){return entityManager.unwrap(Session.class).doReturningWork(c->{var s=read(c,id,false);return new Preview(s.revision(),s.count());});}
    Snapshot prepare(long id,String revision){
        if(revision==null||!revision.matches("[a-f0-9]{64}"))throw problem(HttpStatus.BAD_REQUEST,"confirmationRequired");
        return entityManager.unwrap(Session.class).doReturningWork(c->{var s=read(c,id,true);if(!s.revision().equals(revision))throw problem(HttpStatus.CONFLICT,"staleConversion");return s;});
    }
    void removeObjects(long id,Snapshot snapshot){
        entityManager.unwrap(Session.class).doWork(c->{
            for(var item:Map.of("image",snapshot.images(),"grid",snapshot.tables(),"codebox",snapshot.codeboxes()).entrySet()){
                try(var q=c.prepareStatement("DELETE FROM "+item.getKey()+" WHERE node_id=?")){
                    q.setLong(1,id);if(q.executeUpdate()!=item.getValue())throw problem(HttpStatus.CONFLICT,"staleConversion");
                }
            }
        });
    }
    Snapshot read(Connection c,long id,boolean lock)throws SQLException{
        String suffix=lock&&!c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")?" FOR UPDATE":"";
        try{
            var digest=MessageDigest.getInstance("SHA-256");value(digest,id);String source;
            try(var q=c.prepareStatement("SELECT COALESCE(master_id,0) FROM children WHERE node_id=?"+suffix)){
                q.setLong(1,id);try(var r=q.executeQuery()){if(!r.next()||r.getLong(1)!=0)throw problem(HttpStatus.NOT_FOUND,"missingNode");}
            }
            try(var q=c.prepareStatement("SELECT name,txt,syntax,tags,is_ro,is_richtxt,level,ts_creation,ts_lastsave FROM node WHERE node_id=?"+suffix)){
                q.setLong(1,id);try(var r=q.executeQuery()){
                    if(!r.next())throw problem(HttpStatus.NOT_FOUND,"missingNode");
                    String syntax=r.getString(3);long flags=r.getLong(6);
                    if((r.getLong(5)&1)!=0)throw problem(HttpStatus.FORBIDDEN,"readOnly");
                    if(!"custom-colors".equals(syntax)&&(flags&1)==0)throw problem(HttpStatus.CONFLICT,"staleConversion");
                    source=Objects.requireNonNullElse(r.getString(2),"");if(source.length()>MAX_TEXT)throw problem(HttpStatus.PAYLOAD_TOO_LARGE,"largeConversion");
                    for(int col=1;col<=9;col++)value(digest,r.getObject(col));
                }
            }
            int[] counts=new int[3];int textSize=source.length();
            String offset=JdbcIdentifiers.quote(c,"offset");
            String[] sql={"SELECT "+offset+",justification,anchor,filename,link,time,png FROM image WHERE node_id=? ORDER BY "+offset,
                    "SELECT "+offset+",justification,txt,col_min,col_max FROM grid WHERE node_id=? ORDER BY "+offset,
                    "SELECT "+offset+",justification,txt,syntax,width,height,is_width_pix,do_highl_bra,do_show_linenum FROM codebox WHERE node_id=? ORDER BY "+offset};
            for(int table=0;table<sql.length;table++){
                value(digest,table);
                try(var q=c.prepareStatement(sql[table]+suffix)){
                    q.setLong(1,id);try(var r=q.executeQuery()){
                        while(r.next()){
                            if(++counts[table]>MAX_OBJECTS||Arrays.stream(counts).sum()>MAX_OBJECTS)throw problem(HttpStatus.PAYLOAD_TOO_LARGE,"largeConversion");
                            int columns=r.getMetaData().getColumnCount();
                            for(int col=1;col<=columns;col++){
                                if(table==0&&col==columns){
                                    digest.update((byte)3);try(var in=r.getBinaryStream(col)){
                                        if(in==null)value(digest,null);
                                        else{var binary=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[8192];int n;long size=0;while((n=in.read(buffer))!=-1){binary.update(buffer,0,n);size+=n;}value(digest,size);value(digest,HexFormat.of().formatHex(binary.digest()));}
                                    }
                                }else{
                                    Object data=r.getObject(col);value(digest,data);
                                    if(table>0&&col==3){textSize+=Objects.toString(data,"").length();if(textSize>MAX_TEXT)throw problem(HttpStatus.PAYLOAD_TOO_LARGE,"largeConversion");}
                                }
                            }
                        }
                    }
                }
            }
            String text=MindMapTextContent.text(c,id,source);
            if(text.length()>MAX_TEXT)throw problem(HttpStatus.PAYLOAD_TOO_LARGE,"largeConversion");
            return new Snapshot(HexFormat.of().formatHex(digest.digest()),text,counts[0],counts[1],counts[2]);
        }catch(IOException|NoSuchAlgorithmException error){throw new IllegalStateException("Cannot verify conversion",error);}
    }
    private static void value(MessageDigest digest,Object value){
        digest.update((byte)(value==null?0:1));byte[] bytes=Objects.toString(value,"").getBytes(StandardCharsets.UTF_8);
        digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);
    }
    private static ResponseStatusException problem(HttpStatus status,String code){return new ResponseStatusException(status,code);}
}

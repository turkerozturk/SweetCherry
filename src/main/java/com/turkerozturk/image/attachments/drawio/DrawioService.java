package com.turkerozturk.image.attachments.drawio;

import com.turkerozturk.jdbc.JdbcIdentifiers;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.sql.*;
import java.util.*;

@Service @Transactional(readOnly=true)
public class DrawioService {
    static final int MAX_BYTES=8*1024*1024;
    @PersistenceContext private EntityManager manager;
    public static boolean supports(String filename){return filename!=null&&filename.toLowerCase(Locale.ROOT).endsWith(".drawio");}
    public static final class Problem extends RuntimeException {
        private final String code;public Problem(String code){super(code);this.code=code;}public String code(){return code;}
    }
    public record Page(String name,String xml) {}
    public record Preview(String filename,String nodeName,List<Page> pages) {}
    public Preview preview(long nodeId,int offset){
        if(nodeId<=0||offset<0)throw new Problem("request");
        return manager.unwrap(Session.class).doReturningWork(c->load(c,nodeId,offset));
    }
    static Preview load(Connection c,long nodeId,int offset)throws SQLException{
        String bytes=c.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")?"OCTET_LENGTH(i.png)":"LENGTH(i.png)";
        String from=" FROM image i JOIN node n ON n.node_id=i.node_id WHERE i.node_id=? AND i."+JdbcIdentifiers.quote(c,"offset")+"=? AND i.png IS NOT NULL AND COALESCE(i.anchor,'')=''";
        String filename,name;
        try(var q=c.prepareStatement("SELECT i.filename,n.name,"+bytes+from)){
            q.setLong(1,nodeId);q.setInt(2,offset);try(var rs=q.executeQuery()){
                if(!rs.next())throw new Problem("missing");filename=rs.getString(1);name=rs.getString(2);
                if(!supports(filename))throw new Problem("unsupported");if(rs.getLong(3)>MAX_BYTES)throw new Problem("large");
            }
        }
        try(var q=c.prepareStatement("SELECT i.png"+from+" AND "+bytes+"<=? AND i.filename=?")){
            q.setLong(1,nodeId);q.setInt(2,offset);q.setInt(3,MAX_BYTES);q.setString(4,filename);
            try(var rs=q.executeQuery()){
                if(!rs.next())throw new Problem("missing");try(var in=rs.getBinaryStream(1)){
                    return DrawioDocument.parse(in.readNBytes(MAX_BYTES+1),filename,name);
                }catch(IOException invalid){throw new Problem("invalid");}
            }
        }
    }
}

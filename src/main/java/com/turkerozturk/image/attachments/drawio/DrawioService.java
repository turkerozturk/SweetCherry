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
    static final int MAX_BYTES=DrawioSettings.defaults().fileBytes();
    private final DrawioSettings settings;
    public DrawioService(DrawioSettings settings){this.settings=settings;}
    @PersistenceContext private EntityManager manager;
    public static boolean supports(String filename){return filename!=null&&filename.toLowerCase(Locale.ROOT).endsWith(".drawio");}
    public static final class Problem extends RuntimeException {
        private final String code,limit;private String reason="";private int page,line,column;
        public Problem(String code){this(code,"");}public Problem(String code,String limit){super(code);this.code=code;this.limit=limit;}
        public String code(){return code;}public String limit(){return limit;}public String reason(){return reason;}public int page(){return page;}public int line(){return line;}public int column(){return column;}
        Problem context(String reason,int page){if(this.reason.isEmpty())this.reason=reason;this.page=page;return this;}
        Problem location(int line,int column){this.line=Math.max(0,line);this.column=Math.max(0,column);return this;}
        @Override public String getMessage(){if(!"invalid".equals(code))return code;return code+(reason.isEmpty()?"":" [reason="+reason+", page="+page+(line>0?", line="+line+", column="+column:"")+"]");}
    }
    public record Page(String name,String xml) {}
    public record Preview(String filename,String nodeName,List<Page> pages,int maxPageBytes) {
        public Preview(String filename,String nodeName,List<Page> pages){this(filename,nodeName,pages,DrawioSettings.defaults().pageBytes());}
    }
    public Preview preview(long nodeId,int offset){
        if(nodeId<=0||offset<0)throw new Problem("request");
        return manager.unwrap(Session.class).doReturningWork(c->load(c,nodeId,offset,settings));
    }
    static Preview load(Connection c,long nodeId,int offset)throws SQLException{return load(c,nodeId,offset,DrawioSettings.defaults());}
    static Preview load(Connection c,long nodeId,int offset,DrawioSettings settings)throws SQLException{
        String bytes=c.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")?"OCTET_LENGTH(i.png)":"LENGTH(i.png)";
        String from=" FROM image i JOIN node n ON n.node_id=i.node_id WHERE i.node_id=? AND i."+JdbcIdentifiers.quote(c,"offset")+"=? AND i.png IS NOT NULL AND COALESCE(i.anchor,'')=''";
        String filename,name;
        try(var q=c.prepareStatement("SELECT i.filename,n.name,"+bytes+from)){
            q.setLong(1,nodeId);q.setInt(2,offset);try(var rs=q.executeQuery()){
                if(!rs.next())throw new Problem("missing");filename=rs.getString(1);name=rs.getString(2);
                if(!supports(filename))throw new Problem("unsupported");if(rs.getLong(3)>settings.fileBytes())throw new Problem("large","max-file-bytes");
            }
        }
        try(var q=c.prepareStatement("SELECT i.png"+from+" AND "+bytes+"<=? AND i.filename=?")){
            q.setLong(1,nodeId);q.setInt(2,offset);q.setInt(3,settings.fileBytes());q.setString(4,filename);
            try(var rs=q.executeQuery()){
                if(!rs.next())throw new Problem("missing");try(var in=rs.getBinaryStream(1)){
                    return DrawioDocument.parse(in.readNBytes(settings.fileBytes()+1),filename,name,settings);
                }catch(IOException invalid){throw new Problem("invalid");}
            }
        }
    }
}

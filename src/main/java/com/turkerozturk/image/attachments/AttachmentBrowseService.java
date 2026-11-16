package com.turkerozturk.image.attachments;

import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.turkerozturk.jdbc.JdbcIdentifiers;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/** Lists metadata only and streams a single attachment inside the routed read transaction. */
@Service @Transactional(readOnly=true)
public class AttachmentBrowseService {
    static final int PAGE_SIZE=50;
    @PersistenceContext private EntityManager manager;
    public record Options(int page,String search,String sort){
        public Options{search=search==null?"":search.strip();if(page<1||page>1000000||search.length()>200||!Set.of("newest","name-asc","name-desc","size-asc","size-desc").contains(sort))throw problem(HttpStatus.BAD_REQUEST);}
    }
    public record Item(long nodeId,int offset,String name,String filename,long time,long bytes){
        public boolean excel(){return com.turkerozturk.image.attachments.excel.ExcelPreviewService.supports(filename);}
        public String identity(){return nodeId+":"+offset;}
        public String sizeText(){if(bytes<1024)return bytes+" B";if(bytes<1048576)return String.format(Locale.ROOT,"%.2f KiB",bytes/1024d);if(bytes<1073741824)return String.format(Locale.ROOT,"%.2f MiB",bytes/1048576d);return String.format(Locale.ROOT,"%.2f GiB",bytes/1073741824d);}
        public String timeText(){if(time<=0)return "—";try{return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(time));}catch(DateTimeException invalid){return "—";}}
    }
    public record Page(List<Item> items,long total,Options options){public long pages(){return Math.max(1,(total+PAGE_SIZE-1)/PAGE_SIZE);}}
    public record Download(String filename,long bytes){}
    public Page list(Options options){return manager.unwrap(Session.class).doReturningWork(c->list(c,options));}
    public void stream(long nodeId,int offset,Consumer<Download> headers,OutputStream output){
        manager.unwrap(Session.class).doWork(c->stream(c,nodeId,offset,headers,output));
    }
    private static String candidates(){
        return "i.png IS NOT NULL AND COALESCE(i.anchor,'')='' AND COALESCE(i.filename,'')<>'' AND i.filename<>'__ct_special.tex'";
    }
    private static String length(Connection c)throws SQLException{return c.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")?"OCTET_LENGTH(i.png)":"LENGTH(i.png)";}
    static Page list(Connection c,Options options)throws SQLException{
        String from=" FROM image i JOIN node n ON n.node_id=i.node_id WHERE "+candidates();
        if(!options.search().isEmpty())from+=" AND LOWER(i.filename) LIKE ? ESCAPE '!'";
        long total;try(var q=c.prepareStatement("SELECT COUNT(*)"+from)){bind(q,options);try(var rs=q.executeQuery()){rs.next();total=rs.getLong(1);}}
        String order=switch(options.sort()){
            case "name-asc"->"LOWER(i.filename) ASC";
            case "name-desc"->"LOWER(i.filename) DESC";
            case "size-asc"->length(c)+" ASC";
            case "size-desc"->length(c)+" DESC";
            default->"COALESCE(i.time,0) DESC";
        };
        var items=new ArrayList<Item>();
        try(var q=c.prepareStatement("SELECT i.node_id,i."+JdbcIdentifiers.quote(c,"offset")+",n.name,i.filename,i.time,"+length(c)+from+" ORDER BY "+order+",i.node_id,i."+JdbcIdentifiers.quote(c,"offset")+" LIMIT ? OFFSET ?")){
            int index=bind(q,options);q.setInt(index++,PAGE_SIZE);q.setLong(index,(long)(options.page()-1)*PAGE_SIZE);
            try(var rs=q.executeQuery()){while(rs.next())items.add(new Item(rs.getLong(1),rs.getInt(2),Objects.requireNonNullElse(rs.getString(3),""),rs.getString(4),rs.getLong(5),rs.getLong(6)));}
        }
        return new Page(List.copyOf(items),total,options);
    }
    private static int bind(PreparedStatement q,Options options)throws SQLException{
        if(options.search().isEmpty())return 1;
        q.setString(1,"%"+options.search().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");return 2;
    }
    static void stream(Connection c,long nodeId,int offset,Consumer<Download> headers,OutputStream output)throws SQLException{
        if(nodeId<=0||offset<0)throw problem(HttpStatus.BAD_REQUEST);
        try(var q=c.prepareStatement("SELECT i.filename,"+length(c)+",i.png FROM image i JOIN node n ON n.node_id=i.node_id WHERE i.node_id=? AND i."+JdbcIdentifiers.quote(c,"offset")+"=? AND "+candidates())){
            q.setLong(1,nodeId);q.setInt(2,offset);
            try(var rs=q.executeQuery()){
                if(!rs.next())throw problem(HttpStatus.NOT_FOUND);
                headers.accept(new Download(safeName(rs.getString(1),nodeId,offset),rs.getLong(2)));
                try(var in=rs.getBinaryStream(3)){in.transferTo(output);}catch(IOException error){throw new SQLException("Attachment transfer failed",error);}
            }
        }
    }
    static String safeName(String name,long nodeId,int offset){
        String leaf=Objects.requireNonNullElse(name,"").replace('\\','/');leaf=leaf.substring(leaf.lastIndexOf('/')+1);
        leaf=leaf.codePoints().filter(c->!Character.isISOControl(c)).limit(180).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString().strip();
        return leaf.isBlank()||leaf.equals(".")||leaf.equals("..")?"attachment-"+nodeId+"-"+offset+".bin":leaf;
    }
    private static ResponseStatusException problem(HttpStatus status){return new ResponseStatusException(status);}
}

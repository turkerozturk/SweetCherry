package com.turkerozturk.grid.gallery;

import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.turkerozturk.jdbc.JdbcIdentifiers;
import com.turkerozturk.richtext.experimental.EmbeddedContentAdapter;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @Transactional(readOnly=true)
public class TableGalleryService {
    static final int MAX_XML = 2097152;
    @PersistenceContext private EntityManager manager;
    private final TableGallerySettings settings;
    public TableGalleryService(TableGallerySettings settings) { this.settings=settings; }
    public record Options(int page,String search,String sort) {
        public Options {
            search=search==null?"":search.strip();
            if(page<1||page>1000000||search.length()>200||!Set.of("newest","oldest","node","id").contains(sort))throw problem(HttpStatus.BAD_REQUEST);
        }
    }
    public record Item(long nodeId,int offset,String name,long created,int colMin,int colMax,long characters) {
        public String id(){return nodeId+":"+offset;}
        public String createdText(){
            if(created<=0)return "—";
            try{return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(created));}
            catch(DateTimeException invalid){return "—";}
        }
    }
    public record Page(List<Item> items,long total,int pageSize,Options options){
        public long pages(){return Math.max(1,(total+pageSize-1)/pageSize);}
    }
    public record Data(List<String> header,List<List<String>> rows,int columns,int totalRows,int totalColumns,boolean limited){ }
    public Page list(Options options){return manager.unwrap(Session.class).doReturningWork(c->list(c,options,settings.pageSize()));}
    public Data data(long id,int offset,boolean full){return manager.unwrap(Session.class).doReturningWork(c->parse(source(c,id,offset),full,settings));}
    public String source(long id,int offset){return manager.unwrap(Session.class).doReturningWork(c->source(c,id,offset));}
    static Page list(Connection c,Options options,int size)throws SQLException {
        String from=" FROM grid g JOIN node n ON n.node_id=g.node_id";
        if(!options.search().isEmpty())from+=" WHERE LOWER(n.name) LIKE ? ESCAPE '!'";
        long total;
        try(var q=c.prepareStatement("SELECT COUNT(*)"+from)){
            bind(q,options);try(var rs=q.executeQuery()){rs.next();total=rs.getLong(1);}
        }
        String offset="g."+JdbcIdentifiers.quote(c,"offset");
        String order=switch(options.sort()){
            case "oldest"->"COALESCE(n.ts_creation,0) ASC,g.node_id,"+offset;
            case "node"->"LOWER(n.name),g.node_id,"+offset;
            case "id"->"g.node_id,"+offset;
            default->"COALESCE(n.ts_creation,0) DESC,g.node_id,"+offset;
        };
        var items=new ArrayList<Item>();
        try(var q=c.prepareStatement("SELECT g.node_id,"+offset+",n.name,n.ts_creation,g.col_min,g.col_max,LENGTH(g.txt)"+from+" ORDER BY "+order+" LIMIT ? OFFSET ?")){
            int index=bind(q,options);q.setInt(index++,size);q.setLong(index,(long)(options.page()-1)*size);
            try(var rs=q.executeQuery()){while(rs.next())items.add(new Item(rs.getLong(1),rs.getInt(2),Objects.requireNonNullElse(rs.getString(3),""),rs.getLong(4),rs.getInt(5),rs.getInt(6),rs.getLong(7)));}
        }
        return new Page(List.copyOf(items),total,size,options);
    }
    private static int bind(PreparedStatement q,Options options)throws SQLException{
        if(options.search().isEmpty())return 1;
        q.setString(1,"%"+options.search().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");return 2;
    }
    static String source(Connection c,long id,int offset)throws SQLException{
        if(id<=0||offset<0)throw problem(HttpStatus.BAD_REQUEST);
        String where=" FROM grid g JOIN node n ON n.node_id=g.node_id WHERE g.node_id=? AND g."+JdbcIdentifiers.quote(c,"offset")+"=?";
        try(var q=c.prepareStatement("SELECT LENGTH(g.txt)"+where)){
            q.setLong(1,id);q.setInt(2,offset);try(var rs=q.executeQuery()){
                if(!rs.next())throw problem(HttpStatus.NOT_FOUND);if(rs.getLong(1)>MAX_XML)throw problem(HttpStatus.PAYLOAD_TOO_LARGE);
            }
        }
        try(var q=c.prepareStatement("SELECT g.txt"+where+" AND LENGTH(g.txt)<=?")){
            q.setLong(1,id);q.setInt(2,offset);q.setInt(3,MAX_XML);
            try(var rs=q.executeQuery()){if(!rs.next())throw problem(HttpStatus.NOT_FOUND);String xml=rs.getString(1);if(xml==null)throw problem(HttpStatus.UNPROCESSABLE_ENTITY);if(xml.length()>MAX_XML)throw problem(HttpStatus.PAYLOAD_TOO_LARGE);return xml;}
        }
    }
    static Data parse(String xml,boolean full,TableGallerySettings settings){
        if(xml==null||xml.length()>MAX_XML)throw problem(HttpStatus.PAYLOAD_TOO_LARGE);
        final List<List<String>> stored;
        try{stored=new EmbeddedContentAdapter().readTable(xml).rows();}catch(IllegalArgumentException invalid){throw problem(HttpStatus.UNPROCESSABLE_ENTITY);}
        int columns=stored.stream().mapToInt(List::size).max().orElse(0);
        int totalRows=Math.max(0,stored.size()-1);
        int shownColumns=Math.min(columns,full?100:settings.previewColumns());
        int shownRows=Math.min(totalRows,full?settings.viewerRows():settings.previewRows());
        // Bound rendered cells even when the configured row limit is high.
        if(shownColumns>0)shownRows=Math.min(shownRows,10000/shownColumns-1);
        boolean[] clipped={shownColumns<columns||shownRows<totalRows};
        var rows=new ArrayList<List<String>>();
        List<String> header=stored.isEmpty()?List.of():cells(stored.get(stored.size()-1),shownColumns,full,clipped);
        for(int i=0;i<shownRows;i++)rows.add(cells(stored.get(i),shownColumns,full,clipped));
        return new Data(header,List.copyOf(rows),shownColumns,totalRows,columns,clipped[0]);
    }
    private static List<String> cells(List<String> row,int columns,boolean full,boolean[] clipped){
        var result=new ArrayList<String>();int max=full?20000:240;
        for(int i=0;i<columns;i++){
            String text=i<row.size()?row.get(i):"";
            if(text.length()>max){int end=max;if(Character.isHighSurrogate(text.charAt(end-1)))end--;text=text.substring(0,end)+"…";clipped[0]=true;}
            result.add(text);
        }
        return List.copyOf(result);
    }
    private static ResponseStatusException problem(HttpStatus status){return new ResponseStatusException(status);}
}

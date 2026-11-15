package com.turkerozturk.codebox.gallery;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.turkerozturk.jdbc.JdbcIdentifiers;
import com.turkerozturk.helpers.highlighter.CodeHighLighter;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @Transactional(readOnly=true)
public class CodeGalleryService {
    static final int MAX_SOURCE=2097152;
    @PersistenceContext private EntityManager manager;
    private final CodeGallerySettings settings;
    public CodeGalleryService(CodeGallerySettings settings){this.settings=settings;}
    public record Options(int page,String search,String syntax,String sort){
        public Options{search=search==null?"":search.strip();syntax=syntax==null?"":syntax.strip();if(page<1||page>1000000||search.length()>200||syntax.length()>100||!Set.of("newest","oldest","node","syntax","id").contains(sort))throw problem(HttpStatus.BAD_REQUEST);}
    }
    public record Item(long nodeId,int offset,String name,long created,String syntax,long characters,String preview,String language,boolean limited){
        public String identity(){return nodeId+":"+offset;}
        public String createdText(){if(created<=0)return "—";try{return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(created));}catch(DateTimeException invalid){return "—";}}
    }
    public record Page(List<Item> items,long total,int pageSize,List<String> syntaxes,Options options){public long pages(){return Math.max(1,(total+pageSize-1)/pageSize);}}
    public record Data(String text,String syntax,String language,long characters,boolean limited){}
    public Page list(Options options){return manager.unwrap(Session.class).doReturningWork(c->list(c,options,settings));}
    public Data data(long nodeId,int offset){
        return manager.unwrap(Session.class).doReturningWork(c->{var source=source(c,nodeId,offset);String text=crop(source.text(),settings.viewerCharacters(),Integer.MAX_VALUE);return new Data(text,source.syntax(),source.language(),source.characters(),!text.equals(source.text()));});
    }
    public Data source(long nodeId,int offset){return manager.unwrap(Session.class).doReturningWork(c->source(c,nodeId,offset));}
    static Page list(Connection c,Options options,CodeGallerySettings settings)throws SQLException{
        String from=" FROM codebox b JOIN node n ON n.node_id=b.node_id WHERE 1=1";
        if(!options.search().isEmpty())from+=" AND LOWER(n.name) LIKE ? ESCAPE '!'";
        if(!options.syntax().isEmpty())from+=" AND COALESCE(b.syntax,'plain-text')=?";
        long total;try(var q=c.prepareStatement("SELECT COUNT(*)"+from)){bind(q,options,1);try(var rs=q.executeQuery()){rs.next();total=rs.getLong(1);}}
        String offset="b."+JdbcIdentifiers.quote(c,"offset"),order=switch(options.sort()){
            case "oldest"->"COALESCE(n.ts_creation,0) ASC,b.node_id,"+offset;
            case "node"->"LOWER(n.name),b.node_id,"+offset;
            case "syntax"->"COALESCE(b.syntax,'plain-text'),b.node_id,"+offset;
            case "id"->"b.node_id,"+offset;
            default->"COALESCE(n.ts_creation,0) DESC,b.node_id,"+offset;
        };
        var items=new ArrayList<Item>();
        try(var q=c.prepareStatement("SELECT b.node_id,"+offset+",n.name,n.ts_creation,COALESCE(b.syntax,'plain-text'),COALESCE(LENGTH(b.txt),0),SUBSTR(b.txt,1,?)"+from+" ORDER BY "+order+" LIMIT ? OFFSET ?")){
            q.setInt(1,settings.previewCharacters()+1);int next=bind(q,options,2);q.setInt(next++,settings.pageSize());q.setLong(next,(long)(options.page()-1)*settings.pageSize());
            try(var rs=q.executeQuery()){while(rs.next()){
                String raw=Objects.requireNonNullElse(rs.getString(7),""),preview=crop(raw,settings.previewCharacters(),settings.previewLines()),syntax=rs.getString(5);long characters=rs.getLong(6);
                items.add(new Item(rs.getLong(1),rs.getInt(2),Objects.requireNonNullElse(rs.getString(3),""),rs.getLong(4),syntax,characters,preview,CodeHighLighter.languageFor(syntax),!raw.equals(preview)||characters>raw.codePointCount(0,raw.length())));
            }}
        }
        var syntaxes=new ArrayList<String>();try(var q=c.prepareStatement("SELECT DISTINCT COALESCE(b.syntax,'plain-text') FROM codebox b JOIN node n ON n.node_id=b.node_id ORDER BY 1 LIMIT 200");var rs=q.executeQuery()){while(rs.next()){String s=rs.getString(1);if(s.length()<=100)syntaxes.add(s);}}
        return new Page(List.copyOf(items),total,settings.pageSize(),List.copyOf(syntaxes),options);
    }
    private static int bind(PreparedStatement q,Options options,int next)throws SQLException{
        if(!options.search().isEmpty())q.setString(next++,"%"+options.search().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");
        if(!options.syntax().isEmpty())q.setString(next++,options.syntax());return next;
    }
    static Data source(Connection c,long nodeId,int offset)throws SQLException{
        if(nodeId<=0||offset<0)throw problem(HttpStatus.BAD_REQUEST);
        String from=" FROM codebox b JOIN node n ON n.node_id=b.node_id WHERE b.node_id=? AND b."+JdbcIdentifiers.quote(c,"offset")+"=?";
        try(var q=c.prepareStatement("SELECT COALESCE(LENGTH(b.txt),0)"+from)){q.setLong(1,nodeId);q.setInt(2,offset);try(var rs=q.executeQuery()){if(!rs.next())throw problem(HttpStatus.NOT_FOUND);if(rs.getLong(1)>MAX_SOURCE)throw problem(HttpStatus.PAYLOAD_TOO_LARGE);}}
        try(var q=c.prepareStatement("SELECT b.txt,COALESCE(b.syntax,'plain-text'),COALESCE(LENGTH(b.txt),0)"+from+" AND COALESCE(LENGTH(b.txt),0)<=?")){
            q.setLong(1,nodeId);q.setInt(2,offset);q.setInt(3,MAX_SOURCE);try(var rs=q.executeQuery()){
                if(!rs.next())throw problem(HttpStatus.NOT_FOUND);String text=Objects.requireNonNullElse(rs.getString(1),""),syntax=rs.getString(2);if(text.length()>MAX_SOURCE)throw problem(HttpStatus.PAYLOAD_TOO_LARGE);
                return new Data(text,syntax,CodeHighLighter.languageFor(syntax),rs.getLong(3),false);
            }
        }
    }
    static String crop(String text,int maxCharacters,int maxLines){
        int end=Math.min(text.length(),maxCharacters),lines=1;
        for(int i=0;i<end;i++)if(text.charAt(i)=='\n'&&++lines>maxLines){end=i;break;}
        if(end>0&&Character.isHighSurrogate(text.charAt(end-1)))end--;
        return text.substring(0,end);
    }
    private static ResponseStatusException problem(HttpStatus status){return new ResponseStatusException(status);}
}

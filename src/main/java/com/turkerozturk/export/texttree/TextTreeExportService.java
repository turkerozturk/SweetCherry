package com.turkerozturk.export.texttree;

import com.turkerozturk.exportmindmap.MindMapTextContent;
import jakarta.persistence.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.zip.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class TextTreeExportService {
    @PersistenceContext private EntityManager entityManager;
    private final MindMapTextContent richText;
    private final TextTreeExportLimits limits;
    private final Semaphore permit=new Semaphore(1);
    public TextTreeExportService(MindMapTextContent richText,TextTreeExportLimits limits){this.richText=richText;this.limits=limits;}
    public record Archive(Path path,String filename,int nodes) implements AutoCloseable {
        public void close() throws IOException {Files.deleteIfExists(path);}
    }
    private record Row(long id,long contentId,String name,String syntax,long characters) {}
    private record Pending(Row row,String parent,int depth) {}
    private record Entry(Row row,String path,boolean directory) {}
    @Transactional(readOnly=true)
    public String title(long id){
        if(id<1)throw new ResponseStatusException(BAD_REQUEST);
        return entityManager.unwrap(Session.class).doReturningWork(c->rows(c,"c.node_id",id).stream().findFirst().orElseThrow(()->new ResponseStatusException(NOT_FOUND)).name());
    }
    @Transactional(readOnly=true)
    public Archive export(long id,boolean descendants){
        if(id<1)throw new ResponseStatusException(BAD_REQUEST);
        if(!permit.tryAcquire())throw new ResponseStatusException(TOO_MANY_REQUESTS);
        try{return entityManager.unwrap(Session.class).doReturningWork(c->{
            try{return create(c,id,descendants);}catch(IOException e){throw new UncheckedIOException(e);}
        });}finally{permit.release();}
    }
    /** Queries only bounded text/metadata; image and attachment BLOBs are never selected. */
    Archive create(Connection c,long id,boolean descendants)throws SQLException,IOException {
        Row root=rows(c,"c.node_id",id).stream().findFirst().orElseThrow(()->new ResponseStatusException(NOT_FOUND));
        var stack=new ArrayDeque<Pending>();stack.push(new Pending(root,"",0));
        var seen=new HashSet<Long>();var entries=new ArrayList<Entry>();var paths=new TextTreePaths();
        while(!stack.isEmpty()){
            var pending=stack.pop();var row=pending.row();
            if(!seen.add(row.id()))throw new ResponseStatusException(CONFLICT);
            if(entries.size()>=limits.getMaxNodes()||pending.depth()>limits.getMaxDepth())throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
            if(row.characters()>limits.getMaxNodeCharacters())throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
            var children=descendants?rows(c,"c.father_id",row.id()):List.<Row>of();
            if((long)entries.size()+stack.size()+children.size()+1>limits.getMaxNodes())throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
            boolean directory=!children.isEmpty();String path=paths.allocate(pending.parent(),row.name(),row.id(),directory);
            if(path.getBytes(StandardCharsets.UTF_8).length>2048)throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
            entries.add(new Entry(row,path,directory));
            for(int i=children.size()-1;i>=0;i--)stack.push(new Pending(children.get(i),path+"/",pending.depth()+1));
        }
        Path temp=Files.createTempFile("sweetcherry-text-tree-",".zip");boolean success=false;
        try {
            long remaining=limits.getMaxTextBytes();
            try(var zip=new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)),StandardCharsets.UTF_8)){
                for(var entry:entries){
                    if(entry.directory()){zip.putNextEntry(new ZipEntry(entry.path()+"/"));zip.closeEntry();}
                    String raw=read(c,entry.row().contentId());
                    String text=raw;
                    if("custom-colors".equals(entry.row().syntax())){
                        checkObjects(c,entry.row().contentId(),raw.length());
                        text=richText.text(entry.row().contentId(),entry.row().syntax(),raw);
                    }
                    if(text.length()>limits.getMaxNodeCharacters())throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
                    if(entry.directory()&&text.isEmpty())continue;
                    String target=entry.path()+(entry.directory()?"/_content.txt":"");
                    zip.putNextEntry(new ZipEntry(target));
                    // Limit uncompressed UTF-8 bytes while encoding; do not assemble the ZIP in heap memory.
                    final long budget=remaining;
                    var output=new OutputStream(){long written;
                        public void write(int b)throws IOException{if(++written>budget)throw new ResponseStatusException(PAYLOAD_TOO_LARGE);zip.write(b);}
                        public void write(byte[] b,int off,int len)throws IOException{if((written+=len)>budget)throw new ResponseStatusException(PAYLOAD_TOO_LARGE);zip.write(b,off,len);}
                    };
                    var writer=new OutputStreamWriter(output,StandardCharsets.UTF_8);writer.write(text);writer.flush();
                    remaining-=output.written;zip.closeEntry();
                }
            }
            success=true;return new Archive(temp,TextTreePaths.safe(root.name(),id)+"-"+id+"-text.zip",entries.size());
        }finally{if(!success)Files.deleteIfExists(temp);}
    }
    private List<Row> rows(Connection c,String column,long id)throws SQLException {
        // column is one of the two internal literals above, never supplied by an HTTP request.
        String sql="SELECT c.node_id,CASE WHEN c.master_id IS NULL OR c.master_id=0 THEN c.node_id ELSE c.master_id END,n.name,n.syntax,LENGTH(n.txt) FROM children c LEFT JOIN node n ON n.node_id=CASE WHEN c.master_id IS NULL OR c.master_id=0 THEN c.node_id ELSE c.master_id END WHERE "+column+"=? ORDER BY c.sequence,c.node_id";
        var result=new ArrayList<Row>();
        try(var query=c.prepareStatement(sql)){query.setLong(1,id);query.setMaxRows((int)Math.min(Integer.MAX_VALUE,(long)limits.getMaxNodes()+1));
            try(var rs=query.executeQuery()){while(rs.next()){
                if(result.size()>=limits.getMaxNodes())throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
                if(rs.getString(3)==null)throw new ResponseStatusException(CONFLICT);
                result.add(new Row(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getLong(5)));
            }}
        }return result;
    }
    private String read(Connection c,long id)throws SQLException{
        try(var query=c.prepareStatement("SELECT txt FROM node WHERE node_id=?")){query.setLong(1,id);
            try(var rs=query.executeQuery()){
                if(!rs.next())throw new ResponseStatusException(CONFLICT);
                // Recheck size before requesting the text in case another writer changed it after planning.
                try(var reader=rs.getCharacterStream(1)){
                    if(reader==null)return "";
                    var text=new StringBuilder();char[] buffer=new char[8192];int n;
                    while((n=reader.read(buffer))!=-1){if((long)text.length()+n>limits.getMaxNodeCharacters())throw new ResponseStatusException(PAYLOAD_TOO_LARGE);text.append(buffer,0,n);}
                    return text.toString();
                }catch(IOException e){throw new UncheckedIOException(e);}
            }
        }
    }
    private void checkObjects(Connection c,long id,int rawLength)throws SQLException{
        long size=rawLength;
        for(String table:List.of("codebox","grid"))try(var q=c.prepareStatement("SELECT COALESCE(SUM(LENGTH(txt)),0) FROM "+table+" WHERE node_id=?")){
            q.setLong(1,id);try(var r=q.executeQuery()){r.next();size+=r.getLong(1);}
        }
        if(size>limits.getMaxNodeCharacters())throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
    }
}

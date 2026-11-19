package com.turkerozturk.node.title;

import com.turkerozturk.node.NodeNames;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Renames real nodes only, preserving all content/title bits and using the properties visibility guard. */
@Service
public class NodeTitleService {
    @PersistenceContext private EntityManager manager;
    private final NodePropertiesService properties;
    public NodeTitleService(NodePropertiesService properties){this.properties=properties;}
    public record Snapshot(String id,String name,String revision) { }
    private record Stored(long id,String name,String tags,Long modified) { }
    private String guard(long id){
        if(!properties.writable())throw problem(HttpStatus.FORBIDDEN,"readonly");
        return properties.realNode(id).getTags();
    }
    @Transactional(readOnly=true) public Snapshot open(long id){
        String tags=guard(id);
        return manager.unwrap(Session.class).doReturningWork(c->snapshot(read(c,id,tags,false)));
    }
    @Transactional public Snapshot save(long id,String name,String revision){
        String tags=guard(id);String clean=NodeNames.clean(name);
        if(clean==null||clean.isBlank()||clean.length()>255)throw problem(HttpStatus.BAD_REQUEST,"invalid");
        var result=manager.unwrap(Session.class).doReturningWork(c->save(c,id,clean,revision,tags));
        manager.clear();return result;
    }
    Snapshot save(Connection c,long id,String name,String revision,String guardedTags)throws SQLException{
        var old=read(c,id,guardedTags,true);
        if(!snapshot(old).revision().equals(revision))throw problem(HttpStatus.CONFLICT,"changed");
        if(!Objects.equals(old.name(),name)){
            String sql="UPDATE node SET name=?,ts_lastsave=? WHERE node_id=?"
                    +(old.name()==null?" AND name IS NULL":" AND name=?")
                    +(old.modified()==null?" AND ts_lastsave IS NULL":" AND ts_lastsave=?")
                    +(old.tags()==null?" AND tags IS NULL":" AND tags=?");
            try(var q=c.prepareStatement(sql)){
                q.setString(1,name);q.setLong(2,com.turkerozturk.node.NodeTimestamps.now());q.setLong(3,id);int index=4;
                if(old.name()!=null)q.setString(index++,old.name());if(old.modified()!=null)q.setLong(index++,old.modified());if(old.tags()!=null)q.setString(index,old.tags());
                if(q.executeUpdate()!=1)throw problem(HttpStatus.CONFLICT,"changed");
            }
        }
        return snapshot(read(c,id,guardedTags,false));
    }
    private Stored read(Connection c,long id,String guardedTags,boolean lock)throws SQLException{
        String suffix=lock&&!c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")?" FOR UPDATE":"";
        try(var q=c.prepareStatement("SELECT n.name,n.tags,n.ts_lastsave FROM node n JOIN children c ON c.node_id=n.node_id AND COALESCE(c.master_id,0)=0 WHERE n.node_id=?"+suffix)){
            q.setLong(1,id);try(var rows=q.executeQuery()){
                if(!rows.next())throw problem(HttpStatus.NOT_FOUND,"missing");
                String tags=rows.getString(2);if(!Objects.equals(tags,guardedTags))throw problem(HttpStatus.CONFLICT,"changed");
                long stamp=rows.getLong(3);Long modified=rows.wasNull()?null:stamp;
                return new Stored(id,rows.getString(1),tags,modified);
            }
        }
    }
    private static Snapshot snapshot(Stored row){
        try{
            String text=row.id()+"\n"+row.modified()+"\n"+(row.name()==null?"NULL":"TEXT"+row.name());
            String revision=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
            return new Snapshot(Long.toString(row.id()),Objects.requireNonNullElse(row.name(),""),revision);
        }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static ResponseStatusException problem(HttpStatus status,String reason){return new ResponseStatusException(status,reason);}
}

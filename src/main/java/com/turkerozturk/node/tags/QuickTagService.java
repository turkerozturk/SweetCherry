package com.turkerozturk.node.tags;

import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.*;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Single-node tag writes with fresh source/revision checks, independent of other properties. */
@Service
public class QuickTagService {
    @PersistenceContext private EntityManager entityManager;
    private final NodePropertiesService properties;
    public QuickTagService(NodePropertiesService properties){this.properties=properties;}
    public record Snapshot(String id,String contentId,String name,String tags,String revision,List<String> catalogue){}
    private record Stored(long id,long content,String name,String raw,Long modified){}
    private void writable(){if(!properties.writable())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"readonly");}
    @Transactional(readOnly=true) public Snapshot open(long id){writable();return entityManager.unwrap(Session.class).doReturningWork(c->snapshot(c,read(c,id,false)));}
    @Transactional public Snapshot save(long id,String tags,String revision){
        writable();final String normalized;try{normalized=NodeTags.normalize(tags);}catch(IllegalArgumentException invalid){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalid");}
        return entityManager.unwrap(Session.class).doReturningWork(c->save(c,id,normalized,revision));
    }
    Snapshot save(Connection c,long id,String normalized,String revision)throws SQLException{
        var old=read(c,id,true);if(!hash(old).equals(revision))throw new ResponseStatusException(HttpStatus.CONFLICT,"changed");
        if(!NodeTags.normalize(Objects.requireNonNullElse(old.raw(),"")).equals(normalized)){
            try(var q=c.prepareStatement("UPDATE node SET tags=?,ts_lastsave=? WHERE node_id=?"+(old.modified()==null?" AND ts_lastsave IS NULL":" AND ts_lastsave=?")+(old.raw()==null?" AND tags IS NULL":" AND tags=?"))){
                q.setString(1,normalized);q.setLong(2,com.turkerozturk.node.NodeTimestamps.now());q.setLong(3,old.content());int parameter=4;if(old.modified()!=null)q.setLong(parameter++,old.modified());if(old.raw()!=null)q.setString(parameter,old.raw());
                if(q.executeUpdate()!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"changed");
            }
        }
        return snapshot(c,read(c,id,false));
    }
    private Stored read(Connection c,long id,boolean lock)throws SQLException{
        if(id<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalid");
        String suffix=lock&&!c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")?" FOR UPDATE":"";
        try(var q=c.prepareStatement("SELECT c.node_id,n.node_id,n.name,n.tags,n.ts_lastsave FROM children c JOIN node n ON n.node_id=CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END JOIN children m ON m.node_id=n.node_id AND COALESCE(m.master_id,0)=0 WHERE c.node_id=?"+suffix)){
            q.setLong(1,id);try(var rows=q.executeQuery()){if(!rows.next())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"missing");long stamp=rows.getLong(5);Long modified=rows.wasNull()?null:stamp;return new Stored(rows.getLong(1),rows.getLong(2),Objects.requireNonNullElse(rows.getString(3),""),rows.getString(4),modified);}
        }
    }
    private Snapshot snapshot(Connection c,Stored value)throws SQLException{return new Snapshot(Long.toString(value.id()),Long.toString(value.content()),value.name(),String.join(" ",NodeTags.split(value.raw())),hash(value),NodeTagService.catalogue(c));}
    private static String hash(Stored value){try{var digest=MessageDigest.getInstance("SHA-256");String text=value.id()+"\n"+value.content()+"\n"+value.modified()+"\n"+(value.raw()==null?"NULL":"TEXT"+value.raw());return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
}

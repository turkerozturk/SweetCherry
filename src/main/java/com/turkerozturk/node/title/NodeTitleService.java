package com.turkerozturk.node.title;

import com.turkerozturk.node.NodeNames;
import com.turkerozturk.helpers.NodeIcon;
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

/** Quick name/icon editing for real nodes, preserving content, lock and other title bits. */
@Service
public class NodeTitleService {
    @PersistenceContext private EntityManager manager;
    private final NodePropertiesService properties;
    public NodeTitleService(NodePropertiesService properties){this.properties=properties;}
    public record Snapshot(String id,String name,String revision,String icon) {
        public Snapshot(String id,String name,String revision){this(id,name,revision,null);}
    }
    private record Stored(long id,String name,String tags,Long modified,Long iconBits) { }
    private String guard(long id){
        if(!properties.writable())throw problem(HttpStatus.FORBIDDEN,"readonly");
        return properties.realNode(id).getTags();
    }
    @Transactional(readOnly=true) public Snapshot open(long id){
        String tags=guard(id);
        return manager.unwrap(Session.class).doReturningWork(c->snapshot(read(c,id,tags,false)));
    }
    @Transactional public Snapshot save(long id,String name,String revision){
        return save(id,name,revision,null);
    }
    @Transactional public Snapshot save(long id,String name,String revision,NodeIcon icon){
        String tags=guard(id);String clean=NodeNames.clean(name);
        if(clean==null||clean.isBlank()||clean.length()>255)throw problem(HttpStatus.BAD_REQUEST,"invalid");
        var result=manager.unwrap(Session.class).doReturningWork(c->save(c,id,clean,revision,tags,icon));
        manager.clear();return result;
    }
    Snapshot save(Connection c,long id,String name,String revision,String guardedTags)throws SQLException{
        return save(c,id,name,revision,guardedTags,null);
    }
    Snapshot save(Connection c,long id,String name,String revision,String guardedTags,NodeIcon icon)throws SQLException{
        var old=read(c,id,guardedTags,true);
        if(!snapshot(old).revision().equals(revision))throw problem(HttpStatus.CONFLICT,"changed");
        long iconBits=old.iconBits()==null?0:old.iconBits();
        long updatedIcon=icon==null?iconBits:(iconBits & ~0x7FFEL) | icon.getIconIdIn16bit();
        if(!Objects.equals(old.name(),name)||(icon!=null&&updatedIcon!=iconBits)){
            String sql="UPDATE node SET name=?,ts_lastsave=?"+(icon!=null?",is_ro=?":"")+" WHERE node_id=?"
                    +(old.name()==null?" AND name IS NULL":" AND name=?")
                    +(old.modified()==null?" AND ts_lastsave IS NULL":" AND ts_lastsave=?")
                    +(old.tags()==null?" AND tags IS NULL":" AND tags=?")
                    +(old.iconBits()==null?" AND is_ro IS NULL":" AND is_ro=?");
            try(var q=c.prepareStatement(sql)){
                q.setString(1,name);q.setLong(2,com.turkerozturk.node.NodeTimestamps.now());int index=3;
                if(icon!=null)q.setLong(index++,updatedIcon);q.setLong(index++,id);
                if(old.name()!=null)q.setString(index++,old.name());if(old.modified()!=null)q.setLong(index++,old.modified());if(old.tags()!=null)q.setString(index++,old.tags());if(old.iconBits()!=null)q.setLong(index,old.iconBits());
                if(q.executeUpdate()!=1)throw problem(HttpStatus.CONFLICT,"changed");
            }
        }
        return snapshot(read(c,id,guardedTags,false));
    }
    private Stored read(Connection c,long id,String guardedTags,boolean lock)throws SQLException{
        String suffix=lock&&!c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")?" FOR UPDATE":"";
        try(var q=c.prepareStatement("SELECT n.name,n.tags,n.ts_lastsave,n.is_ro FROM node n JOIN children c ON c.node_id=n.node_id AND COALESCE(c.master_id,0)=0 WHERE n.node_id=?"+suffix)){
            q.setLong(1,id);try(var rows=q.executeQuery()){
                if(!rows.next())throw problem(HttpStatus.NOT_FOUND,"missing");
                String tags=rows.getString(2);if(!Objects.equals(tags,guardedTags))throw problem(HttpStatus.CONFLICT,"changed");
                long stamp=rows.getLong(3);Long modified=rows.wasNull()?null:stamp;
                long bits=rows.getLong(4);Long iconBits=rows.wasNull()?null:bits;
                return new Stored(id,rows.getString(1),tags,modified,iconBits);
            }
        }
    }
    private static Snapshot snapshot(Stored row){
        try{
            String text=row.id()+"\n"+row.modified()+"\n"+row.iconBits()+"\n"+row.tags()+"\n"+(row.name()==null?"NULL":"TEXT"+row.name());
            String revision=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
            String icon=null;int encoded=(int)((row.iconBits()==null?0:row.iconBits()) & 0x7FFE);
            for(var candidate:NodeIcon.values())if(candidate.getIconIdIn16bit()==encoded){icon=candidate.name();break;}
            return new Snapshot(Long.toString(row.id()),Objects.requireNonNullElse(row.name(),""),revision,icon);
        }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static ResponseStatusException problem(HttpStatus status,String reason){return new ResponseStatusException(status,reason);}
}

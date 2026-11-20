package com.turkerozturk.node.moving;

import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.*;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Reorders occurrences, never their descendants or their master content. */
@Service @PreAuthorize("hasRole('ADMIN')")
public class SortChildrenService {
    public enum Key { CREATED, MODIFIED, NAME }
    public enum Order { ASC, DESC }
    public record State(String revision, int count, String name) {}
    record Child(long id,long sequence,long master,String name,long created,long modified) {}
    record Snapshot(State state,List<Child> children) {}
    private final NodePropertiesService properties;
    @PersistenceContext private EntityManager manager;
    public SortChildrenService(NodePropertiesService properties){this.properties=properties;}
    private void guard(){if(!properties.writable())throw problem(HttpStatus.FORBIDDEN);}
    @Transactional(readOnly=true) public State state(long id){guard();return manager.unwrap(Session.class).doReturningWork(c->read(c,id,false).state);}
    @Transactional public void sort(long id,Key key,Order order,String revision,boolean confirmed){
        guard();if(!confirmed||key==null||order==null)throw problem(HttpStatus.BAD_REQUEST);
        manager.unwrap(Session.class).doWork(c->sort(c,id,key,order,revision));manager.clear();
    }
    static Snapshot read(Connection c,long id,boolean lock)throws SQLException{
        if(id<=0)throw problem(HttpStatus.BAD_REQUEST);
        String suffix=lock&&!c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")?" FOR UPDATE":"";
        var fingerprint=new StringBuilder();String name="";long ancestor=id;var visited=new HashSet<Long>();
        // Also reject descendants of system containers. Shared occurrences resolve their own master.
        while(ancestor!=0){
            if(!visited.add(ancestor))throw problem(HttpStatus.CONFLICT);
            try(var q=c.prepareStatement("SELECT c.father_id,COALESCE(c.master_id,0),n.name,n.tags,n.is_ro,n.ts_lastsave FROM children c JOIN node n ON n.node_id=CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END WHERE c.node_id=?"+suffix)){
                q.setLong(1,ancestor);try(var rs=q.executeQuery()){
                    if(!rs.next())throw problem(HttpStatus.NOT_FOUND);
                    String tags=Objects.requireNonNullElse(rs.getString(4),"");
                    if(Set.of("__sweetcherry_audit_v1__","__sweetcherry_trash_v1__","__sweetcherry_reminders_v1__").contains(tags))throw problem(HttpStatus.FORBIDDEN);
                    if(ancestor==id){if((rs.getLong(5)&1)!=0)throw problem(HttpStatus.FORBIDDEN);name=Objects.requireNonNullElse(rs.getString(3),"");}
                    fingerprint.append(ancestor).append(':');for(int i=1;i<=6;i++)field(fingerprint,rs.getString(i));
                    ancestor=rs.getLong(1);
                }
            }
        }
        var children=new ArrayList<Child>();
        // Inner join supports aliases on PostgreSQL as well as SQLite/MySQL; FOR UPDATE locks both rows.
        try(var q=c.prepareStatement("SELECT c.node_id,c.sequence,COALESCE(c.master_id,0),n.name,n.ts_creation,n.ts_lastsave FROM children c JOIN node n ON n.node_id=CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END WHERE c.father_id=? ORDER BY c.sequence,c.node_id"+suffix)){
            q.setLong(1,id);try(var rs=q.executeQuery()){while(rs.next()){
                children.add(new Child(rs.getLong(1),rs.getLong(2),rs.getLong(3),Objects.requireNonNullElse(rs.getString(4),""),rs.getLong(5),rs.getLong(6)));
                for(int i=1;i<=6;i++)field(fingerprint,rs.getString(i));
            }}
        }
        try(var q=c.prepareStatement("SELECT COUNT(*) FROM children WHERE father_id=?")){q.setLong(1,id);try(var rs=q.executeQuery()){rs.next();if(rs.getInt(1)!=children.size())throw problem(HttpStatus.CONFLICT);}}
        return new Snapshot(new State(hash(fingerprint.toString()),children.size(),name),children);
    }
    static void sort(Connection c,long id,Key key,Order order,String expected)throws SQLException{
        var snapshot=read(c,id,true);if(!snapshot.state.revision.equals(expected))throw problem(HttpStatus.CONFLICT);
        Comparator<Child> comparator=switch(key){case CREATED->Comparator.comparingLong(Child::created);case MODIFIED->Comparator.comparingLong(Child::modified);case NAME->Comparator.comparing(Child::name,String.CASE_INSENSITIVE_ORDER);};
        if(order==Order.DESC)comparator=comparator.reversed();
        var sorted=new ArrayList<>(snapshot.children);sorted.sort(comparator); // Stable ties retain the displayed sibling order.
        if(sorted.equals(snapshot.children))return;
        try(var q=c.prepareStatement("UPDATE children SET sequence=? WHERE node_id=? AND father_id=? AND sequence=? AND COALESCE(master_id,0)=?")){
            for(int at=0;at<sorted.size();at++){var child=sorted.get(at);q.setLong(1,at+1L);q.setLong(2,child.id);q.setLong(3,id);q.setLong(4,child.sequence);q.setLong(5,child.master);if(q.executeUpdate()!=1)throw problem(HttpStatus.CONFLICT);}
        }
        // Changing sibling order must not change the children's modified-date sort keys.
        try(var q=c.prepareStatement("UPDATE node SET ts_lastsave=? WHERE node_id=(SELECT CASE WHEN COALESCE(master_id,0)=0 THEN node_id ELSE master_id END FROM children WHERE node_id=?)")){
            q.setLong(1,com.turkerozturk.node.NodeTimestamps.now());q.setLong(2,id);if(q.executeUpdate()!=1)throw problem(HttpStatus.CONFLICT);
        }
    }
    private static void field(StringBuilder text,String value){text.append(value==null?-1:value.length()).append(':').append(value==null?"":value).append(';');}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static ResponseStatusException problem(HttpStatus status){return new ResponseStatusException(status);}
}

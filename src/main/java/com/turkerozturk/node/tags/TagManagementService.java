package com.turkerozturk.node.tags;

import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.multipledatabases.TenantContext;
import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;
import org.hibernate.Session;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class TagManagementService {
    public enum Action { RENAME, REMOVE }
    public record Operation(Action action,String from,String to) {
        public Operation {
            if(action==null||!valid(from))throw new TagManagementProblem("invalid");
            if(action==Action.REMOVE)to=null;
            else if(!valid(to)||to.codePoints().anyMatch(Character::isISOControl)||from.equals(to))throw new TagManagementProblem("invalid");
        }
        private static boolean valid(String tag){return tag!=null&&!tag.isEmpty()&&tag.indexOf(' ')<0&&tag.indexOf('\0')<0&&tag.length()<=NodeTags.MAX_INPUT_CHARACTERS;}
    }
    public record Row(long id,String name,String before,String after) {}
    public record Preview(Operation operation,int count,String revision,List<Row> rows) {}
    public record Result(Operation operation,int count) {}
    private record Scan(List<Row> rows,String revision) {}
    @PersistenceContext private EntityManager entityManager;
    private final NodePropertiesService properties;
    public TagManagementService(NodePropertiesService properties){this.properties=properties;}
    public void requireWritable(){if(!properties.writable())throw new AccessDeniedException("The selected database is read-only.");}
    @Transactional(readOnly=true)
    public Preview preview(Operation operation){requireWritable();return entityManager.unwrap(Session.class).doReturningWork(c->preview(c,operation));}
    Preview preview(Connection c,Operation operation)throws SQLException {
        var scan=scan(c,operation,false);
        if(scan.rows().isEmpty())throw new TagManagementProblem("missing");
        return new Preview(operation,scan.rows().size(),scan.revision(),List.copyOf(scan.rows().subList(0,Math.min(200,scan.rows().size()))));
    }
    @Transactional
    public Result apply(Operation operation,String revision){
        requireWritable();return entityManager.unwrap(Session.class).doReturningWork(c->apply(c,operation,revision));
    }
    Result apply(Connection c,Operation operation,String revision)throws SQLException {
        var scan=scan(c,operation,true);
        if(scan.rows().isEmpty()||!scan.revision().equals(revision))throw new TagManagementProblem("changed");
        long timestamp=com.turkerozturk.node.NodeTimestamps.now();
        try(var update=c.prepareStatement("UPDATE node SET tags=?,ts_lastsave=? WHERE node_id=? AND tags=?")) {
            for(var row:scan.rows()){
                update.setString(1,row.after());update.setLong(2,timestamp);update.setLong(3,row.id());update.setString(4,row.before());
                if(update.executeUpdate()!=1)throw new TagManagementProblem("changed");
            }
        }
        var auth=SecurityContextHolder.getContext().getAuthentication();String actor=auth==null?"unknown":auth.getName();
        String source=TenantContext.getCurrentTenant(),operationId=UUID.randomUUID().toString();
        Runnable audit=()->log(operationId,actor,source,operation,scan.rows());
        // Never report successful node changes before their transaction has actually committed.
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){audit.run();}});
        return new Result(operation,scan.rows().size());
    }
    private Scan scan(Connection c,Operation operation,boolean lock)throws SQLException {
        var changes=new ArrayList<Row>();long characters=0;MessageDigest digest;
        try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
        String suffix=lock&&!c.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("sqlite")?" FOR UPDATE":"";
        // No DISTINCT/LIKE/equality filtering: database collations must not alter literal tag matching.
        try(var query=c.createStatement();var rows=query.executeQuery("SELECT node_id,name,tags FROM node ORDER BY node_id"+suffix)){
            while(rows.next()){
                String raw=rows.getString(3);var tags=NodeTags.split(raw);if(!tags.contains(operation.from()))continue;
                if(raw.length()>NodeTags.MAX_INPUT_CHARACTERS||(characters+=raw.length())>16L*1024*1024||changes.size()>=50000)throw new TagManagementProblem("limit");
                var updated=new LinkedHashSet<String>();
                for(String tag:tags){if(!tag.equals(operation.from()))updated.add(tag);else if(operation.action()==Action.RENAME)updated.add(operation.to());}
                String after=String.join(" ",updated);if(after.length()>NodeTags.MAX_INPUT_CHARACTERS)throw new TagManagementProblem("limit");
                var row=new Row(rows.getLong(1),Objects.toString(rows.getString(2),""),raw,after);changes.add(row);
                hash(digest,Long.toString(row.id()));hash(digest,row.name());hash(digest,raw);hash(digest,after);
            }
        }return new Scan(List.copyOf(changes),HexFormat.of().formatHex(digest.digest()));
    }
    private static void hash(MessageDigest digest,String value){byte[] bytes=value.getBytes(StandardCharsets.UTF_8);digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);}
    static String safeLog(String value){String text=Objects.toString(value,"").replaceAll("[\\p{Cntrl}\\p{Cf}]"," ").replace('"','\'');return text.length()>200?text.substring(0,200)+"…":text;}
    private static void log(String id,String actor,String source,Operation operation,List<Row> rows){
        var logger=org.slf4j.LoggerFactory.getLogger(TagManagementService.class);
        for(var row:rows)logger.info("Tag change: operationId={}, actor=\"{}\", database=\"{}\", nodeId={}, nodeName=\"{}\", action={}, from=\"{}\", to=\"{}\"",id,safeLog(actor),safeLog(source),row.id(),safeLog(row.name()),operation.action(),safeLog(operation.from()),safeLog(operation.to()));
        logger.info("Tag change committed: operationId={}, action={}, changedNodes={}",id,operation.action(),rows.size());
    }
}

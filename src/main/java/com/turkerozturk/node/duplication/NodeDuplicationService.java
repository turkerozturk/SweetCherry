package com.turkerozturk.node.duplication;

import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/** Copies CTB rows in one tenant transaction without decoding XML or binary object payloads. */
@Service
public class NodeDuplicationService {
    private final NodePropertiesService properties;
    @PersistenceContext private EntityManager entityManager;
    public NodeDuplicationService(NodePropertiesService properties) { this.properties=properties; }
    record Row(long id,long parent,long sequence,long master) { }
    record Snapshot(List<Row> rows,Set<Long> realIds) { }
    public record State(String revision,boolean shared,int subtreeCount) { }

    /** Supplies a hierarchy revision and the number of tree occurrences that a subtree copy would create. */
    @Transactional(readOnly=true)
    public State state(long id) {
        requireWritable();
        return entityManager.unwrap(org.hibernate.Session.class).doReturningWork(connection -> state(read(connection),id));
    }
    /** Allocates fresh identities, copies payloads and inserts the new root immediately after the original. */
    @Transactional
    public long duplicate(long id,boolean withSubnodes,String revision) {
        requireWritable();
        long result=entityManager.unwrap(org.hibernate.Session.class).doReturningWork(connection ->
                duplicate(connection,id,withSubnodes,revision,Instant.now().getEpochSecond()));
        entityManager.clear();
        return result;
    }
    /** Creates a sibling occurrence linked to the real master, without copying content or embedded objects. */
    @Transactional
    public long createShared(long id, String revision) {
        requireWritable();
        long result=entityManager.unwrap(org.hibernate.Session.class).doReturningWork(connection -> createShared(connection,id,revision));
        entityManager.clear();
        return result;
    }

    /** Inserts one alias row and normalizes sibling positions only after validating the supplied tree revision. */
    static long createShared(Connection connection,long id,String expected) throws SQLException {
        var snapshot=read(connection); var root=selected(snapshot,id);
        validateAncestors(snapshot,root); branch(snapshot,root,false);
        if (!revision(snapshot).equals(expected)) throw conflict();
        long highest=Math.max(snapshot.realIds.stream().mapToLong(Long::longValue).max().orElse(0),
                snapshot.rows.stream().mapToLong(Row::id).max().orElse(0));
        if (highest==Long.MAX_VALUE) throw conflict();
        long result=highest+1;
        var siblings=snapshot.rows.stream().filter(row->row.parent==root.parent)
                .sorted(Comparator.comparingLong(Row::sequence).thenComparingLong(Row::id)).toList();
        int insertAt=siblings.indexOf(root)+1;
        try(var statement=connection.prepareStatement("UPDATE children SET sequence=? WHERE node_id=?")) {
            for(int at=0;at<siblings.size();at++) {
                statement.setLong(1,at+1L+(at>=insertAt?1:0)); statement.setLong(2,siblings.get(at).id);
                if(statement.executeUpdate()!=1) throw conflict();
            }
        }
        try(var statement=connection.prepareStatement("INSERT INTO children(node_id,father_id,sequence,master_id) VALUES(?,?,?,?)")) {
            statement.setLong(1,result); statement.setLong(2,root.parent); statement.setLong(3,insertAt+1L);
            statement.setLong(4,root.master==0?root.id:root.master);
            if(statement.executeUpdate()!=1) throw conflict();
        }
        return result;
    }
    private void requireWritable() {
        if(!properties.writable()) throw new AccessDeniedException("The selected CTB is read-only.");
    }
    static Snapshot read(Connection connection) throws SQLException {
        var rows=new ArrayList<Row>();var real=new HashSet<Long>();
        try(var statement=connection.prepareStatement("SELECT node_id,father_id,sequence,COALESCE(master_id,0) FROM children ORDER BY node_id");var result=statement.executeQuery()) {
            while(result.next()) rows.add(new Row(result.getLong(1),result.getLong(2),result.getLong(3),result.getLong(4)));
        }
        try(var statement=connection.prepareStatement("SELECT node_id FROM node");var result=statement.executeQuery()) {
            while(result.next()) real.add(result.getLong(1));
        }
        return new Snapshot(List.copyOf(rows),Set.copyOf(real));
    }
    static State state(Snapshot snapshot,long id) {
        var root=selected(snapshot,id);
        validateAncestors(snapshot,root);
        branch(snapshot,root,false);
        // A malformed descendant prevents only subtree duplication; a single real-node copy remains possible.
        int count=0;
        if(root.master==0) try {count=branch(snapshot,root,true).size();} catch(ResponseStatusException ignored) { }
        else branch(snapshot,root,false);
        return new State(revision(snapshot),root.master!=0,count);
    }
    private static Row selected(Snapshot snapshot,long id) {
        return snapshot.rows.stream().filter(row->row.id==id).findFirst().orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND,"Tree node not found"));
    }
    /** Rejects a cyclic or missing occurrence parent chain and broken content references. */
    private static void validateAncestors(Snapshot snapshot,Row root) {
        var index=index(snapshot);var visited=new HashSet<Long>();Row row=root;
        while(true) {
            if(!visited.add(row.id)) throw conflict();
            if(row.parent==0) break;
            row=index.get(row.parent);
            if(row==null) throw conflict();
            validateContentReference(snapshot,row);
        }
    }
    private static Map<Long,Row> index(Snapshot snapshot) {
        var result=new HashMap<Long,Row>();
        for(var row:snapshot.rows) if(row.id<=0 || result.put(row.id,row)!=null) throw conflict();
        return result;
    }
    private static void validateContentReference(Snapshot snapshot,Row row) {
        if(row.master==0 ? !snapshot.realIds.contains(row.id)
                : snapshot.realIds.contains(row.id) || !snapshot.realIds.contains(row.master)) throw conflict();
    }
    /** Includes aliases in the branch but never follows their master as a hierarchy edge. */
    private static List<Row> branch(Snapshot snapshot,Row root,boolean descendants) {
        var children=new HashMap<Long,List<Row>>();
        for(var row:snapshot.rows) children.computeIfAbsent(row.parent,key->new ArrayList<>()).add(row);
        children.values().forEach(group->group.sort(Comparator.comparingLong(Row::sequence).thenComparingLong(Row::id)));
        var result=new ArrayList<Row>();var pending=new ArrayDeque<Row>();var seen=new HashSet<Long>();pending.add(root);
        while(!pending.isEmpty()) {
            var row=pending.removeFirst();
            if(!seen.add(row.id)) throw conflict();
            validateContentReference(snapshot,row);
            var below=children.getOrDefault(row.id,List.of());
            if(descendants && row.master!=0 && !below.isEmpty()) throw conflict();
            result.add(row);
            if(descendants) pending.addAll(below);
        }
        return result;
    }

    /** Uses INSERT SELECT to preserve SQL NULL, text, binary data and additional CTB columns verbatim. */
    static long duplicate(Connection connection,long id,boolean withSubnodes,String expected,long timestamp) throws SQLException {
        var snapshot=read(connection);var root=selected(snapshot,id);validateAncestors(snapshot,root);
        if(!revision(snapshot).equals(expected)) throw conflict();
        if(withSubnodes && root.master!=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A shared node has no subtree to duplicate.");
        var branch=branch(snapshot,root,withSubnodes);
        long highest=snapshot.realIds.stream().mapToLong(Long::longValue).max().orElse(0);
        highest=Math.max(highest,snapshot.rows.stream().mapToLong(Row::id).max().orElse(0));
        if(highest>Long.MAX_VALUE-branch.size()) throw conflict();
        var mapping=new LinkedHashMap<Long,Long>();
        for(var row:branch) mapping.put(row.id,++highest);
        long newRoot=mapping.get(id);
        for(var row:branch) if(row.master==0) {
            copyRows(connection,"node",row.id,mapping.get(row.id),timestamp,true);
            for(String table:List.of("image","grid","codebox")) copyRows(connection,table,row.id,mapping.get(row.id),timestamp,false);
        }
        var siblings=new ArrayList<>(snapshot.rows.stream().filter(row->row.parent==root.parent)
                .sorted(Comparator.comparingLong(Row::sequence).thenComparingLong(Row::id)).toList());
        int insertAt=siblings.indexOf(root)+1;
        // Normalize the affected original siblings around the new root, preserving their relative order.
        try(var statement=connection.prepareStatement("UPDATE children SET sequence=? WHERE node_id=?")) {
            for(int at=0;at<siblings.size();at++) {
                statement.setLong(1,at+1L+(at>=insertAt?1:0));statement.setLong(2,siblings.get(at).id);
                if(statement.executeUpdate()!=1) throw conflict();
            }
        }
        try(var statement=connection.prepareStatement("INSERT INTO children(node_id,father_id,sequence,master_id) VALUES(?,?,?,?)")) {
            var nextSequence=new HashMap<Long,Long>();
            for(var row:branch) {
                long parent=row.id==id?root.parent:mapping.get(row.parent);
                long sequence=row.id==id?insertAt+1L:nextSequence.merge(parent,1L,Long::sum);
                long master=row.master==0?0:mapping.getOrDefault(row.master,row.master);
                statement.setLong(1,mapping.get(row.id));statement.setLong(2,parent);statement.setLong(3,sequence);statement.setLong(4,master);
                if(statement.executeUpdate()!=1) throw conflict();
            }
        }
        return newRoot;
    }
    private static void copyRows(Connection connection,String table,long from,long to,long timestamp,boolean node) throws SQLException {
        if(!Set.of("node","image","grid","codebox").contains(table)) throw new IllegalArgumentException("Unsupported table");
        var columns=new ArrayList<String>();
        try(var statement=connection.prepareStatement("PRAGMA table_info("+quote(table)+")");var rows=statement.executeQuery()) {
            while(rows.next()) columns.add(rows.getString("name"));
        }
        if(!columns.contains("node_id") || node && (!columns.contains("ts_creation") || !columns.contains("ts_lastsave"))) throw conflict();
        var expressions=new ArrayList<String>();var parameters=new ArrayList<Long>();
        for(String column:columns) {
            if(column.equals("node_id")) {expressions.add("?");parameters.add(to);}
            else if(node && (column.equals("ts_creation") || column.equals("ts_lastsave"))) {expressions.add("?");parameters.add(timestamp);}
            else expressions.add(quote(column));
        }
        String sql="INSERT INTO "+quote(table)+" ("+String.join(",",columns.stream().map(NodeDuplicationService::quote).toList())+") SELECT "
                +String.join(",",expressions)+" FROM "+quote(table)+" WHERE node_id=?";
        try(var statement=connection.prepareStatement(sql)) {
            int parameter=1;for(long value:parameters) statement.setLong(parameter++,value);statement.setLong(parameter,from);
            int count=statement.executeUpdate();if(node && count!=1) throw conflict();
        }
    }
    private static String quote(String identifier) {return "\""+identifier.replace("\"","\"\"")+"\"";}
    /** Covers occurrence placement and real-node identities; no source rows are overwritten by duplication. */
    private static String revision(Snapshot snapshot) {
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            for(var row:snapshot.rows) digest.update((row.id+":"+row.parent+":"+row.sequence+":"+row.master+";").getBytes(StandardCharsets.UTF_8));
            snapshot.realIds.stream().sorted().forEach(id->digest.update(("node:"+id+";").getBytes(StandardCharsets.UTF_8)));
            return HexFormat.of().formatHex(digest.digest());
        } catch(java.security.NoSuchAlgorithmException error) {throw new IllegalStateException(error);}
    }
    private static ResponseStatusException conflict() {return new ResponseStatusException(HttpStatus.CONFLICT,"Tree changed or has an unsupported hierarchy; reload the page.");}
}

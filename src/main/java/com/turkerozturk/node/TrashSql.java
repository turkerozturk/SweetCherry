package com.turkerozturk.node;

import java.sql.*;
import java.util.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.*;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Reversible occurrence moves using ordinary CTB nodes, with no schema or payload changes. */
public final class TrashSql {
    private TrashSql(){}
    public static final String TAG="__sweetcherry_trash_v1__";
    static final String PREFIX="SweetCherry trash metadata v1\n";
    static final JsonMapper JSON=JsonMapper.builder().build();
    static final int MAX_ENTRIES=10000, MAX_METADATA_CHARACTERS=16*1024*1024;
    record Branch(long id,long parent,long sequence,long master){}
    record Note(long id,String name,long modified){}
    public record Metadata(String kind,String bin,String operation,long payload,long parent,long parentContent,
            long sequence,int index,long previous,long next,String parentPath,String deletedAt,String actor){}
    record Helper(long id,String raw,Metadata meta){}
    record State(Map<Long,Branch> tree,Map<Long,Note> notes,Helper root,Map<Long,Helper> entries,Map<Long,List<Long>> byParent,Map<Long,Integer> contentCounts){}
    public record Preview(String revision,String name,long nodeId,int descendants,int externalSharedLocations){}
    public record Entry(long id,long nodeId,String name,String parentPath,long originalParent,String deletedAt,
            String actor,int descendants,int externalSharedLocations,boolean originalAvailable,String revision){}
    public record Listing(long rootId,List<Entry> entries){}

    static State read(Connection c,boolean lock)throws SQLException{
        String suffix=lock&&!c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")?" FOR UPDATE":"";
        var tree=new LinkedHashMap<Long,Branch>();
        try(var q=c.prepareStatement("SELECT node_id,father_id,sequence,COALESCE(master_id,0) FROM children ORDER BY node_id"+suffix);var rs=q.executeQuery()){
            while(rs.next()){var b=new Branch(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4));if(b.id<=0||tree.put(b.id,b)!=null)throw changed();}
        }
        var notes=new HashMap<Long,Note>();
        try(var q=c.prepareStatement("SELECT node_id,name,ts_lastsave FROM node ORDER BY node_id"+suffix);var rs=q.executeQuery()){
            while(rs.next()){long id=rs.getLong(1);if(notes.put(id,new Note(id,Objects.requireNonNullElse(rs.getString(2),""),rs.getLong(3)))!=null)throw changed();}
        }
        var checked=new HashSet<Long>();
        for(var b:tree.values()){
            if(b.master==0?!notes.containsKey(b.id):notes.containsKey(b.id)||!notes.containsKey(b.master)||!tree.containsKey(b.master)||tree.get(b.master).master!=0)throw changed();
            var path=new HashSet<Long>();for(long id=b.id;id!=0&&!checked.contains(id);id=tree.get(id).parent){if(!tree.containsKey(id)||!path.add(id))throw changed();}checked.addAll(path);
        }
        var byParent=new HashMap<Long,List<Long>>();var contentCounts=new HashMap<Long,Integer>();var aliasMasters=new HashSet<Long>();
        for(var b:tree.values()){byParent.computeIfAbsent(b.parent,key->new ArrayList<>()).add(b.id);contentCounts.merge(content(b),1,Integer::sum);if(b.master!=0)aliasMasters.add(b.master);}
        Helper root=null;long metadataCharacters=0;var entries=new LinkedHashMap<Long,Helper>();
        try(var q=c.prepareStatement("SELECT node_id,txt,syntax,tags FROM node WHERE tags=? ORDER BY node_id")){
            q.setString(1,TAG);
            try(var rs=q.executeQuery()){while(rs.next()){
                if(!TAG.equals(rs.getString(4)))continue;
                long id=rs.getLong(1);String raw=rs.getString(2);if(raw==null||!raw.startsWith(PREFIX)||raw.length()>32768||!"plain-text".equals(rs.getString(3)))throw changed();
                if((metadataCharacters+=raw.length())>MAX_METADATA_CHARACTERS)throw problem(HttpStatus.PAYLOAD_TOO_LARGE,"trash.tooLarge");
                Metadata meta;try{meta=JSON.readValue(raw.substring(PREFIX.length()),Metadata.class);}catch(RuntimeException invalid){throw changed();}
                if(meta==null||!uuid(meta.bin)||!tree.containsKey(id)||tree.get(id).master!=0||aliasMasters.contains(id))throw changed();
                var h=new Helper(id,raw,meta);
                if("root".equals(meta.kind)){if(root!=null||tree.get(id).parent!=0)throw changed();root=h;}
                else if("entry".equals(meta.kind)&&uuid(meta.operation)){entries.put(id,h);if(entries.size()>MAX_ENTRIES)throw changed();}else throw changed();
            }}
        }
        if(root==null&&!entries.isEmpty())throw changed();
        if(root!=null){
            final var bin=root;
            for(var entry:entries.values()){
                var m=entry.meta;var payload=tree.get(m.payload);
                if(!bin.meta.bin.equals(m.bin)||tree.get(entry.id).parent!=bin.id||payload==null||payload.parent!=entry.id||m.parent<0||m.index<0||m.parentPath==null||m.parentPath.length()>8000||m.actor==null||m.actor.length()>160)throw changed();
                try{Instant.parse(m.deletedAt);}catch(RuntimeException e){throw changed();}
                if(byParent.getOrDefault(entry.id,List.of()).size()!=1)throw changed();
                if(entries.containsKey(m.payload)||m.payload==bin.id)throw changed();
            }
            for(var b:tree.values())if(b.parent==bin.id&&!entries.containsKey(b.id))throw changed();
        }
        return new State(tree,notes,root,entries,byParent,contentCounts);
    }
    private static boolean uuid(String value){try{return value!=null&&UUID.fromString(value).toString().equals(value);}catch(IllegalArgumentException e){return false;}}
    static Preview preview(Connection c,long id)throws SQLException{
        var s=read(c,false);normal(s,id);var subtree=subtree(s,id);return new Preview(revision(s),name(s,id),id,subtree.size()-1,external(s,subtree));
    }
    static long trash(Connection c,long id,String expected,String actor)throws SQLException{
        var s=read(c,true);normal(s,id);requireRevision(s,expected);
        if(s.entries.size()>=MAX_ENTRIES)throw problem(HttpStatus.CONFLICT,"trash.limit");
        var selected=s.tree.get(id);var siblings=siblings(s,selected.parent);int at=siblings.indexOf(selected);
        long next=at>0?siblings.get(at-1).id:at+1<siblings.size()?siblings.get(at+1).id:selected.parent;
        long now=NodeTimestamps.now();long rootId;String bin;
        if(s.root==null){
            // A user-created node with this name is never commandeered as a system container.
            if(s.notes.values().stream().anyMatch(n->n.name.equals("TRASH BIN")))throw problem(HttpStatus.CONFLICT,"trash.nameCollision");
            bin=UUID.randomUUID().toString();rootId=insert(c,"TRASH BIN",encode(new Metadata("root",bin,null,0,0,0,0,0,0,0,"",null,null)),0,now);
        }else{rootId=s.root.id;bin=s.root.meta.bin;}
        var parent=selected.parent==0?null:s.tree.get(selected.parent);long parentContent=parent==null?0:content(parent);
        var metadata=new Metadata("entry",bin,UUID.randomUUID().toString(),id,selected.parent,parentContent,selected.sequence,at,
                at>0?siblings.get(at-1).id:0,at+1<siblings.size()?siblings.get(at+1).id:0,
                path(s,selected.parent),Instant.ofEpochSecond(now).toString(),safeActor(actor));
        String title=metadata.deletedAt+" — "+name(s,id);if(title.length()>255)title=title.substring(0,255);
        String encoded=encode(metadata);long characters=s.root==null?1024:s.root.raw.length();for(var h:s.entries.values())characters+=h.raw.length();
        if(characters+encoded.length()>MAX_METADATA_CHARACTERS)throw problem(HttpStatus.CONFLICT,"trash.limit");
        long entry=insert(c,title,encoded,rootId,now);
        try(var q=c.prepareStatement("UPDATE children SET father_id=?,sequence=1 WHERE node_id=?")){q.setLong(1,entry);q.setLong(2,id);if(q.executeUpdate()!=1)throw changed();}
        NodeTimestamps.touchContent(c,content(selected),now);NodeTimestamps.touchContent(c,rootId,now);
        return next;
    }
    static Listing listing(Connection c)throws SQLException{
        var s=read(c,false);if(s.root==null)return new Listing(0,List.of());
        String revision=revision(s);var result=new ArrayList<Entry>();for(var h:s.entries.values())result.add(entry(s,h,revision));
        result.sort(Comparator.comparing(Entry::deletedAt).reversed().thenComparing(Comparator.comparingLong(Entry::id).reversed()));
        return new Listing(s.root.id,List.copyOf(result));
    }
    static Entry entry(Connection c,long id)throws SQLException{var s=read(c,false);return entry(s,helper(s,id),revision(s));}
    private static Entry entry(State s,Helper h,String revision){var m=h.meta;var payload=subtree(s,m.payload);return new Entry(h.id,m.payload,name(s,m.payload),m.parentPath,m.parent,m.deletedAt,m.actor,payload.size()-1,external(s,payload),available(s,m),revision);}
    static long restore(Connection c,long entryId,String expected,boolean toRoot)throws SQLException{
        var s=read(c,true);var h=helper(s,entryId);requireRevision(s,expected);var m=h.meta;
        if(!toRoot&&!available(s,m))throw problem(HttpStatus.CONFLICT,"trash.parentMissing");
        long destination=toRoot?0:m.parent;var target=siblings(s,destination);var payload=s.tree.get(m.payload);
        int at=target.size();
        if(!toRoot){
            int before=index(target,m.next),after=index(target,m.previous);
            at=before>=0?before:after>=0?after+1:Math.min(m.index,target.size());
        }
        target.add(at,payload);write(c,target,destination);
        // Helper has exactly this payload, no aliases, and is empty after the move.
        try(var q=c.prepareStatement("DELETE FROM bookmark WHERE node_id=?")){q.setLong(1,h.id);q.executeUpdate();}
        for(String table:List.of("codebox","image","grid","children","node"))try(var q=c.prepareStatement("DELETE FROM "+table+" WHERE node_id=?")){q.setLong(1,h.id);q.executeUpdate();}
        long now=NodeTimestamps.now();NodeTimestamps.touchContent(c,content(payload),now);NodeTimestamps.touchContent(c,s.root.id,now);
        return payload.id;
    }
    public record PurgePreview(Entry entry,String revision){}
    static PurgePreview purgePreview(Connection c,long entryId)throws SQLException{
        var s=read(c,false);var h=helper(s,entryId);String base=revision(s);
        return new PurgePreview(entry(s,h,base),purgeRevision(c,s,h,base,false));
    }
    static void purge(Connection c,long entryId,String expected)throws SQLException{
        var s=read(c,true);var h=helper(s,entryId);if(!purgeRevision(c,s,h,revision(s),true).equals(expected))throw changed();
        // The existing permanent-delete engine preserves shared content with surviving locations.
        NodeDeletionSql.delete(c,h.id);NodeTimestamps.touchContent(c,s.root.id,NodeTimestamps.now());
    }
    /** Ordinary moves/deletes cannot bypass restore/purge or move active nodes into helper containers. */
    public static void guardMove(Connection c,Collection<Long> ids,long destination)throws SQLException{
        if(!exists(c))return;var s=read(c,false);for(long id:ids)normal(s,id);if(destination!=0)normal(s,destination);
    }
    public static void guardDelete(Connection c,long id)throws SQLException{if(exists(c))normal(read(c,false),id);}
    public static void guardHelper(Connection c,long id)throws SQLException{
        try(var q=c.prepareStatement("SELECT tags FROM node WHERE node_id=? AND tags=?")){q.setLong(1,id);q.setString(2,TAG);try(var rs=q.executeQuery()){if(rs.next()&&TAG.equals(rs.getString(1)))throw problem(HttpStatus.CONFLICT,"trash.usePage");}}
    }
    private static boolean exists(Connection c)throws SQLException{try(var q=c.prepareStatement("SELECT tags FROM node WHERE tags=?")){q.setString(1,TAG);try(var rs=q.executeQuery()){while(rs.next())if(TAG.equals(rs.getString(1)))return true;return false;}}}
    private static void normal(State s,long id){
        if(id<=0||!s.tree.containsKey(id))throw problem(HttpStatus.NOT_FOUND,"trash.missing");
        if(s.root!=null){for(long p=id;p!=0;p=s.tree.get(p).parent)if(p==s.root.id)throw problem(HttpStatus.CONFLICT,"trash.usePage");
}
    }
    private static boolean available(State s,Metadata m){
        if(m.parent==0)return true;var p=s.tree.get(m.parent);if(p==null||content(p)!=m.parentContent)return false;
        for(long id=p.id;id!=0;id=s.tree.get(id).parent)if(s.root!=null&&id==s.root.id||id==m.payload)return false;
        return true;
    }
    private static Helper helper(State s,long id){var h=s.entries.get(id);if(h==null)throw problem(HttpStatus.NOT_FOUND,"trash.missing");return h;}
    private static int index(List<Branch> list,long id){for(int i=0;i<list.size();i++)if(list.get(i).id==id)return i;return -1;}
    private static List<Branch> siblings(State s,long parent){return new ArrayList<>(s.tree.values().stream().filter(b->b.parent==parent).sorted(Comparator.comparingLong(Branch::sequence).thenComparingLong(Branch::id)).toList());}
    private static Set<Long> subtree(State s,long id){
        var result=new LinkedHashSet<Long>();var pending=new ArrayDeque<Long>();pending.add(id);while(!pending.isEmpty()){long next=pending.remove();if(!result.add(next))throw changed();pending.addAll(s.byParent.getOrDefault(next,List.of()));}return result;
    }
    private static int external(State s,Set<Long> ids){var local=new HashMap<Long,Integer>();for(long id:ids)local.merge(content(s.tree.get(id)),1,Integer::sum);int count=0;for(var e:local.entrySet())count+=s.contentCounts.get(e.getKey())-e.getValue();return count;}
    private static long content(Branch b){return b.master==0?b.id:b.master;}
    private static String name(State s,long id){return s.notes.get(content(s.tree.get(id))).name;}
    private static String path(State s,long id){var parts=new LinkedList<String>();int length=0;for(long p=id;p!=0;p=s.tree.get(p).parent){String part=name(s,p)+" ["+p+"]";length+=part.length();if(length>7000){parts.addFirst("…");break;}parts.addFirst(part);}return id==0?"[root]":String.join(" / ",parts);}
    private static String safeActor(String actor){if(actor==null)return "unknown";String safe=actor.replaceAll("[\\p{Cntrl}\\p{Cf}]"," ");return safe.length()>160?safe.substring(0,160):safe;}
    private static String encode(Metadata m){return PREFIX+JSON.writerWithDefaultPrettyPrinter().writeValueAsString(m);}
    private static long insert(Connection c,String title,String text,long parent,long now)throws SQLException{
        long id;try(var q=c.prepareStatement("SELECT COALESCE(MAX(node_id),0)+1 FROM (SELECT node_id FROM node UNION ALL SELECT node_id FROM children) ids");var rs=q.executeQuery()){rs.next();id=rs.getLong(1);if(id<=0)throw changed();}
        long sequence;try(var q=c.prepareStatement("SELECT COALESCE(MAX(sequence),0)+1 FROM children WHERE father_id=?")){q.setLong(1,parent);try(var rs=q.executeQuery()){rs.next();sequence=rs.getLong(1);}}
        long icon=((long)com.turkerozturk.helpers.NodeIcon.CT_EDIT_DELETE.getIconId()<<1)|1;
        try(var q=c.prepareStatement("INSERT INTO node(node_id,name,txt,syntax,tags,is_ro,is_richtxt,has_codebox,has_table,has_image,level,ts_creation,ts_lastsave) VALUES(?,?,?,'plain-text','__sweetcherry_trash_v1__',?,0,0,0,0,3,?,?)")){q.setLong(1,id);q.setString(2,title);q.setString(3,text);q.setLong(4,icon);q.setLong(5,now);q.setLong(6,now);q.executeUpdate();}
        try(var q=c.prepareStatement("INSERT INTO children(node_id,father_id,sequence,master_id) VALUES(?,?,?,0)")){q.setLong(1,id);q.setLong(2,parent);q.setLong(3,sequence);q.executeUpdate();}return id;
    }
    private static void write(Connection c,List<Branch> list,long parent)throws SQLException{try(var q=c.prepareStatement("UPDATE children SET father_id=?,sequence=? WHERE node_id=?")){for(int i=0;i<list.size();i++){q.setLong(1,parent);q.setLong(2,i+1L);q.setLong(3,list.get(i).id);if(q.executeUpdate()!=1)throw changed();}}}
    /** Destructive confirmation also fingerprints current text and binary payloads, streaming large objects. */
    private static String purgeRevision(Connection c,State s,Helper h,String base,boolean lock)throws SQLException{
        var hash=digest();hash.update(base.getBytes(StandardCharsets.UTF_8));var contents=new TreeSet<Long>();
        for(long id:subtree(s,h.id))contents.add(content(s.tree.get(id)));
        String suffix=lock&&!c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")?" FOR UPDATE":"";
        for(String table:List.of("node","image","grid","codebox"))for(long id:contents){
            hash.update((table+":"+id+";").getBytes(StandardCharsets.UTF_8));var rows=new ArrayList<String>();
            try(var q=c.prepareStatement("SELECT * FROM "+table+" WHERE node_id=?"+suffix)){
                q.setLong(1,id);try(var rs=q.executeQuery()){var meta=rs.getMetaData();while(rs.next()){
                    if(rows.size()>=1000000)throw problem(HttpStatus.PAYLOAD_TOO_LARGE,"trash.tooLarge");var row=digest();
                    for(int column=1;column<=meta.getColumnCount();column++){
                        row.update(meta.getColumnName(column).getBytes(StandardCharsets.UTF_8));row.update((byte)0);var field=digest();int type=meta.getColumnType(column);
                        if(type==Types.BLOB||type==Types.BINARY||type==Types.VARBINARY||type==Types.LONGVARBINARY){
                            try(var in=rs.getBinaryStream(column)){row.update((byte)(in==null?0:1));if(in!=null){byte[] bytes=new byte[8192];for(int count;(count=in.read(bytes))!=-1;)field.update(bytes,0,count);}}
                            catch(java.io.IOException e){throw new SQLException("Cannot fingerprint trash content",e);}
                        }else{
                            try(var in=rs.getCharacterStream(column)){row.update((byte)(in==null?0:2));if(in!=null){char[] chars=new char[4096];byte[] bytes=new byte[8192];for(int count;(count=in.read(chars))!=-1;){for(int n=0;n<count;n++){bytes[n*2]=(byte)(chars[n]>>>8);bytes[n*2+1]=(byte)chars[n];}field.update(bytes,0,count*2);}}}
                            catch(java.io.IOException e){throw new SQLException("Cannot fingerprint trash content",e);}
                        }
                        row.update(field.digest());
                    }
                    rows.add(HexFormat.of().formatHex(row.digest()));
                }}
            }
            Collections.sort(rows);for(String row:rows)hash.update(row.getBytes(StandardCharsets.US_ASCII));
        }
        return HexFormat.of().formatHex(hash.digest());
    }
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    private static String revision(State s){try{
        var hash=MessageDigest.getInstance("SHA-256");for(var b:s.tree.values())hash.update((b.id+":"+b.parent+":"+b.sequence+":"+b.master+";").getBytes(StandardCharsets.UTF_8));
        s.notes.values().stream().sorted(Comparator.comparingLong(Note::id)).forEach(n->hash.update((n.id+":"+n.modified+":"+n.name.length()+":"+n.name+";").getBytes(StandardCharsets.UTF_8)));
        if(s.root!=null)hash.update(s.root.raw.getBytes(StandardCharsets.UTF_8));for(var h:s.entries.values())hash.update(h.raw.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(hash.digest());
    }catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    private static void requireRevision(State s,String expected){if(!revision(s).equals(expected))throw changed();}
    private static ResponseStatusException changed(){return problem(HttpStatus.CONFLICT,"trash.changed");}
    static ResponseStatusException problem(HttpStatus status,String code){return new ResponseStatusException(status,code);}
}

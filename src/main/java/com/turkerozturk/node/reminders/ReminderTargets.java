package com.turkerozturk.node.reminders;

import com.turkerozturk.node.TrashSql;
import com.turkerozturk.node.audit.AuditSql;
import com.turkerozturk.node.tags.NodeTags;
import java.sql.*;
import java.util.*;

/** Resolve real/shared IDs using metadata, excluding hidden, missing and cyclic paths. */
final class ReminderTargets {
    record Target(long id,long contentId,String name) { }
    private record Meta(String name,String tags) { }
    private record Branch(long id,long parent,long content) { }
    private final Map<Long,Meta> nodes=new HashMap<>();
    private final Map<Long,Branch> tree=new TreeMap<>();
    private final Map<Long,Boolean> visibility=new HashMap<>();
    private final Map<Long,List<Target>> live=new HashMap<>();
    static ReminderTargets read(Connection c)throws SQLException {
        var index=new ReminderTargets();
        try(var q=c.prepareStatement("SELECT node_id,name,tags FROM node");var rs=q.executeQuery()){while(rs.next())index.nodes.put(rs.getLong(1),new Meta(Objects.requireNonNullElse(rs.getString(2),""),rs.getString(3)));}
        try(var q=c.prepareStatement("SELECT node_id,father_id,COALESCE(master_id,0) FROM children ORDER BY node_id");var rs=q.executeQuery()){
            while(rs.next()){long id=rs.getLong(1),master=rs.getLong(3);index.tree.put(id,new Branch(id,rs.getLong(2),master==0?id:master));}
        }
        for(var branch:index.tree.values())if(branch.id()>0&&branch.content()>0&&index.visible(branch.id()))index.live.computeIfAbsent(branch.content(),unused->new ArrayList<>()).add(new Target(branch.id(),branch.content(),index.nodes.get(branch.content()).name()));
        return index;
    }
    Target occurrence(long id){var b=tree.get(id);return b!=null&&visible(id)?new Target(id,b.content(),nodes.get(b.content()).name()):null;}
    Target content(long id,long preferred){
        var exact=occurrence(preferred);if(exact!=null&&exact.contentId()==id)return exact;
        var master=occurrence(id);if(master!=null&&master.contentId()==id)return master;
        var choices=live.getOrDefault(id,List.of());return choices.isEmpty()?null:choices.get(0);
    }
    private boolean visible(long id){
        var path=new LinkedHashSet<Long>();boolean valid=true;
        while(id!=0){
            if(visibility.containsKey(id)){valid=visibility.get(id);break;}
            if(!path.add(id)){valid=false;break;}
            var b=tree.get(id);var meta=b==null?null:nodes.get(b.content());var master=b==null?null:tree.get(b.content());
            if(meta==null||master==null||master.content()!=master.id()){valid=false;break;}
            var tags=NodeTags.split(meta.tags());
            if(tags.contains(TrashSql.TAG)||tags.contains(AuditSql.TAG)||tags.contains(ReminderStore.TAG)){valid=false;break;}
            id=b.parent();
        }
        for(long member:path)visibility.put(member,valid);return valid;
    }
}

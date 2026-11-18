package com.turkerozturk.node.fragments;

import java.sql.*;
import java.util.*;

/** Metadata gate runs before loading any note body or object payload. */
final class NodeFragmentSource {
    record Source(long contentId,String name,String syntax) { }
    private static final Set<String> MANAGED=Set.of("__sweetcherry_trash_v1__","__sweetcherry_audit_v1__","__sweetcherry_reminders_v1__");
    static Source read(Connection c,long id,NodeFragmentSettings settings)throws SQLException {
        if(id<=0)throw new NodeFragmentService.Problem("missing");
        String sql="SELECT c.father_id,n.node_id,n.name,n.syntax,n.tags,COALESCE(LENGTH(n.txt),0) FROM children c JOIN node n ON n.node_id=CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END JOIN children m ON m.node_id=n.node_id AND COALESCE(m.master_id,0)=0 WHERE c.node_id=?";
        Source source=null;long chars=0;var path=new HashSet<Long>();
        try(var q=c.prepareStatement(sql)){
            while(id!=0){
                if(path.size()>=256||!path.add(id))throw new NodeFragmentService.Problem("missing");
                q.setLong(1,id);try(var rs=q.executeQuery()){
                    if(!rs.next())throw new NodeFragmentService.Problem("missing");
                    String tags=Objects.requireNonNullElse(rs.getString(5),"");
                    if(Arrays.stream(tags.split(" +")).anyMatch(tag->MANAGED.contains(tag.toLowerCase(Locale.ROOT))))throw new NodeFragmentService.Problem("managed");
                    if(source==null){source=new Source(rs.getLong(2),Objects.requireNonNullElse(rs.getString(3),""),Objects.requireNonNullElse(rs.getString(4),"plain-text"));chars=rs.getLong(6);}
                    id=rs.getLong(1);
                }
            }
        }
        if(source==null)throw new NodeFragmentService.Problem("missing");
        if(chars>settings.characters())throw new NodeFragmentService.Problem("large");
        int objects=0;String bytes=c.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")?"OCTET_LENGTH(png)":"LENGTH(png)";
        try(var q=c.prepareStatement("SELECT COUNT(*),COALESCE(SUM(COALESCE("+bytes+",0)),0) FROM image WHERE node_id=?")){
            q.setLong(1,source.contentId());try(var rs=q.executeQuery()){rs.next();long count=rs.getLong(1);if(count>settings.objects()||rs.getLong(2)>settings.objectBytes())throw new NodeFragmentService.Problem("large");objects=(int)count;}
        }
        for(String table:List.of("codebox","grid"))try(var q=c.prepareStatement("SELECT COUNT(*),COALESCE(SUM(COALESCE(LENGTH(txt),0)),0) FROM "+table+" WHERE node_id=?")){
            q.setLong(1,source.contentId());try(var rs=q.executeQuery()){rs.next();if(rs.getLong(1)>settings.objects()-objects||rs.getLong(2)>settings.characters()-chars)throw new NodeFragmentService.Problem("large");objects+=(int)rs.getLong(1);chars+=rs.getLong(2);}
        }
        return source;
    }
}

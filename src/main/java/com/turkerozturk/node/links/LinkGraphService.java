package com.turkerozturk.node.links;

import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.node.Node;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

/** Derived, bounded, tenant/view-scoped index; never writes a CTB or reads binary payloads. */
@Service
@Transactional(readOnly=true)
public class LinkGraphService {
    @PersistenceContext private EntityManager entityManager;
    private final InternalLinkReportService links;
    public LinkGraphService(InternalLinkReportService links){this.links=links;}
    public record Vertex(String id,String name,String icon,boolean bold,String color,boolean shared,String zone){}
    public record Edge(String id,String source,String target,String kind,int count){}
    public record Graph(String focus,List<Vertex> nodes,List<Edge> edges,boolean more,boolean partial,long unreadable,long broken,long indexedAt){}
    record Place(long id,long parent,String name,String icon,boolean bold,String color,boolean shared){}
    record Arc(long source,long target,String kind){}
    record Index(Map<Long,Place> places,Map<Arc,Integer> arcs,boolean partial,long unreadable,long broken,long at){}
    private record Key(String tenant,String view){}
    private final Map<Key,Index> cache=new LinkedHashMap<>(16,.75f,true);
    public synchronized Graph graph(long focus,String view,boolean hierarchy,int limit,boolean refresh){
        if(focus<0||view==null||view.length()>128||limit<10||limit>200)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        String tenant=TenantContext.getCurrentTenant();if(tenant==null)throw new ResponseStatusException(HttpStatus.CONFLICT);
        var key=new Key(tenant,view);var index=cache.get(key);long now=System.currentTimeMillis();
        if(refresh||index==null||now-index.at()>30000){index=load();cache.put(key,index);while(cache.size()>8)cache.remove(cache.keySet().iterator().next());}
        if(focus==0)focus=index.places().keySet().stream().min(Long::compare).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!index.places().containsKey(focus))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return select(index,focus,hierarchy,limit);
    }
    private Index load(){
        var places=entityManager.unwrap(Session.class).doReturningWork(c->{
            var result=new LinkedHashMap<Long,Place>();
            try(var q=c.prepareStatement("SELECT c.node_id,c.father_id,COALESCE(c.master_id,0),n.name,n.is_ro,n.is_richtxt,n.syntax FROM children c JOIN node n ON n.node_id=CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END ORDER BY c.node_id")){
                q.setMaxRows(20001);try(var rows=q.executeQuery()){while(rows.next()){
                    if(result.size()>=20000)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
                    var node=new Node();node.setIsReadOnly16bit(rows.getLong(5));node.setIsRichText(rows.getLong(6));node.setSyntax(rows.getString(7));node.parseNodeIconAndReadOnlyFlag();node.parseNodeTitleColorAndBoldnessAndTextType();
                    result.put(rows.getLong(1),new Place(rows.getLong(1),rows.getLong(2),Objects.requireNonNullElse(rows.getString(4),""),node.getNodeIcon().getIconName(),node.isBoldnessBit(),String.format(Locale.ROOT,"#%06X",node.getTitleColor()),rows.getLong(3)!=0));
                }}
            }return result;
        });
        var report=links.graphIndex();var arcs=new LinkedHashMap<Arc,Integer>();
        for(var row:report.rows())if(row.target()!=null&&row.target().available()&&places.containsKey(row.source().id())&&places.containsKey(row.target().id())){
            arcs.merge(new Arc(row.source().id(),row.target().id(),"link"),1,Integer::sum);
        }
        boolean partial=report.matched()>report.rows().size()||report.issues().stream().anyMatch(i->i.reason().equals("scanLimit"));
        return new Index(Map.copyOf(places),Map.copyOf(arcs),partial,report.unreadable(),report.broken(),System.currentTimeMillis());
    }
    static Graph select(Index index,long focus,boolean hierarchy,int limit){
        var candidates=new ArrayList<Arc>();
        for(var arc:index.arcs().keySet())if(arc.source()==focus||arc.target()==focus)candidates.add(arc);
        if(hierarchy)for(var p:index.places().values())if(p.parent()!=0&&index.places().containsKey(p.parent())&&(p.id()==focus||p.parent()==focus))candidates.add(new Arc(p.parent(),p.id(),"tree"));
        candidates.sort(Comparator.comparing(Arc::kind).thenComparingLong(Arc::source).thenComparingLong(Arc::target));
        var vertices=new LinkedHashMap<Long,String>();vertices.put(focus,"focus");var edges=new ArrayList<Edge>();boolean more=false;
        for(var arc:candidates){long other=arc.source()==focus?arc.target():arc.source();
            if(!vertices.containsKey(other)&&vertices.size()>=limit+1){more=true;continue;}
            String zone=arc.kind().equals("tree")?(arc.source()==focus?"child":"parent"):(arc.source()==focus?"out":"in");
            vertices.merge(other,zone,(old,value)->old.equals("focus")?old:old.equals(value)?old:"both");
            edges.add(new Edge("e"+edges.size(),Long.toString(arc.source()),Long.toString(arc.target()),arc.kind(),index.arcs().getOrDefault(arc,1)));
        }
        var nodes=new ArrayList<Vertex>();for(var item:vertices.entrySet()){var p=index.places().get(item.getKey());nodes.add(new Vertex(Long.toString(p.id()),p.name(),p.icon(),p.bold(),p.color(),p.shared(),item.getValue()));}
        return new Graph(Long.toString(focus),List.copyOf(nodes),List.copyOf(edges),more,index.partial(),index.unreadable(),index.broken(),index.at());
    }
}

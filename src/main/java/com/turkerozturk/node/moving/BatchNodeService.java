package com.turkerozturk.node.moving;

import com.turkerozturk.node.NodeTimestamps;
import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.node.tags.NodeTags;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.sql.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

/** Explicit, bounded selections. Validate a fresh revision before writing one atomic transaction. */
@Service
public class BatchNodeService {
 @PersistenceContext private EntityManager entityManager;
 private final NodePropertiesService properties;
 public BatchNodeService(NodePropertiesService properties){this.properties=properties;}
 public record Item(String id,String contentId,String name){}
 public record Preview(String revision,List<Item> nodes,int moveRoots,int contentNodes,List<NodeMoveService.BookmarkTarget> bookmarks){}
 public record Result(int changed){}
 private record Content(long id,String name,String tags,Long modified){}
 private record Snapshot(List<NodeMoveService.Row> tree,List<Long> ids,Map<Long,Content> content,Map<Long,Long> masters){}
 private void writable(){if(!properties.writable())throw error(HttpStatus.FORBIDDEN,"readonly");}
 @Transactional(readOnly=true) public Preview preview(List<Long> ids){writable();return entityManager.unwrap(Session.class).doReturningWork(c->preview(c,ids));}
 @Transactional public Result apply(List<Long> ids,String revision,String operation,String parentId,String tag){
  writable();var result=entityManager.unwrap(Session.class).doReturningWork(c->apply(c,ids,revision,operation,parentId,tag));entityManager.clear();return result;
 }
 static List<Long> selection(List<Long> ids){
  if(ids==null||ids.isEmpty()||ids.size()>200||ids.stream().anyMatch(id->id==null||id<=0))throw error(HttpStatus.BAD_REQUEST,"selection");
  return ids.stream().distinct().sorted().toList();
 }
 static Preview preview(Connection c,List<Long> ids)throws SQLException{
  var s=read(c,selection(ids),false);return new Preview(hash(s),s.ids.stream().map(id->{var n=s.content.get(s.masters.get(id));return new Item(id.toString(),Long.toString(n.id),n.name);}).toList(),roots(s).size(),s.content.size(),com.turkerozturk.bookmark.BookmarkShortcuts.read(c).stream().map(n->new NodeMoveService.BookmarkTarget(n.nodeId(),n.name(),n.iconName(),n.bold(),n.color())).toList());
 }
 static Result apply(Connection c,List<Long> ids,String revision,String operation,String parentId,String tag)throws SQLException{
  var s=read(c,selection(ids),true);
  if(!hash(s).equals(revision))throw error(HttpStatus.CONFLICT,"changed");
  return switch(operation){case "move"->move(c,s,parentId);case "tag"->tag(c,s,tag);default->throw error(HttpStatus.BAD_REQUEST,"invalid");};
 }
 private static Snapshot read(Connection c,List<Long> ids,boolean lock)throws SQLException{
  String suffix=lock&&!c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")?" FOR UPDATE":"";
  // Lock the structural snapshot first, consistently for all batch writes on server databases.
  if(!suffix.isEmpty())try(var q=c.prepareStatement("SELECT node_id FROM children ORDER BY node_id"+suffix);var rows=q.executeQuery()){while(rows.next()){} }
  var tree=NodeMoveService.read(c);var index=new HashMap<Long,NodeMoveService.Row>();tree.forEach(n->index.put(n.id(),n));
  var masters=new LinkedHashMap<Long,Long>();var content=new TreeMap<Long,Content>();
  for(long id:ids){var n=index.get(id);if(n==null)throw error(HttpStatus.NOT_FOUND,"missing");long master=n.master()==0?id:n.master();var real=index.get(master);if(real==null||real.master()!=0)throw error(HttpStatus.CONFLICT,"changed");masters.put(id,master);}
  for(long master:new TreeSet<>(masters.values()))try(var q=c.prepareStatement("SELECT name,tags,ts_lastsave FROM node WHERE node_id=?"+suffix)){
   q.setLong(1,master);try(var rs=q.executeQuery()){if(!rs.next())throw error(HttpStatus.NOT_FOUND,"missing");long ts=rs.getLong(3);Long stamp=rs.wasNull()?null:ts;content.put(master,new Content(master,Objects.requireNonNullElse(rs.getString(1),""),rs.getString(2),stamp));}
  }
  return new Snapshot(tree,ids,content,masters);
 }
 /** Topmost selected occurrences, in original tree/sibling order, keep their descendants attached. */
 private static List<NodeMoveService.Row> roots(Snapshot s){
  var index=new HashMap<Long,NodeMoveService.Row>();s.tree.forEach(n->index.put(n.id(),n));var chosen=new HashSet<>(s.ids);
  var roots=new ArrayList<NodeMoveService.Row>();for(long id:s.ids){var n=index.get(id);boolean nested=false;for(long p=n.parent();p!=0;p=index.get(p).parent())if(chosen.contains(p)){nested=true;break;}if(!nested)roots.add(n);}
  roots.sort((a,b)->{var x=path(index,a);var y=path(index,b);for(int i=0;i<Math.min(x.size(),y.size());i++){int cmp=Comparator.comparingLong(NodeMoveService.Row::sequence).thenComparingLong(NodeMoveService.Row::id).compare(x.get(i),y.get(i));if(cmp!=0)return cmp;}return Integer.compare(x.size(),y.size());});return roots;
 }
 private static List<NodeMoveService.Row> path(Map<Long,NodeMoveService.Row> index,NodeMoveService.Row row){var result=new ArrayList<NodeMoveService.Row>();for(var n=row;n!=null;n=index.get(n.parent()))result.add(n);Collections.reverse(result);return result;}
 private static Result move(Connection c,Snapshot s,String raw)throws SQLException{
  long parent;try{if(raw==null||!raw.matches("[0-9]{1,19}"))throw new NumberFormatException();parent=Long.parseLong(raw);}catch(NumberFormatException e){throw error(HttpStatus.BAD_REQUEST,"invalid");}
  var index=new HashMap<Long,NodeMoveService.Row>();s.tree.forEach(n->index.put(n.id(),n));
  if(parent!=0&&!index.containsKey(parent))throw error(HttpStatus.NOT_FOUND,"missing");
  for(long p=parent;p!=0;p=index.get(p).parent())if(s.ids.contains(p))throw error(HttpStatus.UNPROCESSABLE_ENTITY,"cycle");
  if(parent!=0){var n=index.get(parent);try(var q=c.prepareStatement("SELECT node_id FROM node WHERE node_id=?")){q.setLong(1,n.master()==0?parent:n.master());try(var rs=q.executeQuery()){if(!rs.next())throw error(HttpStatus.NOT_FOUND,"missing");}}}
  var moving=roots(s).stream().filter(n->n.parent()!=parent).toList();if(moving.isEmpty())return new Result(0);
  var groups=new LinkedHashMap<Long,List<NodeMoveService.Row>>();for(var n:s.tree)groups.computeIfAbsent(n.parent(),p->new ArrayList<>()).add(n);
  groups.values().forEach(list->list.sort(Comparator.comparingLong(NodeMoveService.Row::sequence).thenComparingLong(NodeMoveService.Row::id)));
  var affected=new LinkedHashSet<Long>();for(var n:moving){groups.get(n.parent()).remove(n);affected.add(n.parent());}groups.computeIfAbsent(parent,p->new ArrayList<>()).addAll(moving);affected.add(parent);
  try(var q=c.prepareStatement("UPDATE children SET father_id=?,sequence=? WHERE node_id=?")){
   for(long p:affected){var list=groups.get(p);for(int i=0;i<list.size();i++){q.setLong(1,p);q.setLong(2,i+1L);q.setLong(3,list.get(i).id());if(q.executeUpdate()!=1)throw error(HttpStatus.CONFLICT,"changed");}}
  }
  long now=NodeTimestamps.now();for(var n:moving)NodeTimestamps.touchOccurrence(c,n.id(),now);return new Result(moving.size());
 }
 private static Result tag(Connection c,Snapshot s,String tag)throws SQLException{
  if(tag==null||tag.isBlank()||tag.length()>200||tag.chars().anyMatch(ch->Character.isWhitespace(ch)||Character.isISOControl(ch)||Character.isSpaceChar(ch)))throw error(HttpStatus.BAD_REQUEST,"invalid");
  var updates=new LinkedHashMap<Long,String>();
  // Validate every resulting value before making the first write.
  for(var n:s.content.values()){var tags=new LinkedHashSet<>(NodeTags.split(n.tags));if(tags.add(tag)){String value=String.join(" ",tags);try{NodeTags.normalize(value);}catch(IllegalArgumentException e){throw error(HttpStatus.BAD_REQUEST,"invalid");}updates.put(n.id,value);}}
  try(var q=c.prepareStatement("UPDATE node SET tags=?,ts_lastsave=? WHERE node_id=?")){long now=NodeTimestamps.now();for(var entry:updates.entrySet()){q.setString(1,entry.getValue());q.setLong(2,now);q.setLong(3,entry.getKey());if(q.executeUpdate()!=1)throw error(HttpStatus.CONFLICT,"changed");}}
  return new Result(updates.size());
 }
 private static String hash(Snapshot s){try{var hash=MessageDigest.getInstance("SHA-256");for(var n:s.tree)hash.update((n.id()+":"+n.parent()+":"+n.sequence()+":"+n.master()+";").getBytes(StandardCharsets.UTF_8));hash.update(s.ids.toString().getBytes(StandardCharsets.UTF_8));for(var n:s.content.values()){String raw=n.id+":"+n.name.length()+":"+n.name+":"+n.modified+":"+(n.tags==null?"NULL":"TEXT"+n.tags.length()+":"+n.tags)+";";hash.update(raw.getBytes(StandardCharsets.UTF_8));}return HexFormat.of().formatHex(hash.digest());}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private static ResponseStatusException error(HttpStatus status,String code){return new ResponseStatusException(status,code);}
}

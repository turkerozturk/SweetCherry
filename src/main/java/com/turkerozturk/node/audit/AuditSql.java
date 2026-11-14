package com.turkerozturk.node.audit;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import com.turkerozturk.node.NodeTimestamps;

/** Append-only daily ledgers and chunked source notes, entirely inside the existing CTB schema. */
public final class AuditSql {
 private AuditSql(){}
 public static final String TAG="__sweetcherry_audit_v1__";
 static final String MAGIC="SweetCherry audit v1\n";
 static final int SEGMENT=65536,CHUNK=100000;
 static final JsonMapper JSON=JsonMapper.builder().build();
 public record Header(int version,String kind,String root,String date,int part,String event,long node,String stage){}
 record Helper(long id,long parent,Header header){}
 public record Change(long id,Map<String,String> before,Map<String,String> after){}
 public record TextState(long id,String stage,String syntax,String name,boolean rawNull,String rawInline,String plainInline,
        List<Long> rawParts,List<Long> plainParts,List<Map<String,String>> objects){}
 public record Event(int version,String id,String time,String actor,String action,List<Long> subjects,
        List<Change> nodes,List<Change> tree,List<Change> bookmarks,List<TextState> sources,List<String> requestedObjectDeletions){}
 public record Index(String id,String time,String actor,String action,String summary,List<Long> nodes,boolean complete,Event inline,List<Long> parts){}
 public record History(long rootId,List<String> days,List<Index> events,int total){}
 static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.CONFLICT,"audit.invalid");}
 static List<Change> changes(Map<Long,Map<String,String>> before,Map<Long,Map<String,String>> after,Set<Long> managed,boolean node){
  var result=new ArrayList<Change>();var ids=new TreeSet<>(before.keySet());ids.addAll(after.keySet());
  for(long id:ids){if(managed.contains(id))continue;var a=before.get(id);var b=after.get(id);
   // This initial scope does not audit creation/deletion: don't attribute unrelated new rows.
   if(node&&(a==null||b==null))continue;
   var x=a==null?null:new LinkedHashMap<>(a);var y=b==null?null:new LinkedHashMap<>(b);if(node){x.remove("ts_lastsave");y.remove("ts_lastsave");}
   if(!Objects.equals(x,y))result.add(new Change(id,a,b));
  }return result;
 }
 static void append(Connection c,AuditSnapshot before,AuditSnapshot after,List<Long> subjects,String action,
        List<String> deletions,String actor,Instant time,int limit)throws SQLException{
  var managed=new HashSet<Long>();before.nodes().forEach((id,n)->{if(AuditSnapshot.managed(n))managed.add(id);});after.nodes().forEach((id,n)->{if(AuditSnapshot.managed(n))managed.add(id);});
  var nodeChanges=changes(before.nodes(),after.nodes(),managed,true);var treeChanges=changes(before.tree(),after.tree(),managed,false);var bookmarkChanges=changes(before.bookmarks(),after.bookmarks(),managed,false);
  treeChanges.removeIf(change->change.before==null);
  treeChanges=treeChanges.stream().map(change->new Change(change.id,placement(before,change.before),placement(after,change.after))).toList();bookmarkChanges.removeIf(change->change.before==null&&!subjects.contains(change.id));
  var changedSources=new ArrayList<AuditSnapshot.Source>();var stages=new ArrayList<String>();
  for(long id:before.sources().keySet()){var a=before.sources().get(id);var b=after.sources().get(id);if(!Objects.equals(a,b)){changedSources.add(a);stages.add("before");if(b!=null){changedSources.add(b);stages.add("after");}}}
  if(nodeChanges.isEmpty()&&treeChanges.isEmpty()&&bookmarkChanges.isEmpty()&&changedSources.isEmpty())return;
  String event=UUID.randomUUID().toString(),day=time.atZone(ZoneOffset.UTC).toLocalDate().toString();long now=time.getEpochSecond();
  var helpers=helpers(c);var roots=helpers.values().stream().filter(h->h.header.kind.equals("root")).toList();if(roots.size()>1)throw invalid();
  Helper root;
  if(roots.isEmpty()){
   if(!helpers.isEmpty())throw invalid();try(var q=c.prepareStatement("SELECT node_id FROM node WHERE name='CTBLOGS'");var rs=q.executeQuery()){if(rs.next())throw invalid();}
   var h=new Header(1,"root",UUID.randomUUID().toString(),"",0,"",0,"");root=new Helper(insert(c,"CTBLOGS",h,"",0,now),0,h);helpers.put(root.id,root);
  }else root=roots.get(0);
  for(var h:helpers.values())if(!h.header.root.equals(root.header.root))throw invalid();
  var days=helpers.values().stream().filter(h->h.header.kind.equals("day")&&h.header.date.equals(day)).toList();if(days.size()>1)throw invalid();
  Helper date;
  if(days.isEmpty()){var h=new Header(1,"day",root.header.root,day,0,"",0,"");date=new Helper(insert(c,day+" (UTC)",h,"",root.id,now),root.id,h);helpers.put(date.id,date);}else date=days.get(0);
  final Helper selectedDate=date;var ledgers=helpers.values().stream().filter(h->h.header.kind.equals("ledger")&&h.parent==selectedDate.id).sorted(Comparator.comparingInt(h->h.header.part)).toList();
  var allIds=new TreeSet<Long>(subjects);nodeChanges.forEach(v->allIds.add(v.id));treeChanges.forEach(v->allIds.add(v.id));bookmarkChanges.forEach(v->allIds.add(v.id));
  var writer=new Writer(c,root.header.root,day,event,date.id,now,limit,time.atZone(ZoneOffset.UTC).toLocalTime().withNano(0)+"Z — "+action+" — "+summary(before,after,subjects,allIds));
  var sources=new ArrayList<TextState>();for(int n=0;n<changedSources.size();n++)sources.add(writer.source(changedSources.get(n),stages.get(n),(stages.get(n).equals("before")?before:after).nodes().get(changedSources.get(n).id()).get("name")));
  var record=new Event(1,event,time.toString(),safe(actor),action,subjects,nodeChanges,treeChanges,bookmarkChanges,sources,deletions);
  String json=JSON.writeValueAsString(record);writer.budget(json.length());
  boolean complete=allIds.size()<=1000;var index=new Index(event,time.toString(),safe(actor),action,summary(before,after,subjects,allIds),allIds.stream().limit(1000).toList(),complete,json.length()<=12000?record:null,json.length()<=12000?List.of():writer.parts("event",0,json));
  String line=JSON.writeValueAsString(index)+"\n";if(line.length()>SEGMENT-2048)throw AuditSnapshot.limit();
  Helper ledger=ledgers.isEmpty()?null:ledgers.get(ledgers.size()-1);String text=ledger==null?"":body(c,ledger.id);
  if(ledger!=null)for(String previous:text.split("\n")){if(previous.isBlank())continue;try{validate(JSON.readValue(previous,Index.class),day);}catch(RuntimeException broken){throw invalid();}}
  if(ledger==null||text.length()+line.length()>SEGMENT){int part=ledger==null?1:ledger.header.part+1;var h=new Header(1,"ledger",root.header.root,day,part,"",0,"");ledger=new Helper(insert(c,String.format(Locale.ROOT,"Events %04d",part),h,"",date.id,now),date.id,h);text="";}
  try(var q=c.prepareStatement("UPDATE node SET txt=?,ts_lastsave=? WHERE node_id=?")){q.setString(1,encode(ledger.header,text+line));q.setLong(2,now);q.setLong(3,ledger.id);if(q.executeUpdate()!=1)throw invalid();}
  NodeTimestamps.touchContent(c,date.id,now);NodeTimestamps.touchContent(c,root.id,now);
 }
 static final class Writer {
  final Connection c;final String root,day,event;final long date,now;final int limit;final String title;long detail=0,characters=0;
  Writer(Connection c,String root,String day,String event,long date,long now,int limit,String title){this.title=crop(title,255);this.c=c;this.root=root;this.day=day;this.event=event;this.date=date;this.now=now;this.limit=limit;}
  void budget(long n){if((characters+=n)>limit)throw AuditSnapshot.limit();}
  List<Long> parts(String stage,long node,String text)throws SQLException{
   budget(text.length());if(detail==0){var h=new Header(1,"detail",root,day,0,event,0,"");detail=insert(c,title,h,"",date,now);}
   var ids=new ArrayList<Long>();int part=0;for(int start=0;start<text.length();){int end=Math.min(text.length(),start+CHUNK);if(end<text.length()&&Character.isHighSurrogate(text.charAt(end-1))&&Character.isLowSurrogate(text.charAt(end)))end--;
    var h=new Header(1,"source",root,day,part++,event,node,stage);ids.add(insert(c,stage+" ["+node+"] — "+part,h,text.substring(start,end),detail,now));start=end;
   }return List.copyOf(ids);
  }
  TextState source(AuditSnapshot.Source source,String stage,String name)throws SQLException{
   String raw=source.raw(),plain=source.plain();boolean small=(raw==null?0:raw.length())+plain.length()<=4000;
   if(small)return new TextState(source.id(),stage,source.syntax(),name,raw==null,raw,plain,List.of(),List.of(),source.objects());
   var rawParts=raw==null?List.<Long>of():parts(stage+"/raw",source.id(),raw);
   var plainParts=Objects.equals(raw,plain)?rawParts:parts(stage+"/plain",source.id(),plain);
   return new TextState(source.id(),stage,source.syntax(),name,raw==null,null,null,rawParts,plainParts,source.objects());
  }
 }
 public static History history(Connection c,String day,long node,int page)throws SQLException{
  var helpers=helpers(c);var root=helpers.values().stream().filter(h->h.header.kind.equals("root")).findFirst().orElse(null);if(root==null)return new History(0,List.of(),List.of(),0);
  var days=helpers.values().stream().filter(h->h.header.kind.equals("day")).map(h->h.header.date).distinct().sorted(Comparator.reverseOrder()).toList();
  var wanted=new HashSet<Long>();wanted.add(node);if(node>0)try(var q=c.prepareStatement("SELECT COALESCE(master_id,0) FROM children WHERE node_id=?")){q.setLong(1,node);try(var rs=q.executeQuery()){if(rs.next()&&rs.getLong(1)>0)wanted.add(rs.getLong(1));}}
  var result=new ArrayList<Index>();
  for(var helper:helpers.values())if(helper.header.kind.equals("ledger")&&helper.header.date.equals(day)){
   String text=body(c,helper.id);if(text.length()>SEGMENT)throw invalid();for(String line:text.split("\n")){if(line.isBlank())continue;Index index;try{index=JSON.readValue(line,Index.class);}catch(RuntimeException e){throw invalid();}
    validate(index,day);if(node>0&&index.nodes.stream().noneMatch(wanted::contains)){if(index.complete)continue;var event=resolve(c,helpers,index);if(affected(event).stream().noneMatch(wanted::contains))continue;}result.add(index);
   }
  }
  result.sort(Comparator.comparing((Index value)->Instant.parse(value.time)).reversed().thenComparing(Index::id));int total=result.size();int start=(int)Math.min((long)Math.max(0,page)*100,total);return new History(root.id,days,List.copyOf(result.subList(start,Math.min(start+100,total))),total);
 }
 private static void validate(Index index,String day){try{if(!UUID.fromString(index.id).toString().equals(index.id)||!Instant.parse(index.time).atZone(ZoneOffset.UTC).toLocalDate().toString().equals(day)||index.nodes==null||index.parts==null||index.action==null)throw invalid();}catch(RuntimeException e){throw invalid();}}
 static Set<Long> affected(Event e){var ids=new TreeSet<Long>(e.subjects);e.nodes.forEach(n->ids.add(n.id));e.tree.forEach(n->ids.add(n.id));e.bookmarks.forEach(n->ids.add(n.id));return ids;}
 public static Event event(Connection c,String id)throws SQLException{
  try{UUID.fromString(id);}catch(RuntimeException e){throw invalid();}var helpers=helpers(c);
  for(var h:helpers.values())if(h.header.kind.equals("ledger"))for(String line:body(c,h.id).split("\n")){if(line.isBlank())continue;Index index;try{index=JSON.readValue(line,Index.class);}catch(RuntimeException e){throw invalid();}validate(index,h.header.date);if(index.id.equals(id))return resolve(c,helpers,index);}
  throw new ResponseStatusException(HttpStatus.NOT_FOUND,"audit.missing");
 }
 private static Event resolve(Connection c,Map<Long,Helper> helpers,Index index)throws SQLException{
  Event result=index.inline;
  if(result==null){String json=parts(c,helpers,index.parts,index.id,0,"event",67108864);try{result=JSON.readValue(json,Event.class);}catch(RuntimeException e){throw invalid();}}
  if(result==null||result.version!=1||!result.id.equals(index.id)||!result.time.equals(index.time)||!result.action.equals(index.action))throw invalid();return result;
 }
 public static String source(Connection c,Event event,long node,String stage,boolean raw)throws SQLException{
  var text=event.sources.stream().filter(s->s.id==node&&s.stage.equals(stage)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"audit.missing"));
  if(raw&&text.rawNull)return "";String inline=raw?text.rawInline:text.plainInline;if(inline!=null)return inline;
  var ids=raw?text.rawParts:text.plainParts;String expected=stage+(raw||text.plainParts.equals(text.rawParts)?"/raw":"/plain");return parts(c,helpers(c),ids,event.id,node,expected,67108864);
 }
 private static String parts(Connection c,Map<Long,Helper> helpers,List<Long> ids,String event,long node,String stage,int limit)throws SQLException{
  var result=new StringBuilder();int part=0;for(long id:ids){var h=helpers.get(id);if(h==null||!h.header.kind.equals("source")||!h.header.event.equals(event)||h.header.node!=node||h.header.part!=part++||!h.header.stage.equals(stage))throw invalid();String text=body(c,id);if(text.length()>CHUNK||result.length()+text.length()>limit)throw invalid();result.append(text);}return result.toString();
 }
 static String name(AuditSnapshot snapshot,long id){if(id==0)return "[root]";var row=snapshot.tree().get(id);long content=row==null?id:AuditSnapshot.number(row.get("master_id"));if(content==0)content=id;var node=snapshot.nodes().get(content);String name=node==null?"?":Objects.requireNonNullElse(node.get("name"),"");return name.length()>160?crop(name,160)+"…":name;}
 static String summary(AuditSnapshot before,AuditSnapshot after,List<Long> subjects,Set<Long> all){var ids=subjects.isEmpty()?new ArrayList<>(all):subjects;return ids.stream().limit(3).map(id->name(before,id)+" ["+id+"]").collect(java.util.stream.Collectors.joining(", "))+(ids.size()>3?" …":"");}
 static Map<String,String> placement(AuditSnapshot snapshot,Map<String,String> values){if(values==null)return null;var result=new LinkedHashMap<>(values);result.put("parentName",name(snapshot,AuditSnapshot.number(values.get("father_id"))));return result;}
 static String crop(String value,int length){if(value.length()<=length)return value;int end=length;if(end>0&&Character.isHighSurrogate(value.charAt(end-1)))end--;return value.substring(0,end);}
 static String safe(String text){String value=Objects.requireNonNullElse(text,"unknown").replaceAll("[\\p{Cntrl}\\p{Cf}]"," ");return crop(value,160);}
 static String encode(Header h,String body){return MAGIC+JSON.writeValueAsString(h)+"\n\n"+body;}
 static Header header(String text){try{if(text==null||!text.startsWith(MAGIC))throw invalid();int end=text.indexOf("\n\n",MAGIC.length());if(end<0)throw invalid();var h=JSON.readValue(text.substring(MAGIC.length(),end),Header.class);if(h.version!=1||!UUID.fromString(h.root).toString().equals(h.root)||!Set.of("root","day","ledger","detail","source").contains(h.kind))throw invalid();if(!h.kind.equals("root"))LocalDate.parse(h.date);return h;}catch(RuntimeException e){throw invalid();}}
 static Map<Long,Helper> helpers(Connection c)throws SQLException{
  var result=new LinkedHashMap<Long,Helper>();
  try(var q=c.prepareStatement("SELECT n.node_id,c.father_id,COALESCE(c.master_id,0),SUBSTR(n.txt,1,2048),n.tags,n.syntax FROM node n JOIN children c ON c.node_id=n.node_id WHERE n.tags=? ORDER BY n.node_id")){
   q.setString(1,TAG);try(var rs=q.executeQuery()){while(rs.next()){if(!TAG.equals(rs.getString(5)))continue;long id=rs.getLong(1),parent=rs.getLong(2);var h=header(rs.getString(4));if(rs.getLong(3)!=0||!"plain-text".equals(rs.getString(6))||h.kind.equals("root")&&parent!=0)throw invalid();result.put(id,new Helper(id,parent,h));}}
  }
  var roots=result.values().stream().filter(h->h.header.kind.equals("root")).toList();if(!result.isEmpty()&&roots.size()!=1)throw invalid();
  if(!roots.isEmpty())for(var helper:result.values())if(!helper.header.root.equals(roots.get(0).header.root))throw invalid();
  for(var h:result.values())if(!h.header.kind.equals("root")){var p=result.get(h.parent);if(p==null||!p.header.root.equals(h.header.root)||!switch(h.header.kind){case "day"->p.header.kind.equals("root");case "ledger","detail"->p.header.kind.equals("day")&&p.header.date.equals(h.header.date);case "source"->p.header.kind.equals("detail")&&p.header.event.equals(h.header.event);default->false;})throw invalid();}
  try(var q=c.prepareStatement("SELECT c.master_id FROM children c JOIN node n ON n.node_id=c.master_id WHERE n.tags=?")){q.setString(1,TAG);try(var rs=q.executeQuery()){while(rs.next())if(result.containsKey(rs.getLong(1)))throw invalid();}}
  return result;
 }
 static String body(Connection c,long id)throws SQLException{try(var q=c.prepareStatement("SELECT txt FROM node WHERE node_id=? AND tags=?")){q.setLong(1,id);q.setString(2,TAG);try(var rs=q.executeQuery()){if(!rs.next())throw invalid();String text=rs.getString(1);header(text);return text.substring(text.indexOf("\n\n",MAGIC.length())+2);}}}
 static long insert(Connection c,String title,Header h,String content,long parent,long now)throws SQLException{
  long id,sequence;try(var q=c.prepareStatement("SELECT COALESCE(MAX(node_id),0)+1 FROM (SELECT node_id FROM node UNION ALL SELECT node_id FROM children) ids");var rs=q.executeQuery()){rs.next();id=rs.getLong(1);if(id<=0)throw invalid();}
  try(var q=c.prepareStatement("SELECT COALESCE(MAX(sequence),0)+1 FROM children WHERE father_id=?")){q.setLong(1,parent);try(var rs=q.executeQuery()){rs.next();sequence=rs.getLong(1);}}
  try(var q=c.prepareStatement("INSERT INTO node(node_id,name,txt,syntax,tags,is_ro,is_richtxt,has_codebox,has_table,has_image,level,ts_creation,ts_lastsave) VALUES(?,?,?,'plain-text',?,1,0,0,0,0,3,?,?)")){q.setLong(1,id);q.setString(2,title);q.setString(3,encode(h,content));q.setString(4,TAG);q.setLong(5,now);q.setLong(6,now);q.executeUpdate();}
  try(var q=c.prepareStatement("INSERT INTO children(node_id,father_id,sequence,master_id) VALUES(?,?,?,0)")){q.setLong(1,id);q.setLong(2,parent);q.setLong(3,sequence);q.executeUpdate();}return id;
 }
 public static void guardHelper(Connection c,long id)throws SQLException{try(var q=c.prepareStatement("SELECT tags FROM node WHERE node_id=?")){q.setLong(1,id);try(var rs=q.executeQuery()){if(rs.next()&&TAG.equals(rs.getString(1)))throw new ResponseStatusException(HttpStatus.CONFLICT,"audit.protected");}}}
 public static void guardMove(Connection c,Collection<Long> ids,long parent)throws SQLException{
  if(parent>0)guardHelper(c,parent);for(long id:ids)guardHelper(c,id);
  // Also protect an externally relocated log container from subtree deletion/copy/move.
  var helperIds=new HashSet<Long>();try(var q=c.prepareStatement("SELECT node_id,tags FROM node WHERE tags=?")){q.setString(1,TAG);try(var rs=q.executeQuery()){while(rs.next())if(TAG.equals(rs.getString(2)))helperIds.add(rs.getLong(1));}}
  if(helperIds.isEmpty()||ids.isEmpty())return;var parents=new HashMap<Long,Long>();try(var q=c.prepareStatement("SELECT node_id,father_id FROM children");var rs=q.executeQuery()){while(rs.next())parents.put(rs.getLong(1),rs.getLong(2));}
  for(long helper:helperIds){var seen=new HashSet<Long>();for(long p=helper;p!=0&&seen.add(p);p=parents.getOrDefault(p,0L))if(ids.contains(p))throw new ResponseStatusException(HttpStatus.CONFLICT,"audit.protected");}
 }
}

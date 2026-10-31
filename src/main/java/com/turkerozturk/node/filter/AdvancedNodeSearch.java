package com.turkerozturk.node.filter;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeService;
import com.turkerozturk.node.tags.NodeTags;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Parameterized, database-side raw text filtering; result loading never selects text or BLOBs. */
@Service @Transactional(readOnly=true)
public class AdvancedNodeSearch {
 @PersistenceContext private EntityManager entityManager;
 private static final Map<String,String> SORT=Map.of("id","c.node_id","name","LOWER(n.name)","created","n.ts_creation","modified","n.ts_lastsave","tags","LOWER(n.tags)","syntax","n.syntax");
 private static final String FROM=" FROM children c JOIN node n ON n.node_id=CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END";
 public Page<Node> search(FormSearch form){return entityManager.unwrap(Session.class).doReturningWork(c->search(c,form));}
 public List<String> syntaxes(){return entityManager.unwrap(Session.class).doReturningWork(c->{var result=new TreeSet<String>();try(var q=c.createStatement();var rs=q.executeQuery("SELECT DISTINCT syntax FROM node WHERE syntax IS NOT NULL")){while(rs.next())result.add(rs.getString(1));}return List.copyOf(result);});}
 record Plan(String where,List<Object> values,String order){}
 static Plan plan(FormSearch f,String product){
  if(!SORT.containsKey(f.getSortBy())||!Set.of("asc","desc").contains(f.getSortDirection())||!Set.of("all","any","phrase").contains(f.getMatchMode())||!Set.of("all","any","contains").contains(f.getTagMode())||f.getPageNumber()<0||f.getPageNumber()>1000000||f.getPageSize()<1||f.getPageSize()>100000)throw new IllegalArgumentException("Invalid search options");
  var predicates=new ArrayList<String>();var values=new ArrayList<Object>();
  text(predicates,values,List.of("n.name","n.txt"),f.getQuery(),f.getMatchMode(),false);
  text(predicates,values,List.of("n.name"),f.getNameFilter(),f.isNameStartsWith()?"phrase":f.getMatchMode(),f.isNameStartsWith());
  text(predicates,values,List.of("n.txt"),f.getTextFilter(),f.getMatchMode(),false);
  text(predicates,values,List.of("n.syntax"),f.getSyntaxFilter(),"phrase",false);
  if(!f.getTagsFilter().isBlank()){
   if(f.getTagMode().equals("contains"))text(predicates,values,List.of("n.tags"),f.getTagsFilter(),"phrase",false);
   else {String padded=product.toLowerCase(Locale.ROOT).matches(".*(mysql|mariadb).*")?"CONCAT(' ',COALESCE(n.tags,''),' ')":"(' ' || COALESCE(n.tags,'') || ' ')";
    var terms=NodeTags.split(f.getTagsFilter().strip());if(terms.size()>32)throw new IllegalArgumentException("At most 32 tags");var tagPredicates=new ArrayList<String>();
    for(String tag:terms){tagPredicates.add("LOWER("+padded+") LIKE LOWER(?) ESCAPE '!'");values.add("% "+escape(tag)+" %");}
    predicates.add("("+String.join(f.getTagMode().equals("all")?" AND ":" OR ",tagPredicates)+")");
   }
  }
  bound(predicates,values,"c.node_id",">=",f.getNodeIdStartFilter());bound(predicates,values,"c.node_id","<=",f.getNodeIdEndFilter());
  date(predicates,values,"n.ts_creation",f.getTsCreationStartFilter(),false);date(predicates,values,"n.ts_creation",f.getTsCreationEndFilter(),true);
  date(predicates,values,"n.ts_lastsave",f.getTsModificationStartFilter(),false);date(predicates,values,"n.ts_lastsave",f.getTsModificationEndFilter(),true);
  if(f.getNodeIcons()!=null&&!f.getNodeIcons().isEmpty()){var icons=NodeService.iconFilterValues(f.getNodeIcons());predicates.add("n.is_ro IN ("+String.join(",",Collections.nCopies(icons.size(),"?"))+")");values.addAll(icons);}
  String order=SORT.get(f.getSortBy())+" "+f.getSortDirection()+(!f.getSortBy().equals("id")?",c.node_id ASC":"");
  return new Plan(predicates.isEmpty()?"":" WHERE "+String.join(" AND ",predicates),List.copyOf(values),order);
 }
 private static void text(List<String> predicates,List<Object> values,List<String> columns,String input,String mode,boolean prefix){
  if(input.length()>2000||input.indexOf('\0')>=0)throw new IllegalArgumentException("Search text too long");
  if(input.isBlank())return;var terms=mode.equals("phrase")?List.of(input.strip()):List.of(input.strip().split("\\s+"));if(terms.size()>32)throw new IllegalArgumentException("At most 32 terms");var checks=new ArrayList<String>();
  for(String term:terms){var alternatives=new ArrayList<String>();for(String column:columns){alternatives.add("LOWER(COALESCE("+column+",'')) LIKE LOWER(?) ESCAPE '!'");values.add((prefix?"":"%")+escape(term)+"%");}checks.add("("+String.join(" OR ",alternatives)+")");}
  predicates.add("("+String.join(mode.equals("any")?" OR ":" AND ",checks)+")");
 }
 private static String escape(String value){return value.replace("!","!!").replace("%","!%").replace("_","!_");}
 private static void bound(List<String> predicates,List<Object> values,String column,String operator,Object value){if(value!=null){predicates.add(column+operator+"?");values.add(value);}}
 private static void date(List<String> predicates,List<Object> values,String column,java.util.Date value,boolean end){if(value==null)return;var day=Instant.ofEpochMilli(value.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();bound(predicates,values,column,end?"<=":">=",(end?day.plusDays(1):day).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()-(end?1:0));}
 static Page<Node> search(Connection c,FormSearch f)throws SQLException{
  var plan=plan(f,c.getMetaData().getDatabaseProductName());long count;
  try(var q=c.prepareStatement("SELECT COUNT(*)"+FROM+plan.where())){bind(q,plan.values());try(var rs=q.executeQuery()){rs.next();count=rs.getLong(1);}}
  int page=(int)Math.min(f.getPageNumber(),Math.max(0,(count-1)/f.getPageSize()));f.setPageNumber(page);var pageable=PageRequest.of(page,f.getPageSize());if(count==0)return new PageImpl<>(List.of(),pageable,0);
  var result=new ArrayList<Node>();String sql="SELECT c.node_id,n.name,n.tags,n.syntax,n.is_ro,n.is_richtxt,n.ts_creation,n.ts_lastsave"+FROM+plan.where()+" ORDER BY "+plan.order()+" LIMIT ? OFFSET ?";
  try(var q=c.prepareStatement(sql)){bind(q,plan.values());q.setInt(plan.values().size()+1,f.getPageSize());q.setLong(plan.values().size()+2,pageable.getOffset());try(var rs=q.executeQuery()){while(rs.next()){var n=new Node();n.setNodeId(rs.getLong(1));n.setName(rs.getString(2));n.setTags(rs.getString(3));n.setSyntax(Objects.requireNonNullElse(rs.getString(4),"plain-text"));n.setIsReadOnly16bit(rs.getLong(5));n.setIsRichText(rs.getLong(6));n.setCreationTimestamp(rs.getLong(7));n.setLastSaveTimestamp(rs.getLong(8));n.parseNodeIconAndReadOnlyFlag();n.parseNodeTitleColorAndBoldnessAndTextType();n.setTitleColorAsHtmlHex(String.format(Locale.ROOT,"#%06X",n.getTitleColor()));result.add(n);}}}
  breadcrumbs(c,result);return new PageImpl<>(result,pageable,count);
 }
 private static void bind(PreparedStatement query,List<Object> values)throws SQLException{for(int i=0;i<values.size();i++)query.setObject(i+1,values.get(i));}
 private record Branch(long parent,String name){}
 private static void breadcrumbs(Connection c,List<Node> result)throws SQLException{
  var branches=new HashMap<Long,Branch>();try(var q=c.createStatement();var rs=q.executeQuery("SELECT c.node_id,c.father_id,n.name"+FROM)){while(rs.next())branches.put(rs.getLong(1),new Branch(rs.getLong(2),Objects.requireNonNullElse(rs.getString(3),"?")));}
  for(Node n:result){var backwards=new LinkedHashMap<Long,String>();var visited=new HashSet<Long>();long id=n.getNodeId();int size=0;while(id!=0){if(!visited.add(id)||visited.size()>200||size>8000){backwards.put(-1L,"… ↻");break;}var b=branches.get(id);if(b==null){backwards.put(id,"?");break;}String name=b.name().length()>500?b.name().substring(0,500)+"…":b.name();backwards.put(id,name);size+=name.length();id=b.parent();}var path=new LinkedHashMap<Long,String>();var ids=new ArrayList<>(backwards.keySet());Collections.reverse(ids);for(long key:ids)path.put(key,backwards.get(key));n.setBreadcrumbs(path);}
 }
}

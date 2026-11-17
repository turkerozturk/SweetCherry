package com.turkerozturk.node.audit;

import java.sql.*;
import java.util.*;
import java.security.*;
import java.io.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Metadata comparisons plus scoped text/object manifests; binary data is hashed, never archived. */
public record AuditSnapshot(Map<Long,Map<String,String>> nodes,Map<Long,Map<String,String>> tree,
        Map<Long,Map<String,String>> bookmarks,Map<Long,Source> sources) {
 public record Source(long id,String syntax,String raw,String plain,List<Map<String,String>> objects){}
 static final String[] NODE_FIELDS={"node_id","name","syntax","tags","is_ro","is_richtxt","has_codebox","has_table","has_image","level","ts_creation","ts_lastsave"};
 static AuditSnapshot read(Connection c,Collection<Long> subjects,boolean content,int limit,boolean lock)throws SQLException{
  String suffix=lock&&!c.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")?" FOR UPDATE":"";
  var nodes=rows(c,"SELECT "+String.join(",",NODE_FIELDS)+" FROM node ORDER BY node_id"+suffix,NODE_FIELDS);
  var tree=rows(c,"SELECT node_id,father_id,sequence,master_id FROM children ORDER BY node_id"+suffix,new String[]{"node_id","father_id","sequence","master_id"});
  var bookmarks=rows(c,"SELECT node_id,sequence FROM bookmark ORDER BY node_id"+suffix,new String[]{"node_id","sequence"});
  var source=new TreeMap<Long,Source>();long characters=0;
  if(content)for(long occurrence:subjects){
   var branch=tree.get(occurrence);if(branch==null)continue;long id=number(branch.get("master_id"));if(id==0)id=occurrence;if(source.containsKey(id)||!nodes.containsKey(id)||managed(nodes.get(id)))continue;
   String raw;try(var q=c.prepareStatement("SELECT txt FROM node WHERE node_id=?"+suffix)){q.setLong(1,id);try(var rs=q.executeQuery()){if(!rs.next())continue;raw=rs.getString(1);}}
   if(raw!=null&&(characters+=raw.length())>limit)throw limit();String syntax=nodes.get(id).get("syntax");String plain=plain(syntax,raw);
   if(!Objects.equals(raw,plain)&&(characters+=plain.length())>limit)throw limit();var objects=new ArrayList<Map<String,String>>();
   for(String table:List.of("image","grid","codebox"))try(var q=c.prepareStatement("SELECT * FROM "+table+" WHERE node_id=?"+suffix)){
    q.setLong(1,id);try(var rs=q.executeQuery()){var meta=rs.getMetaData();while(rs.next()){
     var object=new LinkedHashMap<String,String>();object.put("table",table);
     for(int col=1;col<=meta.getColumnCount();col++){
      String name=meta.getColumnName(col);int type=meta.getColumnType(col);
      if(type==Types.BLOB||type==Types.BINARY||type==Types.VARBINARY||type==Types.LONGVARBINARY){
       try(var in=rs.getBinaryStream(col)){if(in==null){object.put(name+".bytes",null);object.put(name+".sha256",null);}else{var hash=digest();long bytes=0;byte[] data=new byte[8192];for(int n;(n=in.read(data))!=-1;){hash.update(data,0,n);bytes+=n;}object.put(name+".bytes",Long.toString(bytes));object.put(name+".sha256",HexFormat.of().formatHex(hash.digest()));}}catch(IOException e){throw new SQLException("Cannot fingerprint audit object",e);}
      }else{String value=rs.getString(col);if(value!=null&&(characters+=value.length())>limit)throw limit();object.put(name,value);}
     }
     objects.add(Collections.unmodifiableMap(object));if(objects.size()>50000)throw limit();
    }}
   }
   objects.sort(Comparator.comparing(o->o.get("table")+":"+o.get("offset")+":"+o.toString()));source.put(id,new Source(id,syntax,raw,plain,List.copyOf(objects)));
  }
  return new AuditSnapshot(nodes,tree,bookmarks,source);
 }
 private static Map<Long,Map<String,String>> rows(Connection c,String sql,String[] fields)throws SQLException{
  var result=new TreeMap<Long,Map<String,String>>();try(var q=c.prepareStatement(sql);var rs=q.executeQuery()){while(rs.next()){var values=new LinkedHashMap<String,String>();for(int n=0;n<fields.length;n++)values.put(fields[n],rs.getString(n+1));result.put(rs.getLong(1),Collections.unmodifiableMap(values));}}return result;
 }
 static boolean managed(Map<String,String> node){return node!=null&&(AuditSql.TAG.equals(node.get("tags"))||com.turkerozturk.node.TrashSql.TAG.equals(node.get("tags")));}
 static long number(String value){return value==null?0:Long.parseLong(value);}
 static String plain(String syntax,String raw){
  if(raw==null)return "";
  if("custom-colors".equals(syntax))try{return new com.turkerozturk.richtext.experimental.RichTextXmlReader().read(raw).runs().stream().map(r->r.text()).collect(java.util.stream.Collectors.joining());}catch(RuntimeException invalid){return raw;}
  if("html".equals(syntax))return org.jsoup.Jsoup.parse(raw).wholeText();return raw;
 }
 static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
 static ResponseStatusException limit(){return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"audit.limit");}
}

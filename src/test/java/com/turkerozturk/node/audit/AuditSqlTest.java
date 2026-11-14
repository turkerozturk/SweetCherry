package com.turkerozturk.node.audit;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
class AuditSqlTest {
 Connection c;
 @BeforeEach void setup()throws Exception{c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var q=c.createStatement();var in=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")){for(String line:new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\\R"))if(line.startsWith("CREATE TABLE")||line.startsWith("INSERT INTO"))q.execute(line);}c.setAutoCommit(false);}
 @AfterEach void close()throws Exception{c.close();}
 AuditSnapshot state(String tags,String raw){var node=new LinkedHashMap<String,String>();node.put("node_id","2");node.put("name","A <name>");node.put("syntax","plain-text");node.put("tags",tags);node.put("ts_lastsave","100");var sources=raw==null?Map.<Long,AuditSnapshot.Source>of():Map.of(2L,new AuditSnapshot.Source(2,"plain-text",raw,raw,List.of()));return new AuditSnapshot(Map.of(2L,node),Map.of(),Map.of(),sources);}
 void append(AuditSnapshot before,AuditSnapshot after,Instant time)throws Exception{AuditSql.append(c,before,after,List.of(2L),"tags.update",List.of(),"admin",time,16777216);}
 @Test void utcDailyContainersAndLedgerRotationKeepEveryEventWithoutOneNodePerSmallChange()throws Exception{
  var time=Instant.parse("2026-11-14T23:59:59Z");for(int n=0;n<90;n++)append(state("a".repeat(500)+n,null),state("b".repeat(500)+n,null),time.plusNanos(n));append(state("old",null),state("new",null),time.plusSeconds(1));
  var first=AuditSql.history(c,"2026-11-14",0,0);assertThat(first.total()).isEqualTo(90);assertThat(first.events().get(0).time()).isEqualTo(time.plusNanos(89).toString());assertThat(first.days()).containsExactly("2026-11-15","2026-11-14");var second=AuditSql.history(c,"2026-11-15",2,0);assertThat(second.total()).isEqualTo(1);
  var helpers=AuditSql.helpers(c);assertThat(helpers.values().stream().filter(h->h.header().kind().equals("root"))).hasSize(1);assertThat(helpers.values().stream().filter(h->h.header().kind().equals("ledger"))).hasSizeGreaterThan(2);assertThat(helpers.values().stream().filter(h->h.header().kind().equals("detail"))).isEmpty();assertThat(AuditSql.event(c,first.events().get(0).id()).nodes()).hasSize(1);
 }
 @Test void unicodeChunksAndCompleteRawDownloadsAreLossless()throws Exception{
  String before="a".repeat(99999)+"😀"+"Old end",after="b".repeat(120000)+"New end";var time=Instant.parse("2026-11-14T12:00:00Z");append(state("same",before),state("same",after),time);
  var event=AuditSql.event(c,AuditSql.history(c,"2026-11-14",2,0).events().get(0).id());assertThat(event.sources()).hasSize(2);assertThat(event.sources().get(0).rawInline()).isNull();assertThat(event.sources().get(0).rawParts()).hasSize(2);assertThat(AuditSql.source(c,event,2,"before",true)).isEqualTo(before);assertThat(AuditSql.source(c,event,2,"before",false)).isEqualTo(before);assertThat(AuditSql.source(c,event,2,"after",true)).isEqualTo(after);
 }
 @Test void changedRawSourceAndFormattingWithSamePlainTextStillHaveBothVersions()throws Exception{
  var a=state("same",null);var old=new AuditSnapshot.Source(2,"custom-colors","<node><rich_text>A</rich_text></node>","A",List.of());var next=new AuditSnapshot.Source(2,"custom-colors","<node><rich_text weight=\"heavy\">A</rich_text></node>","A",List.of());
  append(new AuditSnapshot(a.nodes(),a.tree(),a.bookmarks(),Map.of(2L,old)),new AuditSnapshot(a.nodes(),a.tree(),a.bookmarks(),Map.of(2L,next)),Instant.parse("2026-11-14T12:00:00Z"));var event=AuditSql.event(c,AuditSql.history(c,"2026-11-14",0,0).events().get(0).id());assertThat(event.sources()).hasSize(2);assertThat(AuditSql.source(c,event,2,"before",true)).doesNotContain("heavy");assertThat(AuditSql.source(c,event,2,"after",true)).contains("heavy");
 }
 @Test void largerEventsAreResolvedFromDetailsAndFilteringDoesNotMissOtherAffectedNodes()throws Exception{
  var before=new TreeMap<Long,Map<String,String>>();var after=new TreeMap<Long,Map<String,String>>();for(long id=1;id<=1100;id++){before.put(id,Map.of("node_id",""+id,"name","N"+id,"tags","old"));after.put(id,Map.of("node_id",""+id,"name","N"+id,"tags","new"));}
  append(new AuditSnapshot(before,Map.of(),Map.of(),Map.of()),new AuditSnapshot(after,Map.of(),Map.of(),Map.of()),Instant.parse("2026-11-14T12:00:00Z"));var history=AuditSql.history(c,"2026-11-14",1100,0);assertThat(history.total()).isEqualTo(1);assertThat(history.events().get(0).complete()).isFalse();assertThat(history.events().get(0).inline()).isNull();assertThat(AuditSql.event(c,history.events().get(0).id()).nodes()).hasSize(1100);
 }
 @Test void snapshotsUseBinaryManifestsAndRetainTableAndCodeboxText()throws Exception{
  try(var q=c.createStatement()){q.execute("INSERT INTO image VALUES(2,0,'left','',X'0102FF','report.bin','',0)");q.execute("INSERT INTO codebox VALUES(2,1,'left','print(42)','python',100,50,1,0,0)");q.execute("INSERT INTO grid VALUES(2,2,'left','<table>old cells</table>',80,90)");}
  var snapshot=AuditSnapshot.read(c,List.of(2L),true,1000000,false);var objects=snapshot.sources().get(2L).objects();assertThat(objects).hasSize(3);assertThat(objects).anyMatch(o->"3".equals(o.get("png.bytes"))&&o.get("png.sha256").length()==64);assertThat(objects).anyMatch(o->"print(42)".equals(o.get("txt")));assertThat(objects).anyMatch(o->"<table>old cells</table>".equals(o.get("txt")));assertThat(objects).noneMatch(o->o.containsKey("png"));
 }
 @Test void missingOrModifiedSourcePartsFailInsteadOfReturningPartialRecoveredText()throws Exception{
  append(state("same","a".repeat(100001)),state("same","b"),Instant.parse("2026-11-14T12:00:00Z"));var e=AuditSql.event(c,AuditSql.history(c,"2026-11-14",0,0).events().get(0).id());long part=e.sources().get(0).rawParts().get(1);try(var q=c.createStatement()){q.execute("DELETE FROM node WHERE node_id="+part);}assertThatThrownBy(()->AuditSql.source(c,e,2,"before",true)).hasMessageContaining("audit.invalid");
 }
}

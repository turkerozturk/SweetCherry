package com.turkerozturk.node.moving;
import com.turkerozturk.node.properties.NodePropertiesService;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class BatchNodeServiceTest {
 Connection c;
 @BeforeEach void setup()throws Exception{
  c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var q=c.createStatement()){
   q.execute("CREATE TABLE children(node_id INTEGER PRIMARY KEY,father_id INTEGER,sequence INTEGER,master_id INTEGER)");
   q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,tags TEXT,ts_lastsave INTEGER,txt TEXT,is_ro INTEGER DEFAULT 0,is_richtxt INTEGER DEFAULT 0,syntax TEXT DEFAULT 'plain-text')");
   q.execute("CREATE TABLE bookmark(node_id INTEGER,sequence INTEGER)");
   q.execute("INSERT INTO node(node_id,name,tags,ts_lastsave,txt) VALUES(1,'A','old',100,'content'),(2,'B','tag',100,'content'),(3,'C',NULL,NULL,'content'),(4,'Child','old',100,'content'),(5,'Deep','old',100,'content')");
   q.execute("INSERT INTO children VALUES(1,0,10,0),(2,0,20,0),(3,0,30,0),(4,1,20,0),(5,4,10,0),(9,0,40,1)");
   q.execute("INSERT INTO bookmark VALUES(2,1)");
  }c.setAutoCommit(false);
 }
 @AfterEach void close()throws Exception{c.close();}
 BatchNodeService.Preview preview(Long... ids)throws Exception{return BatchNodeService.preview(c,List.of(ids));}
 int apply(List<Long> ids,String operation,String parent,String tag)throws Exception{return BatchNodeService.apply(c,ids,BatchNodeService.preview(c,ids).revision(),operation,parent,tag).changed();}
 NodeMoveService.Row row(long id)throws Exception{return NodeMoveService.read(c).stream().filter(n->n.id()==id).findFirst().orElseThrow();}
 String value(long id,String field)throws Exception{try(var q=c.prepareStatement("SELECT "+field+" FROM node WHERE node_id=?")){q.setLong(1,id);try(var rs=q.executeQuery()){rs.next();return rs.getString(1);}}}
 @Test void movesTopmostSelectionsPreservingDescendantsAndOriginalTreeOrder()throws Exception{
  assertThat(preview(4L,1L,9L).moveRoots()).isEqualTo(2);assertThat(apply(List.of(9L,4L,1L),"move","2","")).isEqualTo(2);
  assertThat(row(1).parent()).isEqualTo(2);assertThat(row(1).sequence()).isEqualTo(1);assertThat(row(9).sequence()).isEqualTo(2);
  assertThat(row(4).parent()).isEqualTo(1);assertThat(row(5).parent()).isEqualTo(4);assertThat(row(9).master()).isEqualTo(1);
  assertThat(value(1,"txt")).isEqualTo("content");assertThat(value(4,"ts_lastsave")).isEqualTo("100");
 }
 @Test void appendsInTreeOrderToRootAndSharedParentAndPreservesExistingSiblings()throws Exception{
  assertThat(apply(List.of(5L,4L),"move","9","")).isEqualTo(1);assertThat(row(4).parent()).isEqualTo(9);assertThat(row(5).parent()).isEqualTo(4);
  assertThat(apply(List.of(4L),"move","0","")).isEqualTo(1);assertThat(row(4).sequence()).isEqualTo(5);
 }
 @Test void alreadyAtDestinationIsANoopIncludingTimestampAndOrder()throws Exception{
  var before=NodeMoveService.read(c);assertThat(apply(List.of(2L,3L),"move","0","")).isZero();assertThat(NodeMoveService.read(c)).isEqualTo(before);assertThat(value(2,"ts_lastsave")).isEqualTo("100");
 }
 @Test void rejectsCyclesMissingTargetsAndStaleHierarchyBeforeWriting()throws Exception{
  var snapshot=preview(1L,3L);var before=NodeMoveService.read(c);
  for(String parent:List.of("1","4","5","999","-1","0?mode=memory"))assertThatThrownBy(()->BatchNodeService.apply(c,List.of(1L,3L),snapshot.revision(),"move",parent,"")).isInstanceOf(ResponseStatusException.class);
  assertThat(NodeMoveService.read(c)).isEqualTo(before);
  try(var q=c.createStatement()){q.execute("UPDATE children SET sequence=99 WHERE node_id=2");}
  assertThatThrownBy(()->BatchNodeService.apply(c,List.of(1L,3L),snapshot.revision(),"move","2","")).isInstanceOf(ResponseStatusException.class);assertThat(row(1).parent()).isZero();
 }
 @Test void addsOneLiteralTagToEachMasterOnceAndRetainsExistingTags()throws Exception{
  var snapshot=preview(1L,9L,2L,3L);assertThat(snapshot.contentNodes()).isEqualTo(3);assertThat(snapshot.bookmarks()).hasSize(1);
  assertThat(apply(List.of(1L,9L,2L,3L),"tag","","C++")).isEqualTo(3);assertThat(value(1,"tags")).isEqualTo("old C++");assertThat(value(2,"tags")).isEqualTo("tag C++");assertThat(value(3,"tags")).isEqualTo("C++");
  String timestamp=value(1,"ts_lastsave");assertThat(apply(List.of(1L,9L),"tag","","C++")).isZero();assertThat(value(1,"ts_lastsave")).isEqualTo(timestamp);
  assertThat(apply(List.of(1L),"tag","","one,two")).isEqualTo(1);assertThat(value(1,"tags")).endsWith("one,two");
 }
 @Test void staleTagsIncludingSameSecondCaseChangeRejectAndNoPartialWritesOnOversize()throws Exception{
  var snapshot=preview(1L,2L);try(var q=c.createStatement()){q.execute("UPDATE node SET tags='Old' WHERE node_id=1");}
  assertThatThrownBy(()->BatchNodeService.apply(c,List.of(1L,2L),snapshot.revision(),"tag","","new")).isInstanceOf(ResponseStatusException.class);assertThat(value(2,"tags")).isEqualTo("tag");
  try(var q=c.prepareStatement("UPDATE node SET tags=? WHERE node_id=3")){q.setString(1,"a".repeat(20000));q.executeUpdate();}
  assertThatThrownBy(()->apply(List.of(1L,3L),"tag","","new")).isInstanceOf(ResponseStatusException.class);assertThat(value(1,"tags")).isEqualTo("Old");
 }
 @Test void selectionIdentityAndInputValidationCannotRetargetAnotherOccurrence()throws Exception{
  for(var ids:List.of(List.<Long>of(),List.of(0L),Collections.nCopies(201,1L),List.of(999L)))assertThatThrownBy(()->BatchNodeService.preview(c,ids)).isInstanceOf(ResponseStatusException.class);
  var p=preview(1L);assertThatThrownBy(()->BatchNodeService.apply(c,List.of(9L),p.revision(),"tag","","new")).isInstanceOf(ResponseStatusException.class);
  for(String tag:List.of("","two tags","tabs\there","line\n","x".repeat(201),"a\u0000b"))assertThatThrownBy(()->apply(List.of(1L),"tag","",tag)).isInstanceOf(ResponseStatusException.class);
 }
 @Test void writableGuardAppliesBeforeReadingOrSaving(){var properties=mock(NodePropertiesService.class);var service=new BatchNodeService(properties);assertThatThrownBy(()->service.preview(List.of(1L))).isInstanceOf(ResponseStatusException.class);assertThatThrownBy(()->service.apply(List.of(1L),"r","tag","","new")).isInstanceOf(ResponseStatusException.class);}
}

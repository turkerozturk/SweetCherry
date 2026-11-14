package com.turkerozturk.node.tags;
import java.sql.*;
import org.junit.jupiter.api.*;
import com.turkerozturk.node.properties.NodePropertiesService;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class QuickTagServiceTest {
 Connection c;QuickTagService service;NodePropertiesService properties;
 @BeforeEach void setup()throws Exception{
  c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var q=c.createStatement()){
   q.execute("CREATE TABLE node(node_id INTEGER,name TEXT,tags TEXT COLLATE NOCASE,ts_lastsave INTEGER,txt TEXT,is_richtxt INTEGER)");q.execute("CREATE TABLE children(node_id INTEGER,father_id INTEGER,master_id INTEGER)");
   q.execute("INSERT INTO node VALUES(1,'Name','foo Foo',100,'Protected text',7),(2,'Other','C++ one,two',200,'Other text',0)");q.execute("INSERT INTO children VALUES(1,0,0),(2,0,0),(11,2,1)");
  }
  properties=mock(NodePropertiesService.class);when(properties.writable()).thenReturn(true);service=new QuickTagService(properties);
  var em=mock(jakarta.persistence.EntityManager.class);var session=mock(org.hibernate.Session.class);when(em.unwrap(org.hibernate.Session.class)).thenReturn(session);
  when(session.doReturningWork(any())).thenAnswer(call->((org.hibernate.jdbc.ReturningWork<?>)call.getArgument(0)).execute(c));ReflectionTestUtils.setField(service,"entityManager",em);
 }
 @AfterEach void close()throws Exception{c.close();}
 @Test void editsMasterThroughSharedOccurrenceWithoutChangingOtherProperties()throws Exception{
  var original=service.open(11);assertThat(original.contentId()).isEqualTo("1");assertThat(original.catalogue()).contains("C++","one,two","foo","Foo");
  var result=service.save(11,"C++ one,two New New",original.revision());assertThat(result.tags()).isEqualTo("C++ one,two New");assertThat(result.revision()).isNotEqualTo(original.revision());
  try(var q=c.createStatement();var rows=q.executeQuery("SELECT tags,txt,is_richtxt,ts_lastsave FROM node WHERE node_id=1")){rows.next();assertThat(rows.getString(1)).isEqualTo("C++ one,two New");assertThat(rows.getString(2)).isEqualTo("Protected text");assertThat(rows.getLong(3)).isEqualTo(7);assertThat(rows.getLong(4)).isGreaterThan(100);}
 }
 @Test void unchangedTagsDoNotTouchModificationTime(){var before=service.open(1);assertThat(service.save(1,"foo Foo",before.revision()).revision()).isEqualTo(before.revision());}
 @Test void staleTagsWithSameTimestampAndDifferentCaseCannotOverwrite()throws Exception{
  var original=service.open(1);try(var q=c.createStatement()){q.execute("UPDATE node SET tags='FOO Foo' WHERE node_id=1");}
  assertThatThrownBy(()->service.save(1,"new",original.revision())).hasMessageContaining("changed");assertThat(service.open(1).tags()).isEqualTo("FOO Foo");
 }
 @Test void rejectsWrongOccurrenceMissingMasterReadonlyAndOversizedTags()throws Exception{
  var snapshot=service.open(1);assertThatThrownBy(()->service.save(11,"new",snapshot.revision())).hasMessageContaining("changed");
  assertThatThrownBy(()->service.save(1,"a".repeat(20001),snapshot.revision())).hasMessageContaining("invalid");
  when(properties.writable()).thenReturn(false);assertThatThrownBy(()->service.open(1)).hasMessageContaining("readonly");assertThatThrownBy(()->service.save(1,"new",snapshot.revision())).hasMessageContaining("readonly");when(properties.writable()).thenReturn(true);
  try(var q=c.createStatement()){q.execute("DELETE FROM children WHERE node_id=1");}assertThatThrownBy(()->service.open(11)).hasMessageContaining("missing");
 }
 @Test void permitsRemovingAllTagsAndNormalizesNullWithoutSpuriousWrites()throws Exception{
  var snapshot=service.open(1);assertThat(service.save(1,"",snapshot.revision()).tags()).isEmpty();
  try(var q=c.createStatement()){q.execute("UPDATE node SET tags=NULL,ts_lastsave=10 WHERE node_id=1");}
  var empty=service.open(1);assertThat(service.save(1,"",empty.revision()).revision()).isEqualTo(empty.revision());
  try(var q=c.createStatement()){q.execute("UPDATE node SET ts_lastsave=NULL WHERE node_id=1");}var nullable=service.open(1);assertThat(service.save(1,"new",nullable.revision()).tags()).isEqualTo("new");
 }
}

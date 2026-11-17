package com.turkerozturk.node.reminders;

import com.turkerozturk.calendar.CalendarSettings;
import com.turkerozturk.export.SqliteSchemaLoader;
import com.turkerozturk.node.properties.NodePropertiesService;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReminderServiceTest {
 Connection c;ReminderService service;NodePropertiesService properties;jakarta.persistence.EntityManager manager;
 static final Instant NOW=Instant.parse("2026-11-17T10:00:00Z");
 @BeforeEach void setup()throws Exception{
  c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var sql=c.createStatement()){
   for(String ddl:SqliteSchemaLoader.load().split(";"))if(!ddl.isBlank())sql.execute(ddl);
   sql.execute("INSERT INTO node(node_id,name,txt,tags,syntax,is_ro,is_richtxt,ts_creation,ts_lastsave) VALUES(1,'<Alpha>','Original','todo project','plain-text',0,0,100,200),(2,'Beta','Other','project','plain-text',0,0,300,400)");
   sql.execute("INSERT INTO children(node_id,father_id,sequence,master_id) VALUES(1,0,1,0),(2,0,2,0),(11,2,1,1)");
   sql.execute("INSERT INTO image(node_id,png) VALUES(1,X'010203')");
  }
  properties=mock(NodePropertiesService.class);when(properties.writable()).thenReturn(true);
  service=new ReminderService(properties,new CalendarSettings("MONDAY","Europe/Istanbul",1000),Clock.fixed(NOW,ZoneOffset.UTC));
  manager=mock(jakarta.persistence.EntityManager.class);var session=mock(org.hibernate.Session.class);when(manager.unwrap(org.hibernate.Session.class)).thenReturn(session);
  when(session.doReturningWork(any())).thenAnswer(call->((org.hibernate.jdbc.ReturningWork<?>)call.getArgument(0)).execute(c));ReflectionTestUtils.setField(service,"manager",manager);
 }
 @AfterEach void close()throws Exception{c.close();}
 ReminderService.View view(String status){return service.inspect(new ReminderService.Options("",status,50,1));}
 String save(long node,String local,String note){var e=service.editor("",node);return service.save("",e.nodeId(),e.contentId(),local,e.zone(),note,e.revision());}
 String text(String sql)throws Exception{try(var q=c.createStatement();var rs=q.executeQuery(sql)){rs.next();return rs.getString(1);}}
 @Test void emptyListingAndEditorDoNotCreateStoreAndUseCalendarZone()throws Exception{
  assertThat(view("pending").rows()).isEmpty();var e=service.editor("",11);assertThat(e.nodeId()).isEqualTo(11);assertThat(e.contentId()).isEqualTo(1);
  assertThat(e.zone()).isEqualTo("Europe/Istanbul");assertThat(e.localTime()).isEqualTo("2026-11-17T14:00");
  assertThat(text("SELECT COUNT(*) FROM node")).isEqualTo("2");
 }
 @Test void createSharedRemindersPreservesTargetContentTagsTimestampsAndBlobs()throws Exception{
  String id=save(11,"2026-11-17T16:00","<Call> 😀");var row=view("pending").rows().get(0);
  assertThat(row.id()).isEqualTo(id);assertThat(row.nodeId()).isEqualTo(11);assertThat(row.contentId()).isEqualTo(1);assertThat(row.due()).isEqualTo("2026-11-17T13:00:00Z");
  assertThat(text("SELECT txt FROM node WHERE node_id=1")).isEqualTo("Original");assertThat(text("SELECT tags FROM node WHERE node_id=1")).isEqualTo("todo project");
  assertThat(text("SELECT ts_creation||':'||ts_lastsave FROM node WHERE node_id=1")).isEqualTo("100:200");assertThat(text("SELECT hex(png) FROM image WHERE node_id=1")).isEqualTo("010203");
  var store=ReminderStore.read(c,false);assertThat(store.id()).isGreaterThan(11);assertThat(store.data().items()).hasSize(1);
  assertThat(text("SELECT is_ro||':'||level FROM node WHERE tags='"+ReminderStore.TAG+"'")).isEqualTo("1:3");
 }
 @Test void dueBoundaryFilteringPaginationAndCountersUseOneInstant(){
  save(1,"2026-11-17T13:00","due now");save(1,"2026-11-17T13:01","future");save(2,"2026-11-16T10:00","past");
  var all=view("pending");assertThat(all.overdue()).isEqualTo(2);assertThat(all.upcoming()).isEqualTo(1);assertThat(all.rows()).hasSize(3);
  assertThat(view("upcoming").rows()).hasSize(1);assertThat(view("overdue").rows()).hasSize(2);
  var page=service.inspect(new ReminderService.Options("","all",1,2));assertThat(page.matched()).isEqualTo(3);assertThat(page.first()).isEqualTo(2);assertThat(page.pages()).isEqualTo(3);
  assertThat(service.inspect(new ReminderService.Options("future","all",50,1)).rows()).hasSize(1);
 }
 @Test void editsKeepStateAndExplicitReopenRestoresPending(){
  String id=save(1,"2026-11-17T16:00","first");service.changeState(id,"done",view("all").revision());assertThat(view("done").rows()).hasSize(1);
  var edit=service.editor(id,0);service.save(id,edit.nodeId(),edit.contentId(),"2026-11-18T16:00",edit.zone(),"changed",edit.revision());
  assertThat(view("done").rows().get(0).message()).isEqualTo("changed");
  service.changeState(id,"pending",view("all").revision());assertThat(view("upcoming").rows()).hasSize(1);
  service.changeState(id,"cancelled",view("all").revision());assertThat(view("cancelled").rows()).hasSize(1);
 }
 @Test void staleFormsDuplicateSubmissionsAndRetargetedAliasesCannotOverwrite()throws Exception{
  var e=service.editor("",11);String id=service.save("",11,1,e.localTime(),e.zone(),"one",e.revision());
  assertThatThrownBy(()->service.save("",11,1,e.localTime(),e.zone(),"duplicate",e.revision())).hasMessageContaining("changed");assertThat(view("all").rows()).hasSize(1);
  var stale=service.editor(id,0);save(2,"2026-11-18T10:00","other");
  assertThatThrownBy(()->service.save(id,11,1,stale.localTime(),stale.zone(),"overwrite",stale.revision())).hasMessageContaining("changed");
  var newForm=service.editor("",11);try(var q=c.createStatement()){q.execute("UPDATE children SET master_id=2 WHERE node_id=11");}
  assertThatThrownBy(()->service.save("",11,1,newForm.localTime(),newForm.zone(),"wrong target",newForm.revision())).hasMessageContaining("targetMissing");
 }
 @Test void deletedOrTrashedLocationsFallBackToSharedThenRemainAsUnavailableRecords()throws Exception{
  String id=save(11,"2026-11-17T16:00","keep record");
  try(var q=c.createStatement()){
   q.execute("INSERT INTO node(node_id,name,tags,syntax) VALUES(90,'Trash','"+com.turkerozturk.node.TrashSql.TAG+"','plain-text')");q.execute("INSERT INTO children(node_id,father_id,sequence,master_id) VALUES(90,0,4,0)");q.execute("UPDATE children SET father_id=90 WHERE node_id=11");
  }
  assertThat(view("pending").rows().get(0).nodeId()).isEqualTo(1);
  try(var q=c.createStatement()){q.execute("DELETE FROM children WHERE node_id IN(1,11)");q.execute("DELETE FROM node WHERE node_id=1");}
  assertThat(view("pending").rows().get(0).available()).isFalse();assertThat(view("pending").rows().get(0).name()).isEqualTo("<Alpha>");
  service.changeState(id,"cancelled",view("all").revision());assertThatThrownBy(()->service.changeState(id,"pending",view("all").revision())).hasMessageContaining("targetMissing");
 }
 @Test void corruptDuplicateOrMovedStoresFailClosedWithoutReplacingData()throws Exception{
  String id=save(1,"2026-11-17T16:00","original");var edit=service.editor(id,0);long store=ReminderStore.read(c,false).id();
  try(var q=c.createStatement()){q.execute("UPDATE node SET txt='corrupt' WHERE node_id="+store);}
  assertThatThrownBy(()->view("all")).hasMessageContaining("storeInvalid");
  assertThatThrownBy(()->service.save(id,1,1,edit.localTime(),edit.zone(),"bad",edit.revision())).hasMessageContaining("storeInvalid");assertThat(text("SELECT txt FROM node WHERE node_id="+store)).isEqualTo("corrupt");
 }
 @Test void duplicateAndRelocatedHelpersAreRejected()throws Exception{
  save(1,"2026-11-17T16:00","first");long store=ReminderStore.read(c,false).id();
  try(var q=c.createStatement()){q.execute("UPDATE children SET father_id=2 WHERE node_id="+store);}
  assertThatThrownBy(()->view("all")).hasMessageContaining("storeInvalid");
  try(var q=c.createStatement()){
   q.execute("UPDATE children SET father_id=0 WHERE node_id="+store);
   q.execute("INSERT INTO node(node_id,name,txt,tags,syntax,is_ro,level) SELECT 90,name,txt,tags,syntax,is_ro,level FROM node WHERE node_id="+store);
   q.execute("INSERT INTO children(node_id,father_id,sequence,master_id) VALUES(90,0,8,0)");
  }
  assertThatThrownBy(()->view("all")).hasMessageContaining("storeInvalid");
 }
 @Test void ordinaryTagMoveDeleteAndChildCreationPathsProtectHelper()throws Exception{
  save(1,"2026-11-17T16:00","protected");long root=ReminderStore.read(c,false).id();
  assertThatThrownBy(()->com.turkerozturk.node.tags.NodeTags.normalize(ReminderStore.TAG)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->com.turkerozturk.node.audit.AuditSql.guardHelper(c,root)).hasMessageContaining("reminder.protected");
  assertThatThrownBy(()->com.turkerozturk.node.TrashSql.guardDelete(c,root)).hasMessageContaining("reminder.protected");
  assertThatThrownBy(()->com.turkerozturk.node.audit.AuditSql.guardMove(c,List.of(),root)).hasMessageContaining("reminder.protected");
  var quick=new com.turkerozturk.node.tags.QuickTagService(properties);ReflectionTestUtils.setField(quick,"entityManager",manager);var snapshot=quick.open(root);
  assertThat(snapshot.catalogue()).doesNotContain(ReminderStore.TAG);assertThatThrownBy(()->quick.save(root,"ordinary",snapshot.revision())).hasMessageContaining("reminder.protected");
 }
 @Test void readonlyAndInvalidInputsCannotCreateOrChangeRecords()throws Exception{
  var e=service.editor("",1);when(properties.writable()).thenReturn(false);assertThat(view("all").rows()).isEmpty();
  assertThatThrownBy(()->service.save("",1,1,e.localTime(),e.zone(),"x",e.revision())).hasMessageContaining("readonly");when(properties.writable()).thenReturn(true);
  assertThatThrownBy(()->service.save("",1,1,"2026-02-30T12:00","UTC","x",e.revision())).hasMessageContaining("invalidTime");
  assertThatThrownBy(()->service.save("",1,1,e.localTime(),e.zone(),"x".repeat(2001),e.revision())).hasMessageContaining("invalidInput");
  assertThat(ReminderStore.read(c,false).id()).isZero();
 }
}

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
 ReminderService at(String instant){var value=new ReminderService(properties,new CalendarSettings("MONDAY","Europe/Istanbul",1000),Clock.fixed(Instant.parse(instant),ZoneOffset.UTC));ReflectionTestUtils.setField(value,"manager",manager);return value;}
 ReminderService.View all(ReminderService value){return value.inspect(new ReminderService.Options("","all",50,1));}
 String annual(ReminderService value,String policy){var form=value.editor("",1);return value.save("",1,1,"2024-02-29T10:00","UTC","Birthday","yearly",policy,form.revision());}
 @Test void annualCompletionAdvancesAndPreservesFebruary29AcrossNonLeapYears()throws Exception{
  var value=at("2024-02-29T10:00:00Z");String id=annual(value,"feb28");
  for(String expected:List.of("2025-02-28T10:00:00Z","2026-02-28T10:00:00Z","2027-02-28T10:00:00Z","2028-02-29T10:00:00Z")){
   var current=all(value);value.changeState(id,"done",current.revision(),current.rows().get(0).alarmId());
   assertThat(all(value).rows().get(0).due()).isEqualTo(expected);assertThat(all(value).rows().get(0).status()).isEqualTo("upcoming");
  }
  assertThat(ReminderStore.read(c,false).data().items().get(0).anchor()).isEqualTo("2024-02-29T10:00");
  assertThat(text("SELECT ts_creation||':'||ts_lastsave FROM node WHERE node_id=1")).isEqualTo("100:200");
 }
 @Test void yearlyNoteAndZoneEditsKeepLeapAnchorAndMarchPolicyWorks()throws Exception{
  var value=at("2026-11-17T10:00:00Z");String id=annual(value,"feb28");var edit=value.editor(id,0);
  value.save(id,edit.nodeId(),edit.contentId(),edit.localTime(),"Europe/Istanbul","Updated","yearly","feb28",edit.revision());
  assertThat(ReminderStore.read(c,false).data().items().get(0).anchor()).isEqualTo("2024-02-29T10:00");
  var current=all(value);value.changeState(id,"done",current.revision(),current.rows().get(0).alarmId());
  assertThat(all(value).rows().get(0).dueText()).contains("2027-02-28 10:00");
  var next=at("2028-02-29T10:00:00Z");assertThat(all(next).rows().get(0).dueText()).contains("2028-02-29");
 }
 @Test void newAnniversaryRearmsDismissedPriorYearWithoutWritingOnReads()throws Exception{
  var first=at("2024-02-29T10:00:00Z");String id=annual(first,"mar1");var original=first.alarms().items().get(0);String occurrence=all(first).rows().get(0).alarmId();first.alarmAction(id,original.key(),"dismiss");
  assertThat(first.alarms().items()).isEmpty();String raw=ReminderStore.read(c,false).raw();
  var next=at("2025-03-01T10:00:00Z");var alarm=next.alarms().items().get(0);assertThat(alarm.key()).isNotEqualTo(original.key());assertThat(next.alarms().items().get(0).key()).isEqualTo(alarm.key());
  assertThat(ReminderStore.read(c,false).raw()).isEqualTo(raw);
  assertThatThrownBy(()->next.changeState(id,"done",all(next).revision(),occurrence)).hasMessageContaining("changed");
  next.alarmAction(id,alarm.key(),"dismiss");assertThat(next.alarms().items()).isEmpty();
 }
 @Test void snoozeAndDismissPersistAndOldAlarmButtonsCannotChangeNewSchedule(){
  String id=save(1,"2026-11-17T12:00","Alarm");var original=service.alarms().items().get(0);service.alarmAction(id,original.key(),"snooze");assertThat(service.alarms().items()).isEmpty();
  assertThat(at("2026-11-17T10:09:59Z").alarms().items()).isEmpty();var later=at("2026-11-17T10:10:00Z");var again=later.alarms().items().get(0);assertThat(again.key()).isNotEqualTo(original.key());
  assertThatThrownBy(()->later.alarmAction(id,original.key(),"snooze")).hasMessageContaining("changed");later.alarmAction(id,again.key(),"dismiss");assertThat(at("2026-11-18T10:00:00Z").alarms().items()).isEmpty();
  var view=all(later);assertThat(view.rows().get(0).alarmMuted()).isTrue();later.changeState(id,"pending",view.revision(),view.rows().get(0).alarmId());var rearmed=later.alarms().items().get(0);assertThat(rearmed.key()).isNotEqualTo(original.key());
  var edit=later.editor(id,0);later.save(id,1,1,"2026-11-19T16:00",edit.zone(),"Rescheduled","none","feb28",edit.revision());assertThatThrownBy(()->later.alarmAction(id,rearmed.key(),"dismiss")).hasMessageContaining("changed");
 }
 @Test void oldVersionLoadsWithoutWritesAndMigratesOnlyOnMutation()throws Exception{
  String id="11111111-1111-4111-8111-111111111111";
  var item=Map.of("id",id,"contentId",1L,"occurrence",11L,"targetName","Alpha","message","Old","due","2026-11-17T09:00:00Z","zone","UTC","status","pending","created",NOW.toString(),"updated",NOW.toString());
  String raw=ReminderStore.OLD_PREFIX+ReminderStore.JSON.writeValueAsString(Map.of("version",1,"kind","reminders","storeId",id,"items",List.of(item)));
  try(var q=c.prepareStatement("INSERT INTO node(node_id,name,txt,tags,syntax,is_ro,level) VALUES(90,'REMINDERS',?,?,'plain-text',1,3)")){q.setString(1,raw);q.setString(2,ReminderStore.TAG);q.executeUpdate();}
  try(var q=c.createStatement()){q.execute("INSERT INTO children(node_id,father_id,sequence,master_id) VALUES(90,0,4,0)");}
  assertThat(all(service).rows().get(0).repeat()).isEqualTo("none");String key=service.alarms().items().get(0).key();assertThat(service.alarms().items().get(0).key()).isEqualTo(key);assertThat(text("SELECT txt FROM node WHERE node_id=90")).isEqualTo(raw);
  var edit=service.editor(id,0);service.save(id,11,1,edit.localTime(),edit.zone(),"Updated",edit.revision());assertThat(text("SELECT txt FROM node WHERE node_id=90")).startsWith(ReminderStore.PREFIX);assertThat(ReminderStore.read(c,false).data().version()).isEqualTo(2);
 }
 @Test void alarmFeedIsBoundedIgnoresMissingTargetsAndReadonlyWritesAreRejected()throws Exception{
  for(int i=0;i<25;i++)save(1,String.format(Locale.ROOT,"2026-11-16T00:%02d",i),"Due "+i);
  assertThat(service.alarms().total()).isEqualTo(25);assertThat(service.alarms().items()).hasSize(20);assertThat(service.alarms().items().get(0).message()).isEqualTo("Due 24");
  var alarm=service.alarms().items().get(0);when(properties.writable()).thenReturn(false);assertThatThrownBy(()->service.alarmAction(alarm.id(),alarm.key(),"dismiss")).hasMessageContaining("readonly");when(properties.writable()).thenReturn(true);
  try(var q=c.createStatement()){q.execute("DELETE FROM children WHERE node_id IN(1,11)");}assertThat(service.alarms().items()).isEmpty();
 }

 @Test void anOldAnnualEditFormCannotResetTheAnchorAtTheNextAnniversary()throws Exception{
  var first=at("2024-02-29T10:00:00Z");String id=annual(first,"mar1");var old=first.editor(id,0);String raw=ReminderStore.read(c,false).raw();
  var next=at("2025-03-01T10:00:00Z");
  assertThatThrownBy(()->next.save(id,old.nodeId(),old.contentId(),old.localTime(),old.zone(),"Note edit",old.repeat(),old.leapDay(),old.revision(),old.alarmId())).hasMessageContaining("changed");
  assertThat(ReminderStore.read(c,false).raw()).isEqualTo(raw);
 }

}

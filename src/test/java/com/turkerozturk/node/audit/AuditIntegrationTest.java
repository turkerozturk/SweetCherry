package com.turkerozturk.node.audit;
import com.turkerozturk.multipledatabases.*;
import com.turkerozturk.node.contentediting.NodeContentEditingService;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"myapp.openWebBrowserOnStartup=false","myapp.desktop.enabled=false","myapp.command-line-runner.enabled=false","myapp.audit.enabled=true","myapp.trash.enabled=true","myapp.audit.max-event-characters=1000000"})
class AuditIntegrationTest {
 @Autowired NodeContentEditingService content;@Autowired AuditReadService history;
 @Autowired com.turkerozturk.node.properties.NodePropertiesService properties;
 @Autowired com.turkerozturk.node.tags.QuickTagService tags;
 @Autowired com.turkerozturk.node.moving.NodeMoveService moves;
 @Autowired com.turkerozturk.node.moving.BatchNodeService batch;
 @Autowired com.turkerozturk.bookmark.BookmarkWriteService bookmarks;
 @Autowired com.turkerozturk.node.tags.TagManagementService globalTags;
 @Autowired com.turkerozturk.richtext.editing.RichTextEditingService rich;
 @Autowired com.turkerozturk.node.TrashService trash;
 @Autowired javax.sql.DataSource dataSource;@Autowired CustomPropertiesHolder custom;
 @org.junit.jupiter.api.io.TempDir Path directory;ManagedTenantDataSource source;MultitenantDataSource routing;Map<Object,Object> original;
 @BeforeEach void setup()throws Exception{
  var file=directory.resolve("audit.ctb");try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var in=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")){
   for(String line:new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\\R"))if(line.startsWith("CREATE TABLE")||line.startsWith("INSERT INTO"))q.execute(line);q.execute("UPDATE node SET ts_lastsave=100,tags='old'");
  }
  source=new ManagedTenantDataSource(()->{var p=new com.zaxxer.hikari.HikariDataSource();p.setJdbcUrl("jdbc:sqlite:"+file);return p;});source.activate();routing=(MultitenantDataSource)dataSource;original=new HashMap<>(routing.getResolvedDataSources());var next=new HashMap<Object,Object>(original);next.put("Audit test",source);routing.setTargetDataSources(next);routing.afterPropertiesSet();custom.addCustomProperties("Audit test",Map.of("custom.isWritable","true"));TenantContext.setCurrentTenant("Audit test");
  SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("audit-admin","",List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
 }
 @AfterEach void close(){SecurityContextHolder.clearContext();TenantContext.clear();custom.removeCustomProperties("Audit test");if(original!=null){routing.setTargetDataSources(original);routing.afterPropertiesSet();}if(source!=null)source.close();}
 void sql(String text)throws Exception{try(var c=source.getConnection();var q=c.createStatement()){q.execute(text);}}
 String text(String sql)throws Exception{try(var c=source.getConnection();var q=c.createStatement();var rs=q.executeQuery(sql)){rs.next();return rs.getString(1);}}
 List<AuditSql.Index> events(){return history.history(LocalDate.now(ZoneOffset.UTC).toString(),0,0).events();}
 AuditSql.Event event(){return history.event(events().get(0).id());}
 @Test void advisedContentSaveRetainsExactBeforeAfterTextAndActorAndDoesNotAuditItself()throws Exception{
  String before=text("SELECT txt FROM node WHERE node_id=2");content.update(2,"New 😀 text");assertThat(events()).hasSize(1);var event=event();assertThat(event.action()).isEqualTo("content.edit");assertThat(event.actor()).isEqualTo("audit-admin");assertThat(history.source(event,2,"before",true)).isEqualTo(before);assertThat(history.source(event,2,"after",false)).isEqualTo("New 😀 text");assertThat(history.history(event.time().substring(0,10),2,0).total()).isEqualTo(1);assertThat(history.history(event.time().substring(0,10),11,0).total()).isEqualTo(1);assertThat(text("SELECT txt FROM node WHERE node_id=2")).isEqualTo("New 😀 text");
  content.update(2,"New 😀 text");assertThat(events()).hasSize(1);
 }
 @Test void identicalPropertySaveSkipsTimestampOnlyNoiseButChangesAreLogged(){
  var node=properties.realNode(2);properties.update(2,node.getName(),false,null,com.turkerozturk.helpers.NodeIcon.OTHER,false);assertThat(events()).isEmpty();
  properties.update(2,"Renamed",true,null,com.turkerozturk.helpers.NodeIcon.CT_HOME,false);assertThat(events()).hasSize(1);assertThat(event().nodes().get(0).before().get("name")).isEqualTo("Node 2");assertThat(event().sources()).isEmpty();
 }
 @Test void sharedQuickTagsBatchTagsAndGlobalTagsHaveOneEventPerOperation(){
  var selected=tags.open(11);tags.save(11,"old C++",selected.revision());assertThat(events()).hasSize(1);assertThat(event().subjects()).containsExactly(11L);assertThat(event().nodes()).anyMatch(c->c.id()==2&&"old".equals(c.before().get("tags")));assertThat(history.history(event().time().substring(0,10),11,0).total()).isEqualTo(1);
  var p=batch.preview(List.of(1L,2L));batch.apply(List.of(1L,2L),p.revision(),"tag","","batch");assertThat(events()).hasSize(2);
  var operation=new com.turkerozturk.node.tags.TagManagementService.Operation(com.turkerozturk.node.tags.TagManagementService.Action.RENAME,"batch","renamed");var preview=globalTags.preview(operation);globalTags.apply(operation,preview.revision());assertThat(events()).hasSize(3);assertThat(events().stream().map(AuditSql.Index::action)).contains("tags.update","batch.tag","tags.manage");
 }
 @Test void moveAndBookmarkChangesCaptureActualPlacementAndOrdering(){
  moves.moveTo(2,4,moves.state(2).revision());assertThat(event().tree()).anyMatch(c->c.id()==2&&"0".equals(c.before().get("father_id"))&&"4".equals(c.after().get("father_id")));int total=events().size();moves.moveTo(2,4,moves.state(2).revision());assertThat(events()).hasSize(total);
  bookmarks.add(2);bookmarks.add(2);bookmarks.reorder(2,5);bookmarks.remove(2);assertThat(events()).hasSize(total+3);assertThat(events().stream().map(AuditSql.Index::action)).contains("bookmark.add","bookmark.reorder","bookmark.remove");
 }
 @Test void richTextObjectDeletionRecordsReadableTextOriginalXmlAndBinaryManifest()throws Exception{
  sql("INSERT INTO image VALUES(2,0,'left','',X'0102FF','removed.bin','',0)");var open=rich.open(2);
  rich.save(2,"<node><rich_text>Replacement</rich_text></node>",open.revision(),"{}","{}","{}","[\"image:0\"]",true);
  assertThat(events()).hasSize(1);var event=event();assertThat(event.requestedObjectDeletions()).containsExactly("image:0");var before=event.sources().stream().filter(s->s.stage().equals("before")).findFirst().orElseThrow();assertThat(before.objects()).anyMatch(o->"removed.bin".equals(o.get("filename"))&&"3".equals(o.get("png.bytes"))&&o.get("png.sha256").length()==64);assertThat(event.sources().stream().filter(s->s.stage().equals("after")).findFirst().orElseThrow().objects()).isEmpty();assertThat(history.source(event,2,"after",false)).isEqualTo("Replacement");
 }
 @Test void failedAuditInsertionRollsBackTheUserEditAndEveryHelper()throws Exception{
  String old=text("SELECT txt FROM node WHERE node_id=2");sql("CREATE TRIGGER refuse_audit BEFORE INSERT ON node WHEN NEW.tags='"+AuditSql.TAG+"' BEGIN SELECT RAISE(ABORT,'audit test failure'); END");assertThatThrownBy(()->content.update(2,"Lost update must not commit")).isInstanceOf(RuntimeException.class);assertThat(text("SELECT txt FROM node WHERE node_id=2")).isEqualTo(old);assertThat(events()).isEmpty();
 }
 @Test void oversizedAfterSnapshotRollsBackTheEditRatherThanTruncatingItsHistory()throws Exception{
  String old=text("SELECT txt FROM node WHERE node_id=2");assertThatThrownBy(()->content.update(2,"x".repeat(1100000))).hasMessageContaining("audit.limit");assertThat(text("SELECT txt FROM node WHERE node_id=2")).isEqualTo(old);assertThat(events()).isEmpty();
 }
 @Test void managedLogNotesCannotBeEditedMovedDeletedOrRetagged()throws Exception{
  content.update(2,"new");long root=history.history(LocalDate.now(ZoneOffset.UTC).toString(),0,0).rootId();assertThat(root).isPositive();assertThatThrownBy(()->properties.update(root,"rename",false,null,com.turkerozturk.helpers.NodeIcon.OTHER,false)).hasMessageContaining("audit.protected");assertThatThrownBy(()->moves.moveTo(root,1,moves.state(root).revision())).hasMessageContaining("audit.protected");var p=tags.open(root);assertThatThrownBy(()->tags.save(root,"new",p.revision())).hasMessageContaining("audit.protected");assertThat(events()).hasSize(1);
 }
 @Test void reversibleDeleteCannotMoveTheAuditRootIntoTrash(){
  content.update(2,"new");long root=history.history(LocalDate.now(ZoneOffset.UTC).toString(),0,0).rootId();assertThatThrownBy(()->trash.preview(root)).hasMessageContaining("audit.protected");assertThatThrownBy(()->trash.trash(root,"invalid")).hasMessageContaining("audit.protected");assertThat(events()).hasSize(1);
 }
 @Test void unmanagedRootNameCollisionRollsBackInsteadOfOverwritingANote()throws Exception{
  sql("UPDATE node SET name='CTBLOGS' WHERE node_id=4");String old=text("SELECT txt FROM node WHERE node_id=2");assertThatThrownBy(()->content.update(2,"new")).hasMessageContaining("audit.invalid");assertThat(text("SELECT txt FROM node WHERE node_id=2")).isEqualTo(old);assertThat(events()).isEmpty();
 }

}

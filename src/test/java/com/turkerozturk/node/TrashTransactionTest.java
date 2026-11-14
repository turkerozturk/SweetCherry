package com.turkerozturk.node;
import com.turkerozturk.multipledatabases.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"myapp.openWebBrowserOnStartup=false","myapp.desktop.enabled=false","myapp.command-line-runner.enabled=false","myapp.trash.enabled=true"})
class TrashTransactionTest {
 @Autowired TrashService service;@Autowired com.turkerozturk.node.properties.NodePropertiesService properties;
 @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager manager;@Autowired org.springframework.transaction.PlatformTransactionManager transactions;@Autowired NodeDeletionService deletion;@Autowired javax.sql.DataSource dataSource;@Autowired CustomPropertiesHolder custom;
 @org.junit.jupiter.api.io.TempDir Path directory;ManagedTenantDataSource source;MultitenantDataSource routing;Map<Object,Object> original;
 @BeforeEach void setup()throws Exception{
  var file=directory.resolve("trash.ctb");try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var in=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")){
   for(String line:new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\\R"))if(line.startsWith("CREATE TABLE")||line.startsWith("INSERT INTO"))q.execute(line);
   q.execute("UPDATE node SET ts_lastsave=100");q.execute("INSERT INTO image VALUES(2,0,'left','',X'0102FF','binary.bin','',0)");
  }
  source=new ManagedTenantDataSource(()->{var p=new com.zaxxer.hikari.HikariDataSource();p.setJdbcUrl("jdbc:sqlite:"+file);return p;});source.activate();routing=(MultitenantDataSource)dataSource;original=new HashMap<>(routing.getResolvedDataSources());var next=new HashMap<Object,Object>(original);next.put("Trash test",source);routing.setTargetDataSources(next);routing.afterPropertiesSet();custom.addCustomProperties("Trash test",Map.of("custom.isWritable","true"));TenantContext.setCurrentTenant("Trash test");
 }
 @AfterEach void close(){TenantContext.clear();custom.removeCustomProperties("Trash test");if(original!=null){routing.setTargetDataSources(original);routing.afterPropertiesSet();}if(source!=null)source.close();}
 void sql(String text)throws Exception{try(var c=source.getConnection();var q=c.createStatement()){q.execute(text);}}
 long number(String text)throws Exception{try(var c=source.getConnection();var q=c.createStatement();var rs=q.executeQuery(text)){rs.next();return rs.getLong(1);}}
 TrashSql.Entry trash(){var preview=service.preview(2);service.trash(2,preview.revision());return service.listing().entries().get(0);}
 @Test void failedMoveRollsBackNewRootEntryAndTimestamps()throws Exception{
  long count=number("SELECT COUNT(*) FROM node");var p=service.preview(2);
  sql("CREATE TRIGGER refuse_move BEFORE UPDATE OF father_id ON children WHEN OLD.node_id=2 BEGIN SELECT RAISE(ABORT,'test failure'); END");
  assertThatThrownBy(()->service.trash(2,p.revision())).isInstanceOf(RuntimeException.class);assertThat(number("SELECT COUNT(*) FROM node")).isEqualTo(count);assertThat(service.listing().entries()).isEmpty();assertThat(number("SELECT father_id FROM children WHERE node_id=2")).isZero();assertThat(number("SELECT ts_lastsave FROM node WHERE node_id=2")).isEqualTo(100);
 }
 @Test void failedRestoreRollsBackThePayloadMoveAndHelperDeletion()throws Exception{
  var e=trash();sql("CREATE TRIGGER refuse_entry_delete BEFORE DELETE ON node WHEN OLD.node_id="+e.id()+" BEGIN SELECT RAISE(ABORT,'test failure'); END");
  assertThatThrownBy(()->service.restore(e.id(),e.revision(),false)).isInstanceOf(RuntimeException.class);assertThat(number("SELECT father_id FROM children WHERE node_id=2")).isEqualTo(e.id());assertThat(service.entry(e.id()).revision()).isEqualTo(e.revision());
 }
 @Test void failedPurgeRollsBackSharedPromotionAndEveryDeletion()throws Exception{
  var e=trash();var revision=service.purgePreview(e.id()).revision();sql("CREATE TRIGGER refuse_entry_delete BEFORE DELETE ON node WHEN OLD.node_id="+e.id()+" BEGIN SELECT RAISE(ABORT,'test failure'); END");
  assertThatThrownBy(()->service.purge(e.id(),revision,true)).isInstanceOf(RuntimeException.class);assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=2")).isEqualTo(1);assertThat(number("SELECT master_id FROM children WHERE node_id=11")).isEqualTo(2);assertThat(service.entry(e.id()).revision()).isEqualTo(e.revision());
 }
 @Test void committedRoundTripPreservesBinaryBytesAndGuardsExistingPermanentDelete()throws Exception{
  var e=trash();assertThatThrownBy(()->deletion.deleteNodeWithSubNodes(2)).hasMessageContaining("trash.usePage");assertThatThrownBy(()->service.purge(e.id(),e.revision(),false)).hasMessageContaining("trash.confirmRequired");
  service.restore(e.id(),e.revision(),false);
  try(var c=source.getConnection();var q=c.createStatement();var rs=q.executeQuery("SELECT png FROM image WHERE node_id=2")){assertThat(rs.next()).isTrue();assertThat(rs.getBytes(1)).containsExactly(1,2,-1);}
 }
 @Test void readonlyDatabaseCannotPreviewRestoreOrPurge(){
  custom.addCustomProperties("Trash test",Map.of("custom.isWritable","false"));assertThatThrownBy(()->service.preview(2)).hasMessageContaining("trash.readonly");assertThatThrownBy(()->service.trash(2,"revision")).hasMessageContaining("trash.readonly");assertThatThrownBy(()->service.restore(1,"revision",true)).hasMessageContaining("trash.readonly");assertThatThrownBy(()->service.purge(1,"revision",true)).hasMessageContaining("trash.readonly");
 }
 @Test void disablingNewTrashOperationsDoesNotPreventRestoringExistingEntries(){
  var e=trash();var disabled=new TrashService(properties,false);org.springframework.test.util.ReflectionTestUtils.setField(disabled,"entityManager",manager);
  assertThatThrownBy(()->disabled.trash(1,"revision")).hasMessageContaining("trash.disabled");
  Long restored=new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->disabled.restore(e.id(),e.revision(),false));assertThat(restored).isEqualTo(2);assertThat(service.listing().entries()).isEmpty();
 }
 @Test void managedHelpersCannotBeUnlockedOrRetaggedThroughNormalEditors(){
  var e=trash();long root=service.listing().rootId();assertThatThrownBy(()->properties.update(root,"Renamed",false,null,com.turkerozturk.helpers.NodeIcon.OTHER,false)).hasMessageContaining("trash.usePage");
  assertThat(service.listing().rootId()).isEqualTo(root);assertThat(service.entry(e.id()).revision()).isEqualTo(e.revision());
 }

}

package com.turkerozturk.node.moving;
import com.turkerozturk.multipledatabases.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"myapp.openWebBrowserOnStartup=false","myapp.desktop.enabled=false","myapp.command-line-runner.enabled=false"})
class BatchNodeTransactionTest {
 @Autowired BatchNodeService service;@Autowired javax.sql.DataSource dataSource;@Autowired CustomPropertiesHolder custom;
 @org.junit.jupiter.api.io.TempDir Path directory;ManagedTenantDataSource source;MultitenantDataSource routing;Map<Object,Object> original;
 @BeforeEach void setup()throws Exception{
  var file=directory.resolve("batch.ctb");try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var in=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")){
   for(String line:new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\\R"))if(line.startsWith("CREATE TABLE")||line.startsWith("INSERT INTO"))q.execute(line);
   q.execute("UPDATE node SET tags='old',ts_lastsave=100");
  }
  source=new ManagedTenantDataSource(()->{var p=new com.zaxxer.hikari.HikariDataSource();p.setJdbcUrl("jdbc:sqlite:"+file);return p;});source.activate();routing=(MultitenantDataSource)dataSource;original=new HashMap<>(routing.getResolvedDataSources());var next=new HashMap<Object,Object>(original);next.put("Batch test",source);routing.setTargetDataSources(next);routing.afterPropertiesSet();custom.addCustomProperties("Batch test",Map.of("custom.isWritable","true"));TenantContext.setCurrentTenant("Batch test");
 }
 @AfterEach void close(){TenantContext.clear();custom.removeCustomProperties("Batch test");if(original!=null){routing.setTargetDataSources(original);routing.afterPropertiesSet();}if(source!=null)source.close();}
 void sql(String sql)throws Exception{try(var c=source.getConnection();var q=c.createStatement()){q.execute(sql);}}
 @Test void tagFailureRollsBackEveryNodeAndModificationDate()throws Exception{
  var ids=List.of(1L,2L);var revision=service.preview(ids).revision();sql("CREATE TRIGGER refuse_second_tag BEFORE UPDATE OF tags ON node WHEN OLD.node_id=2 BEGIN SELECT RAISE(ABORT,'test failure'); END");
  assertThatThrownBy(()->service.apply(ids,revision,"tag","","new")).isInstanceOf(RuntimeException.class);
  try(var c=source.getConnection();var q=c.createStatement();var rs=q.executeQuery("SELECT tags,ts_lastsave FROM node WHERE node_id IN (1,2)")){while(rs.next()){assertThat(rs.getString(1)).isEqualTo("old");assertThat(rs.getLong(2)).isEqualTo(100);}}
 }
 @Test void moveFailureRollsBackTheEntireHierarchy()throws Exception{
  var ids=List.of(1L,2L);var revision=service.preview(ids).revision();sql("CREATE TRIGGER refuse_second_move BEFORE UPDATE OF father_id ON children WHEN OLD.node_id=2 BEGIN SELECT RAISE(ABORT,'test failure'); END");
  assertThatThrownBy(()->service.apply(ids,revision,"move","4","")).isInstanceOf(RuntimeException.class);
  assertThat(service.preview(ids).revision()).isEqualTo(revision);
 }
}

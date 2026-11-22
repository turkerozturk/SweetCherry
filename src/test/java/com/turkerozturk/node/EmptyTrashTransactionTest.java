package com.turkerozturk.node;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Executes the real JDBC callbacks inside the service's Spring transaction boundaries. */
class EmptyTrashTransactionTest {
 @TempDir Path directory;
 DriverManagerDataSource source;TrashService service;
 com.turkerozturk.node.properties.NodePropertiesService properties;
 @BeforeEach void setup()throws Exception{
  source=new DriverManagerDataSource("jdbc:sqlite:"+directory.resolve("trash.ctb"));
  try(var c=source.getConnection();var q=c.createStatement();var in=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")){
   for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\\R"))if(line.startsWith("CREATE TABLE")||line.startsWith("INSERT INTO"))q.execute(line);
  }
  properties=mock(com.turkerozturk.node.properties.NodePropertiesService.class);when(properties.writable()).thenReturn(true);
  var target=new TrashService(properties,true);var manager=mock(jakarta.persistence.EntityManager.class);var session=mock(Session.class);
  when(manager.unwrap(Session.class)).thenReturn(session);ReflectionTestUtils.setField(target,"entityManager",manager);
  doAnswer(call->{var c=DataSourceUtils.getConnection(source);try{call.<Work>getArgument(0).execute(c);return null;}catch(SQLException error){throw new IllegalStateException(error);}finally{DataSourceUtils.releaseConnection(c,source);}}).when(session).doWork(any());
  doAnswer(call->{var c=DataSourceUtils.getConnection(source);try{return call.<ReturningWork<?>>getArgument(0).execute(c);}catch(SQLException error){throw new IllegalStateException(error);}finally{DataSourceUtils.releaseConnection(c,source);}}).when(session).doReturningWork(any());
  var proxy=new ProxyFactory(target);proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source),new AnnotationTransactionAttributeSource()));
  service=(TrashService)proxy.getProxy();
 }
 void trash(long id){service.trash(id,service.preview(id).revision());}
 long number(String text)throws Exception{try(var c=source.getConnection();var q=c.createStatement();var rs=q.executeQuery(text)){rs.next();return rs.getLong(1);}}
 @Test void failureInLaterEntryRollsBackEarlierDeletionSharedPromotionAndTimestamps()throws Exception{
  trash(1);trash(2);var listing=service.listing();long second=listing.entries().stream().filter(e->e.nodeId()==2).findFirst().orElseThrow().id();
  try(var c=source.getConnection();var q=c.createStatement()){q.execute("CREATE TRIGGER refuse_last BEFORE DELETE ON node WHEN OLD.node_id="+second+" BEGIN SELECT RAISE(ABORT,'test failure'); END");}
  var preview=service.emptyPreview();long timestamp=number("SELECT ts_lastsave FROM node WHERE node_id=1");
  assertThatThrownBy(()->service.empty(preview.revision(),true)).isInstanceOf(RuntimeException.class);
  assertThat(service.listing().entries()).hasSize(2);assertThat(number("SELECT COUNT(*) FROM node WHERE node_id IN (1,2,3)")).isEqualTo(3);
  assertThat(number("SELECT master_id FROM children WHERE node_id=11")).isEqualTo(2);
  assertThat(number("SELECT ts_lastsave FROM node WHERE node_id=1")).isEqualTo(timestamp);
 }
 @Test void explicitConfirmationAndWriteAccessAreRequiredAndSuccessfulEmptyCommits()throws Exception{
  trash(2);var preview=service.emptyPreview();
  assertThatThrownBy(()->service.empty(preview.revision(),false)).hasMessageContaining("trash.confirmRequired");
  when(properties.writable()).thenReturn(false);
  assertThatThrownBy(()->service.empty(preview.revision(),true)).hasMessageContaining("trash.readonly");
  when(properties.writable()).thenReturn(true);assertThat(service.listing().entries()).hasSize(1);
  service.empty(preview.revision(),true);assertThat(service.listing().entries()).isEmpty();
  assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=11")).isEqualTo(1);
 }
}

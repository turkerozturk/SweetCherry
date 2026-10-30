package com.turkerozturk.node.quick;

import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.node.creation.ChildNodeService;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.orm.jpa.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuickNodeServiceTest {
 @TempDir Path directory;
 private Connection fixture(String name) throws Exception {
  var connection=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve(name));
  try(var input=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql");var statement=connection.createStatement()) {
   for(String part:new String(input.readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "").split(";"))if(!part.isBlank())statement.execute(part);
  }
  return connection;
 }
 @Test void titleSearchPreservesSharedPositionsStylesAndBreadcrumbsAndHandlesTurkish() throws Exception {
  try(var c=fixture("search.ctb");var s=c.createStatement()) {
   s.execute("UPDATE node SET name='İSTANBUL Çalışma',is_richtxt=286331138 WHERE node_id=1");
   var found=QuickNodeService.search(c,"istanbul calisma");
   assertThat(found.items()).extracting(QuickNodeService.Target::id).containsExactly("1","10","18");
   var nested=found.items().get(2);assertThat(nested.shared()).isTrue();assertThat(nested.bold()).isTrue();
   assertThat(nested.breadcrumb()).contains("Node 2 [11]","İSTANBUL Çalışma [18]");
   assertThat(QuickNodeService.search(c,"Content of master").items()).isEmpty();
   assertThat(QuickNodeService.search(c,"   ").items()).isEmpty();
  }
 }
 @Test void titleOrderingAndLiteralSymbolsDoNotBecomeSqlWildcards() throws Exception {
  try(var c=fixture("order.ctb");var s=c.createStatement()) {
   s.execute("UPDATE node SET name='100%_! O''Brien <b>x</b>' WHERE node_id=3");
   assertThat(QuickNodeService.search(c,"%_!").items()).extracting(QuickNodeService.Target::id).containsExactly("3");
   assertThat(QuickNodeService.search(c,"Node 2").items().get(0).id()).isEqualTo("2");
   assertThat(QuickNodeService.search(c,"' OR 1=1 --").items()).isEmpty();
  }
 }
 @Test void longIdsStayStringsAndMoreResultsAreReported() throws Exception {
  try(var c=fixture("limit.ctb");var s=c.createStatement()) {
   for(int i=100;i<145;i++){s.execute("INSERT INTO children VALUES ("+i+",0,"+i+",1)");}
   s.execute("INSERT INTO children VALUES (9223372036854775807,0,999,1)");
   assertThat(QuickNodeService.search(c,"Node 1").more()).isTrue();
   s.execute("DELETE FROM children WHERE node_id BETWEEN 100 AND 144");
   assertThat(QuickNodeService.search(c,"Node 1").items()).extracting(QuickNodeService.Target::id).contains("9223372036854775807");
  }
 }
 @Test void invalidAndReadOnlyRequestsNeverInvokeCreation() {
  var properties=mock(NodePropertiesService.class);var creation=mock(ChildNodeService.class);
  var service=new QuickNodeService(creation,properties);
  when(properties.writable()).thenReturn(true);
  for(String syntax:new String[]{"custom-colors","java",""})assertThatThrownBy(()->service.create(0,"title","text",syntax)).isInstanceOf(RuntimeException.class);
  assertThatThrownBy(()->service.create(0," ","text","plain-text")).isInstanceOf(RuntimeException.class);
  assertThatThrownBy(()->service.create(0,"title","x".repeat(200001),"plain-text")).isInstanceOf(RuntimeException.class);
  assertThatThrownBy(()->service.create(-1,"title","text","plain-text")).isInstanceOf(RuntimeException.class);
  when(properties.writable()).thenReturn(false);
  assertThatThrownBy(()->service.create(0,"title","text","plain-text")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  verifyNoInteractions(creation);
 }
 private <T> T proxy(T target,JpaTransactionManager manager) {
  var proxy=new ProxyFactory(target);proxy.setProxyTargetClass(true);
  proxy.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
  return (T)proxy.getProxy();
 }
 @Test void realTransactionsPreserveHierarchyDatesRawTextAndRollbackPartialCreation() throws Exception {
  try(var c=fixture("write.ctb");var s=c.createStatement()) {
   s.execute("UPDATE node SET ts_creation=50,ts_lastsave=100");
   var config=new org.hibernate.cfg.Configuration().setProperty("hibernate.dialect","org.hibernate.community.dialect.SQLiteDialect")
    .setProperty("hibernate.connection.driver_class","org.sqlite.JDBC").setProperty("hibernate.connection.url","jdbc:sqlite:"+directory.resolve("write.ctb"))
    .setProperty("hibernate.hbm2ddl.auto","none");
   try(var factory=config.buildSessionFactory()) {
    EntityManager em=SharedEntityManagerCreator.createSharedEntityManager(factory);var tx=new JpaTransactionManager(factory);
    var properties=mock(NodePropertiesService.class);when(properties.writable()).thenReturn(true);
    var child=new ChildNodeService(properties,new CustomPropertiesHolder());ReflectionTestUtils.setField(child,"entityManager",em);
    var service=new QuickNodeService(proxy(child,tx),properties);ReflectionTestUtils.setField(service,"entityManager",em);service=proxy(service,tx);
    long id=service.create(11,"  New note  ","# Heading\n\nraw **text**\n","markdown");
    try(var row=s.executeQuery("SELECT c.father_id,c.master_id,n.name,n.txt,n.syntax,n.ts_creation,n.ts_lastsave FROM children c JOIN node n ON n.node_id=c.node_id WHERE c.node_id="+id)) {
     assertThat(row.next()).isTrue();assertThat(row.getLong(1)).isEqualTo(11);assertThat(row.getLong(2)).isZero();
     assertThat(row.getString(3)).isEqualTo("New note");assertThat(row.getString(4)).isEqualTo("# Heading\n\nraw **text**\n");
     assertThat(row.getString(5)).isEqualTo("markdown");assertThat(row.getLong(6)).isEqualTo(row.getLong(7)).isGreaterThan(100);
    }
    long root=service.create(0,"Root note","","plain-text");
    try(var row=s.executeQuery("SELECT father_id FROM children WHERE node_id="+root)){assertThat(row.next()).isTrue();assertThat(row.getLong(1)).isZero();}
    long count;try(var row=s.executeQuery("SELECT COUNT(*) FROM node")){row.next();count=row.getLong(1);}
    s.execute("CREATE TRIGGER reject_quick BEFORE UPDATE ON node WHEN NEW.txt='reject' BEGIN SELECT RAISE(ABORT,'test rejection'); END");
    QuickNodeService active=service;
    assertThatThrownBy(()->active.create(11,"Rejected","reject","plain-text")).isInstanceOf(RuntimeException.class);
    try(var row=s.executeQuery("SELECT COUNT(*) FROM node")){row.next();assertThat(row.getLong(1)).isEqualTo(count);}
    assertThatThrownBy(()->active.create(9999,"Missing parent","text","plain-text")).isInstanceOf(RuntimeException.class);
    try(var row=s.executeQuery("SELECT ts_creation,ts_lastsave FROM node WHERE node_id=2")){row.next();assertThat(row.getLong(1)).isEqualTo(50);assertThat(row.getLong(2)).isGreaterThan(100);}
   }
  }
 }
}

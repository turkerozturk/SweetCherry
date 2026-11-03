package com.turkerozturk.customlists.views;

import com.turkerozturk.node.*;
import com.turkerozturk.children.*;
import com.turkerozturk.node.creation.ChildNodeService;
import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.orm.jpa.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecordViewCreationTest {
    @TempDir Path directory;
    private <T> T proxy(T target,JpaTransactionManager manager){var p=new ProxyFactory(target);p.setProxyTargetClass(true);p.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));return (T)p.getProxy();}
    @Test void createsOrdinaryPlainNodesAndRejectsReplay()throws Exception{run(false);}
    @Test void laterFailureRollsBackFolderAndLayoutTogether()throws Exception{run(true);}
    void run(boolean reject)throws Exception {
        Path db=directory.resolve("layout.ctb");
        try(var connection=DriverManager.getConnection("jdbc:sqlite:"+db);var statement=connection.createStatement()){
            String sql=new String(getClass().getResourceAsStream("/fixtures/shared-node-tree.sql").readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$","");for(String part:sql.split(";"))if(!part.isBlank())statement.execute(part);
            statement.execute("UPDATE node SET txt='application'||char(9)||'App'||char(10)||'status'||char(9)||'Status',ts_creation=100,ts_lastsave=100 WHERE node_id=1");
            long before=count(connection,"SELECT count(*) FROM node");
            if(reject)statement.execute("CREATE TRIGGER reject_layout BEFORE UPDATE ON node WHEN NEW.name='Example layout' BEGIN SELECT RAISE(ABORT,'test failure'); END");
            var config=new org.hibernate.cfg.Configuration().setProperty("hibernate.dialect","org.hibernate.community.dialect.SQLiteDialect").setProperty("hibernate.connection.driver_class","org.sqlite.JDBC").setProperty("hibernate.connection.url","jdbc:sqlite:"+db).setProperty("hibernate.hbm2ddl.auto","none");
            try(var factory=config.buildSessionFactory()){
                var em=SharedEntityManagerCreator.createSharedEntityManager(factory);var tx=new JpaTransactionManager(factory);var properties=mock(NodePropertiesService.class);when(properties.writable()).thenReturn(true);
                var creation=new ChildNodeService(properties,new CustomPropertiesHolder());ReflectionTestUtils.setField(creation,"entityManager",em);
                var nodes=mock(NodeService.class);when(nodes.findDisplayNode(anyLong())).thenAnswer(call->{
                    try(var q=connection.prepareStatement("SELECT node_id,name,txt,syntax FROM node WHERE node_id=?")){q.setLong(1,call.getArgument(0));try(var row=q.executeQuery()){if(!row.next())return null;var node=RecordViewRendererTest.node(row.getLong(1),row.getString(2),row.getString(3));node.setSyntax(row.getString(4));return node;}}
                });
                var children=mock(ChildrenRepository.class);when(children.findByFatherIdOrderBySequenceAsc(anyLong())).thenAnswer(call->{var rows=new ArrayList<Children>();try(var q=connection.prepareStatement("SELECT node_id FROM children WHERE father_id=? ORDER BY sequence")){q.setLong(1,call.getArgument(0));try(var row=q.executeQuery()){while(row.next()){var c=new Children();c.setNodeId(row.getLong(1));rows.add(c);}}}return rows;});
                var service=new RecordViewService(nodes,children,mock(TemplateService.class),properties,proxy(creation,tx),mock(RecordViewRenderer.class),new RecordViewSettings());ReflectionTestUtils.setField(service,"manager",em);var active=proxy(service,tx);
                if(reject){assertThatThrownBy(()->active.createExample(1)).isInstanceOf(RuntimeException.class);assertThat(count(connection,"SELECT count(*) FROM node")).isEqualTo(before);assertThat(count(connection,"SELECT ts_lastsave FROM node WHERE node_id=1")).isEqualTo(100);}
                else {
                    long id=active.createExample(1);assertThat(id).isEqualTo(21);assertThat(count(connection,"SELECT father_id FROM children WHERE node_id=20")).isEqualTo(1);assertThat(count(connection,"SELECT father_id FROM children WHERE node_id=21")).isEqualTo(20);assertThat(count(connection,"SELECT count(*) FROM node WHERE node_id IN(20,21) AND syntax='plain-text'")).isEqualTo(2);assertThat(count(connection,"SELECT ts_creation FROM node WHERE node_id=1")).isEqualTo(100);assertThat(count(connection,"SELECT ts_lastsave FROM node WHERE node_id=1")).isGreaterThan(100);
                    try(var row=statement.executeQuery("SELECT txt FROM node WHERE node_id=21")){row.next();assertThat(row.getString(1)).contains("{{field.application}}","{{field.status}}","{{now}}");}
                    assertThatThrownBy(()->active.createExample(1)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);assertThat(count(connection,"SELECT count(*) FROM node")).isEqualTo(before+2);
                }
            }
        }
    }
    long count(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
}

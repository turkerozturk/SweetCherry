package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManagerFactory;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"myapp.openWebBrowserOnStartup=false","myapp.desktop.enabled=false","myapp.command-line-runner.enabled=false"})
class TenantJpaBootTest {
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired com.turkerozturk.node.NodeRepository nodes;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory;
    @Test void bootsWithRepositoryMetamodelWithoutSelectingAnyDatabase() {
        assertThat(TenantContext.getCurrentTenant()).isNull();
        assertThat(entityManagerFactory.getMetamodel().entity(com.turkerozturk.node.Node.class)).isNotNull();
        assertThat(nodes).isNotNull();
    }
    @Test void repositoryPaginationAndJpaWritesUseTheRoutedFactory() throws Exception {
        var file=directory.resolve("repository.ctb");
        try(var connection=java.sql.DriverManager.getConnection("jdbc:sqlite:"+file);var statement=connection.createStatement();
            var input=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")) {
            for(String line:new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\\R"))
                if(line.startsWith("CREATE TABLE") || line.startsWith("INSERT INTO")) statement.execute(line);
        }
        var source=new ManagedTenantDataSource(()->{var pool=new com.zaxxer.hikari.HikariDataSource();pool.setJdbcUrl("jdbc:sqlite:"+file);return pool;});
        var routing=(MultitenantDataSource)dataSource;
        var original=new java.util.HashMap<Object,Object>(routing.getResolvedDataSources());
        try {
            source.activate();var next=new java.util.HashMap<Object,Object>(original);next.put("Repository test",source);
            routing.setTargetDataSources(next);routing.afterPropertiesSet();TenantContext.setCurrentTenant("Repository test");
            var page=nodes.findByCustomQuery("","","",org.springframework.data.domain.PageRequest.of(0,2));
            assertThat(page.getContent()).hasSize(2);assertThat(page.getTotalElements()).isGreaterThan(2);
            new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status->{
                var manager=org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
                var node=manager.find(com.turkerozturk.node.Node.class,2L);node.setName("Changed by routed JPA");
            });
            try(var connection=source.getConnection();var statement=connection.createStatement();var row=statement.executeQuery("SELECT name,has_image FROM node WHERE node_id=2")) {
                assertThat(row.next()).isTrue();assertThat(row.getString(1)).isEqualTo("Changed by routed JPA");
                assertThat(row.getInt(2)).isIn(0,1);
            }
        } finally {TenantContext.clear();routing.setTargetDataSources(original);routing.afterPropertiesSet();source.close();}
    }
}

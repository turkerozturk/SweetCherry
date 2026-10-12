package com.turkerozturk.multipledatabases;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantEntityManagerFactoriesTest {
    @TempDir Path directory;
    @AfterEach void clear() { TenantContext.clear(); }
    private EntityManagerFactoryBuilder builder() {
        return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), ds->Map.of("hibernate.dialect",
            "org.hibernate.dialect.PostgreSQLDialect"),null);
    }
    @Test void switchesVendorsAndRoutesSpringNativeEntityManagerCreation() {
        var routing=new MultitenantDataSource();
        var sqlite=mock(ManagedTenantDataSource.class);var postgres=mock(ManagedTenantDataSource.class);var maria=mock(ManagedTenantDataSource.class);
        try {
            when(sqlite.hibernateDialect()).thenReturn(new TenantHibernateDialect(new org.hibernate.community.dialect.SQLiteDialect()));
            when(postgres.hibernateDialect()).thenReturn(new TenantHibernateDialect(new org.hibernate.dialect.PostgreSQLDialect(org.hibernate.dialect.DatabaseVersion.make(15))));
            when(maria.hibernateDialect()).thenReturn(new TenantHibernateDialect(new org.hibernate.dialect.MariaDBDialect(org.hibernate.dialect.DatabaseVersion.make(10,11))));
        } catch(SQLException failure){throw new AssertionError(failure);}
        routing.setTargetDataSources(Map.of("sqlite",sqlite,"postgres",postgres,"maria",maria));routing.afterPropertiesSet();
        var factory=new TenantEntityManagerFactories(builder(),routing,new CustomPropertiesHolder()).proxy();
        try {
            assertThat(factory.getMetamodel().getEntities()).isNotEmpty();
            for(String tenant:List.of("sqlite","postgres","maria","sqlite")) {
                TenantContext.setCurrentTenant(tenant);
                var manager=((EntityManagerFactoryInfo)factory).createNativeEntityManager(Map.of());
                try {
                    var dialect=manager.unwrap(org.hibernate.Session.class).getSessionFactory()
                        .unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
                    assertThat(dialect.getClass().getSimpleName()).isEqualTo(switch(tenant){case "postgres"->"PostgreSQLDialect";case "maria"->"MariaDBDialect";default->"SQLiteDialect";});
                } finally {manager.close();}
            }
        } finally {factory.close();}
        assertThat(factory.isOpen()).isFalse();
        try { verify(sqlite,never()).getConnection();verify(postgres,never()).getConnection();verify(maria,never()).getConnection(); }
        catch(SQLException failure){throw new AssertionError(failure); }
    }
    private ManagedTenantDataSource database(String name) throws Exception {
        Path file=directory.resolve(name+".ctb");
        try(var connection=DriverManager.getConnection("jdbc:sqlite:"+file);var statement=connection.createStatement();
            var input=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")) {
            for(String line:new String(input.readAllBytes(),StandardCharsets.UTF_8).split("\\R"))
                if(line.startsWith("CREATE TABLE") || line.startsWith("INSERT INTO")) statement.execute(line);
            statement.execute("UPDATE node SET name='"+name+"' WHERE node_id=2");
        }
        var source=new ManagedTenantDataSource(()->{var pool=new HikariDataSource();pool.setJdbcUrl("jdbc:sqlite:"+file);return pool;});
        source.activate();return source;
    }
    @Test void springTransactionsAndPaginationStayInsideTheSelectedTenant() throws Exception {
        try(var one=database("One");var two=database("Two")) {
            var routing=new MultitenantDataSource();routing.setTargetDataSources(Map.of("One",one,"Two",two));routing.afterPropertiesSet();
            var factory=new TenantEntityManagerFactories(builder(),routing,new CustomPropertiesHolder()).proxy();
            try {
                var transactions=new TransactionTemplate(new JpaTransactionManager(factory));
                for(String tenant:List.of("One","Two","One")) {
                    TenantContext.setCurrentTenant(tenant);
                    transactions.executeWithoutResult(status->{
                        var manager=EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
                        var nodes=manager.createQuery("select n from Node n order by n.nodeId",com.turkerozturk.node.Node.class)
                            .setFirstResult(1).setMaxResults(1).getResultList();
                        assertThat(nodes.get(0).getName()).isEqualTo(tenant);
                        manager.createNativeQuery("UPDATE node SET tags=:tags WHERE node_id=2").setParameter("tags",tenant).executeUpdate();
                    });
                }
                for(var entry:Map.of("One",one,"Two",two).entrySet()) {
                    try(var connection=entry.getValue().getConnection();var query=connection.createStatement();var row=query.executeQuery("SELECT tags FROM node WHERE node_id=2")) {
                        assertThat(row.next()).isTrue();assertThat(row.getString(1)).isEqualTo(entry.getKey());
                    }
                }
            } finally {factory.close();}
        }
    }
}

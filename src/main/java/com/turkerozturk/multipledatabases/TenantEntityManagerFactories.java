package com.turkerozturk.multipledatabases;

import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/** One SQL/mapping factory per dialect and server version; tenant connections remain routed. */
final class TenantEntityManagerFactories {
    private final EntityManagerFactoryBuilder builder;
    private final MultitenantDataSource source;
    private final CustomPropertiesHolder properties;
    private final Map<String,LocalContainerEntityManagerFactoryBean> factories=new LinkedHashMap<>();
    private final LocalContainerEntityManagerFactoryBean bootstrap;
    private boolean closed;

    TenantEntityManagerFactories(EntityManagerFactoryBuilder builder,MultitenantDataSource source,
                                CustomPropertiesHolder properties) {
        this.builder=builder;this.source=source;this.properties=properties;
        // Repositories require a metamodel at startup, including when all remote servers are offline.
        bootstrap=build("org.hibernate.community.dialect.SQLiteDialect");
    }
    private LocalContainerEntityManagerFactoryBean build(Object dialect) {
        var options=new HashMap<String,Object>();
        options.put("hibernate.dialect",dialect);
        options.put("hibernate.boot.allow_jdbc_metadata_access",false);
        options.put("hibernate.temp.use_jdbc_metadata_defaults",false);
        options.put("hibernate.hbm2ddl.auto","none");
        options.put("jakarta.persistence.schema-generation.database.action","none");
        // Sharing a factory must never share entity/query cache entries between tenant databases.
        options.put("hibernate.cache.use_second_level_cache",false);
        options.put("hibernate.cache.use_query_cache",false);
        var factory=builder.dataSource(source).packages("com.turkerozturk").persistenceUnit("SweetCherry")
                .properties(options).build();
        factory.afterPropertiesSet();
        return factory;
    }
    private synchronized LocalContainerEntityManagerFactoryBean current() {
        if(closed) throw new IllegalStateException("Hibernate factories are closed");
        String tenant=TenantContext.getCurrentTenant();
        if(tenant==null) return bootstrap;
        var selected=source.getResolvedDataSources().get(tenant);
        if(!(selected instanceof ManagedTenantDataSource managed))
            throw new IllegalStateException("Selected tenant is no longer registered");
        try {
            var dialect=managed.hibernateDialect();
            var custom=properties.getCustomProperties(tenant);
            Object setting=dialect.setting(custom==null?null:custom.get("custom.hibernateDialect"));
            return factories.computeIfAbsent(dialect.key(),key->build(setting));
        } catch(java.sql.SQLException failure) { throw new IllegalStateException("Cannot resolve selected tenant dialect",failure); }
    }
    private synchronized void close() {
        if(!closed) {
            closed=true;
            factories.values().forEach(LocalContainerEntityManagerFactoryBean::destroy);
            bootstrap.destroy();factories.clear();
        }
    }
    EntityManagerFactory proxy() {
        return (EntityManagerFactory)Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{EntityManagerFactory.class,EntityManagerFactoryInfo.class},(proxy,method,args)->{
            if(method.getDeclaringClass()==Object.class) return switch(method.getName()) {
                case "equals" -> proxy==args[0];case "hashCode" -> System.identityHashCode(proxy);
                default -> "SweetCherry tenant EntityManagerFactory";
            };
            if(method.getName().equals("close")) {close();return null;}
            if(method.getName().equals("isOpen")) return !closed;
            // Startup metadata is stable; only session creation/native factory selection is tenant-dependent.
            var factory=method.getName().equals("createEntityManager") || method.getName().equals("createNativeEntityManager") || method.getName().equals("getNativeEntityManagerFactory")
                    || method.getName().equals("unwrap") || method.getName().equals("getCriteriaBuilder")
                    || method.getName().equals("getMetamodel") || method.getName().equals("getProperties") ? current() : bootstrap;
            Object delegate=method.getDeclaringClass()==EntityManagerFactoryInfo.class ? factory : factory.getObject();
            try { return method.invoke(delegate,args); }
            catch(InvocationTargetException failure) { throw failure.getCause(); }
        });
    }
}

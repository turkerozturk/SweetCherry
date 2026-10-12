package com.turkerozturk.multipledatabases;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantJpaConfiguration {
    @Bean(name="entityManagerFactory", destroyMethod="close")
    public EntityManagerFactory entityManagerFactory(EntityManagerFactoryBuilder builder, DataSource dataSource,
                                                     CustomPropertiesHolder properties) {
        return new TenantEntityManagerFactories(builder,(MultitenantDataSource)dataSource,properties).proxy();
    }
}

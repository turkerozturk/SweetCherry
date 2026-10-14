package com.turkerozturk.multipledatabases;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

@Configuration(proxyBeanMethods = false)
public class TenantHealthConfiguration {
    /** Replaces Boot's routing datasource check, which also probes intentionally closed registrations. */
    @Bean(name = "dbHealthIndicator")
    public HealthIndicator dbHealthIndicator(javax.sql.DataSource dataSource) {
        return () -> {
            if (!(dataSource instanceof MultitenantDataSource routing)) return Health.unknown().build();
            int active = 0, inactive = 0, failed = 0;
            for (var source : routing.getResolvedDataSources().values()) {
                if (!(source instanceof ManagedTenantDataSource managed)) { inactive++; continue; }
                synchronized (managed) {
                    if (managed.isClosed() || !managed.configuration().isRunning()) { inactive++; continue; }
                    active++;
                    try (var connection = managed.getConnection()) {
                        if (!connection.isValid(2)) failed++;
                    } catch (java.sql.SQLException | RuntimeException error) { failed++; }
                }
            }
            return (failed == 0 ? Health.up() : Health.down())
                    .withDetail("activePools", active).withDetail("inactiveRegistrations", inactive)
                    .withDetail("failedPools", failed).build();
        };
    }
}

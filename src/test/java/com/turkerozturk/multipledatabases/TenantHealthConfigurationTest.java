package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantHealthConfigurationTest {
    @Test void inactiveRegistrationsAreNotOpened() throws Exception {
        var routing = mock(MultitenantDataSource.class);
        var closed = mock(ManagedTenantDataSource.class);
        var unused = mock(ManagedTenantDataSource.class);
        var pool = mock(com.zaxxer.hikari.HikariDataSource.class);
        when(closed.isClosed()).thenReturn(true);
        when(unused.configuration()).thenReturn(pool);
        when(routing.getResolvedDataSources()).thenReturn(Map.of("closed",closed,"unused",unused));
        var health = new TenantHealthConfiguration().dbHealthIndicator(routing).health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("inactiveRegistrations")).isEqualTo(2);
        verify(closed,never()).getConnection();
        verify(unused,never()).getConnection();
    }

    @Test void runningPoolFailureRemainsDownWithoutExposingServerMessage() throws Exception {
        var routing = mock(MultitenantDataSource.class);
        var source = mock(ManagedTenantDataSource.class);
        var pool = mock(com.zaxxer.hikari.HikariDataSource.class);
        when(source.configuration()).thenReturn(pool);
        when(pool.isRunning()).thenReturn(true);
        when(source.getConnection()).thenThrow(new java.sql.SQLException("password=private"));
        when(routing.getResolvedDataSources()).thenReturn(Map.of("privateTenant",source));
        var health = new TenantHealthConfiguration().dbHealthIndicator(routing).health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().toString()).doesNotContain("private");
    }

    @Test void runningHealthyPoolIsValidatedAndConnectionClosed() throws Exception {
        var routing = mock(MultitenantDataSource.class);
        var source = mock(ManagedTenantDataSource.class);
        var pool = mock(com.zaxxer.hikari.HikariDataSource.class);
        var connection = mock(java.sql.Connection.class);
        when(source.configuration()).thenReturn(pool);
        when(pool.isRunning()).thenReturn(true);
        when(source.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        when(routing.getResolvedDataSources()).thenReturn(Map.of("active",source));
        assertThat(new TenantHealthConfiguration().dbHealthIndicator(routing).health().getStatus()).isEqualTo(Status.UP);
        verify(connection).close();
    }
}

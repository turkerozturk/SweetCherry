package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void reportsWhetherAUsableTenantIsSelected() {
        assertThat(TenantContext.hasCurrentTenant()).isFalse();

        TenantContext.setCurrentTenant(" ");
        assertThat(TenantContext.hasCurrentTenant()).isFalse();

        TenantContext.setCurrentTenant("Demo Database");
        assertThat(TenantContext.hasCurrentTenant()).isTrue();

        TenantContext.clear();
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}

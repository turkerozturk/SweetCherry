package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class TenantConnectionFailureTest {
    @Test void classifiesWrappedErrorsWithoutReturningSensitiveText() {
        assertThat(TenantConnectionFailure.classify(new RuntimeException(new java.sql.SQLException("password=secret", "42501")))).isEqualTo("connect");
        assertThat(TenantConnectionFailure.classify(new java.sql.SQLException("secret", "28P01"))).isEqualTo("credentials");
        assertThat(TenantConnectionFailure.classify(new RuntimeException(new java.net.ConnectException("private IP")))).isEqualTo("network");
        assertThat(TenantConnectionFailure.classify(new java.sql.SQLException("secret", "3D000"))).isEqualTo("database");
        assertThat(TenantConnectionFailure.classify(new IllegalArgumentException("jdbc:secret"))).isEqualTo("unknown");
    }
}

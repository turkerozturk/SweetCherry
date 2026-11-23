package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantWizardConnectionTestsTest {
    @Test void remoteProbeRebuildsTheUrlAndUsesItsOwnTimeouts() {
        var target = TenantWizardConnectionTests.remoteTarget("postgresql",
                "jdbc:postgresql://localhost:5432/notes?sslmode=require");
        assertThat(target.url()).isEqualTo("jdbc:postgresql://localhost:5432/notes");
        assertThat(target.properties()).containsEntry("connectTimeout", "2").containsEntry("socketTimeout", "2")
                .containsEntry("sslmode", "require");
        assertThatThrownBy(() -> TenantWizardConnectionTests.remoteTarget("postgresql",
                "jdbc:postgresql://localhost/notes?socketFactory=not-permitted")).hasMessage("wizard.test.options");
        assertThatThrownBy(() -> TenantWizardConnectionTests.remoteTarget("postgresql",
                "jdbc:postgresql://another@localhost/notes")).hasMessage("wizard.invalidUrl");
    }
}

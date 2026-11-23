package com.turkerozturk.node.dtt;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DttWizardSettingsTest {
    @Test void remoteAddressIsBoundToTheServerConfiguredPeerForTheNamedToken() {
        var environment = Map.of("DTT_BRIDGE_URL_HOME", "https://dtt.example.org/context");
        assertThat(DttWizardSettings.validate("https://dtt.example.org/context/", "DTT_BRIDGE_TOKEN_HOME",
                environment::get)).hasToString("https://dtt.example.org/context/");
        assertThatThrownBy(() -> DttWizardSettings.validate("https://other.example.org/context", "DTT_BRIDGE_TOKEN_HOME",
                environment::get)).hasMessage("wizard.dttTarget");
        assertThatThrownBy(() -> DttWizardSettings.validate("https://dtt.example.org", "DTT_BRIDGE_TOKEN_HOME",
                environment::get)).hasMessage("wizard.dttTarget");
        assertThatThrownBy(() -> DttWizardSettings.validate("http://127.0.0.1:8081", "DTT_BRIDGE_TOKEN_HOME",
                environment::get)).hasMessage("wizard.dttTarget");
        assertThatThrownBy(() -> DttWizardSettings.validate("http://127.0.0.1:8081", "UNRELATED_SECRET",
                environment::get)).hasMessage("wizard.dttInvalid");
    }
}

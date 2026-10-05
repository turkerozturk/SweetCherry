package com.turkerozturk.settings;

import com.turkerozturk.sunandmoon.AstronomyProperties;
import com.turkerozturk.sunandmoon.AstronomyService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.*;

class SettingsControllerTest {
    @Test void informationModelContainsOnlySelectedNonSecretValues() {
        var controller = new SettingsController(new AstronomyService(new AstronomyProperties()));
        var model = new ExtendedModelMap();
        assertThat(controller.getSettings(model, new org.springframework.mock.web.MockHttpServletRequest())).isEqualTo("settings/settings");
        assertThat(model.keySet()).containsExactlyInAnyOrder("astronomyProperties", "openWebBrowserOnStartup",
                "debug", "syntaxHighlightingEnabled", "serverPort", "sslEnabled", "configurationValues", "requestAddress");
    }
    @Test void separatesConfiguredPortsFromObservedRequestWithoutExposingSecrets() {
        var controller = new SettingsController(new AstronomyService(new AstronomyProperties()));
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "serverPort", 443);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "httpPort", 8080);
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setServerName("localhost"); request.setServerPort(8080); request.setScheme("http");
        var model = new ExtendedModelMap(); controller.getSettings(model, request);
        assertThat(model.get("requestAddress")).isEqualTo("http://localhost:8080");
        var values = (java.util.Map<?, ?>) model.get("configurationValues");
        assertThat(values.get("server.port")).isEqualTo(443);
        assertThat(values.get("server.http.port")).isEqualTo(8080);
        assertThat(values.keySet().stream().map(Object::toString).toList()).noneMatch(key -> key.contains("password") || key.contains("private-key"));
    }
}

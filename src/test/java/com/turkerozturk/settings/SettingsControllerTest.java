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
        assertThat(controller.getSettings(model)).isEqualTo("settings/settings");
        assertThat(model.keySet()).containsExactlyInAnyOrder("astronomyProperties", "openWebBrowserOnStartup",
                "debug", "syntaxHighlightingEnabled", "serverPort", "sslEnabled");
    }
}

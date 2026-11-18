package com.turkerozturk.settings;

import com.turkerozturk.settings.editing.*;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class NodeBackgroundSettingsTest {
    @Test void literalNamedColorsAndHexAreSafeAndInvalidConfigurationFallsBack() {
        for (var value : List.of("white", " Ivory ", "RebeccaPurple", "lightgoldenrodyellow", "darkgrey", "#abc", "#abcd", "#ABCDEF", "#11223344")) {
            assertThat(NodeBackgroundAdvice.valid(value)).as(value).isTrue();
            assertThat(new NodeBackgroundAdvice(value).color()).isEqualTo(value.trim().toLowerCase(java.util.Locale.ROOT));
        }
        for (var value : List.of("red;display:none", "url(file:///secret)", "var(--color)", "currentColor", "transparent", "unknown", "#12", "#12345", "")) {
            assertThat(NodeBackgroundAdvice.valid(value)).as(value).isFalse();
            assertThat(new NodeBackgroundAdvice(value).color()).isEqualTo("white");
        }
        assertThat(new NodeBackgroundAdvice(null).color()).isEqualTo("white");
    }
    @Test void settingsAcceptColorAndQuoteHexWhileRejectingInvalidCss() {
        var values = new LinkedHashMap<String,String>();
        ApplicationSettingsService.SETTINGS.forEach(s -> values.put(s.key(),s.fallback()));
        String key="myapp.appearance.node-background-color";
        assertThat(values.get(key)).isEqualTo("white");
        values.put(key,"#fff8dc");assertThat(ApplicationSettingsService.update("myapp: {}\n",values)).contains(key+": '#fff8dc'");
        values.put(key,"PapayaWhip");assertThat(ApplicationSettingsService.update("myapp: {}\n",values)).contains("'PapayaWhip'");
        values.put(key,"white;display:none");assertThatThrownBy(() -> ApplicationSettingsService.update("myapp: {}\n",values)).isInstanceOf(IllegalArgumentException.class);
    }
}

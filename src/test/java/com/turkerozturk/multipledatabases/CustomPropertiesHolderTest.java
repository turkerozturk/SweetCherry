package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class CustomPropertiesHolderTest {
    @Test void absentAndUnknownTenantHaveNoSettings() {
        var holder = new CustomPropertiesHolder();
        assertThat(holder.getCustomProperties(null)).isNull();
        assertThat(holder.getCustomProperties("unknown")).isNull();
    }

    @Test void nullRemovalDoesNotChangeRegisteredSettings() {
        var holder = new CustomPropertiesHolder();
        var settings = Map.of("custom.maxEmbeddedFileSizeMB", "12");
        holder.addCustomProperties("Demo", settings);
        assertThatCode(() -> holder.removeCustomProperties(null)).doesNotThrowAnyException();
        assertThat(holder.getCustomProperties("Demo")).isEqualTo(settings);
    }

    @Test void removalAffectsOnlySelectedRegistration() {
        var holder = new CustomPropertiesHolder();
        holder.addCustomProperties("Demo", Map.of("propertyFileName", "demo.txt"));
        holder.addCustomProperties("Other", Map.of("propertyFileName", "other.txt"));
        holder.removeCustomProperties("Demo");
        assertThat(holder.getCustomProperties("Demo")).isNull();
        assertThat(holder.getCustomProperties("Other")).containsEntry("propertyFileName", "other.txt");
    }
}

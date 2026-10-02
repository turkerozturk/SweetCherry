package com.turkerozturk.richtext.editing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;
import static org.assertj.core.api.Assertions.*;

class EmbeddedUploadPolicyTest {
    private final CustomPropertiesHolder settings = new CustomPropertiesHolder();
    private final EmbeddedUploadPolicy policy = new EmbeddedUploadPolicy(settings);
    @AfterEach void clearTenant() { TenantContext.clear(); }
    @Test void defaultsToNineDecimalMb() { assertThat(policy.fileBytes()).isEqualTo(9_000_000); }
    @Test void usesOnlyTheCurrentTenantSettings() {
        settings.addCustomProperties("one", java.util.Map.of("custom.maxEmbeddedFileSizeMB", "2"));
        settings.addCustomProperties("two", java.util.Map.of("custom.maxEmbeddedFileSizeMB", "20"));
        TenantContext.setCurrentTenant("one"); assertThat(policy.fileBytes()).isEqualTo(2_000_000);
        TenantContext.setCurrentTenant("two"); assertThat(policy.fileBytes()).isEqualTo(20_000_000);
    }
    @Test void fallsBackForInvalidOutOfRangeAndMissingSettings() {
        TenantContext.setCurrentTenant("test");
        for (String value : java.util.List.of("", "bad", "0", "21", "-1", "9.5")) {
            settings.addCustomProperties("test", java.util.Map.of("custom.maxEmbeddedFileSizeMB", value));
            assertThat(policy.fileBytes()).isEqualTo(9_000_000);
        }
        settings.addCustomProperties("test", java.util.Map.of()); assertThat(policy.fileBytes()).isEqualTo(9_000_000);
    }
}

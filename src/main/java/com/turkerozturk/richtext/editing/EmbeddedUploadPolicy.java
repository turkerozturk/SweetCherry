package com.turkerozturk.richtext.editing;

import org.springframework.stereotype.Component;
import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;

/** Resolves a tenant's shared image/attachment size policy independently of CTB content. */
@Component
public class EmbeddedUploadPolicy {
    static final int TOTAL_BYTES = 20_000_000;
    static final int JSON_CHARACTERS = 30_000_000;
    private final CustomPropertiesHolder settings;
    public EmbeddedUploadPolicy(CustomPropertiesHolder settings) { this.settings = settings; }

    /** Uses decimal MB; absent/invalid values fall back to 9 MB, with a 20 MB request budget. */
    public int fileBytes() {
        var properties = settings.getCustomProperties(TenantContext.getCurrentTenant());
        try {
            int mb = Integer.parseInt(properties == null ? "9" : properties.getOrDefault("custom.maxEmbeddedFileSizeMB", "9").trim());
            return mb >= 1 && mb <= 20 ? mb * 1_000_000 : 9_000_000;
        } catch (NumberFormatException error) { return 9_000_000; }
    }
}

package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class TenantIconsTest {
    @Test void onlyBundledPngBasenamesAreAcceptedAndChangesAreVisible() {
        for(String input:new String[]{"", "ct_db.png", "../ct_home", "https://example.org/icon", "unknown-icon", "<img>","a".repeat(129)})
            assertThat(TenantIcons.resolve(input)).isEqualTo("ct_db");
        assertThat(TenantIcons.resolve(null)).isEqualTo("ct_db");
        assertThat(TenantIcons.resolve(" cherry_blue ")).isEqualTo("cherry_blue");
        var holder=new CustomPropertiesHolder();var icons=new TenantIcons(holder);
        assertThat(icons.icon(null)).isEqualTo("ct_db");
        holder.addCustomProperties("Demo",Map.of(TenantIcons.PROPERTY,"ct_home"));
        assertThat(icons.icon("Demo")).isEqualTo("ct_home");
        holder.addCustomProperties("Demo",Map.of(TenantIcons.PROPERTY,"cherry_red"));
        assertThat(icons.icon("Demo")).isEqualTo("cherry_red");
    }
}

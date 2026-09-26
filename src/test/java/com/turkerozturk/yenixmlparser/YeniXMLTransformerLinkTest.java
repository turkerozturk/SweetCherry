package com.turkerozturk.yenixmlparser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YeniXMLTransformerLinkTest {
    @Test void onlyHttpAndHttpsExternalLinksAreClickable() {
        assertThat(YeniXMLTransformer.safeExternalHref("https://example.org/path?a=1")).isTrue();
        assertThat(YeniXMLTransformer.safeExternalHref("http://example.org/")).isTrue();
        for (String value : new String[]{"javascript:alert(1)", "data:text/html,hi", "file:///secret",
                "https://example.org\\@evil.test/", "https://user@example.org/", "//example.org/", ""}) {
            assertThat(YeniXMLTransformer.safeExternalHref(value)).as(value).isFalse();
        }
    }
}

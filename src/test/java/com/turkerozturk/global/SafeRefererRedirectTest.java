package com.turkerozturk.global;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.assertThat;

class SafeRefererRedirectTest {
    @Test void allowsOnlyLocalNavigationAndKeepsTheQuery() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setScheme("http");
        request.addHeader("Referer", "http://localhost:8080/nodes/36?level=3");
        assertThat(SafeRefererRedirect.target(request)).isEqualTo("/nodes/36?level=3");
    }

    @Test void rejectsForeignOriginAndMissingHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(SafeRefererRedirect.target(request)).isEqualTo("/");
        request.addHeader("Referer", "https://example.org/other");
        assertThat(SafeRefererRedirect.target(request)).isEqualTo("/");
    }
}

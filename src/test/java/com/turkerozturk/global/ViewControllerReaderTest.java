package com.turkerozturk.global;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;

class ViewControllerReaderTest {
    /** Shared references keep their requested tree IDs rather than switching to the master. */
    @Test void keepsSelectedReferenceAndTenantToken() {
        assertThat(change("http://localhost/tree?nodeId=55&_tenantView=old-token", "reader"))
                .isEqualTo("redirect:/nodes/55?_tenantView=old-token");
    }

    @Test void virtualRootCanOpenTheReader() {
        assertThat(change("http://localhost/tree?nodeId=0", "reader"))
                .isEqualTo("redirect:/nodes/0");
    }

    @Test void rejectsInvalidIdsAndForeignReferers() {
        for (String id : new String[]{"-1", "abc", "999999999999999999999999"}) {
            assertThat(change("http://localhost/tree?nodeId=" + id, "reader")).isEqualTo("redirect:/");
        }
        assertThat(change("https://example.com/tree?nodeId=55", "reader")).isEqualTo("redirect:/");
    }

    @Test void existingNodePageAndLegacyModeRoutingRemainUnchanged() {
        assertThat(change("http://localhost/nodes/55?_tenantView=token", "reader"))
                .isEqualTo("redirect:/nodes/55?_tenantView=token");
        assertThat(change("http://localhost/tree?nodeId=55", "desktop")).isEqualTo("redirect:/");
    }

    private String change(String referer, String mode) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", referer);
        return new ViewController().changeView(mode, new MockHttpServletResponse(), request);
    }
}

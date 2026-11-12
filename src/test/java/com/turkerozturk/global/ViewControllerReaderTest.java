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

    @Test void existingNodePageAndLegacyModeMigrationKeepTheSelection() {
        assertThat(change("http://localhost/nodes/55?_tenantView=token", "reader"))
                .isEqualTo("redirect:/nodes/55?_tenantView=token");
        assertThat(change("http://localhost/tree?nodeId=55", "desktop")).isEqualTo("redirect:/tree?nodeId=55");
    }

    @Test void desktopWorkspacePreservesSharedReferenceAndToken() {
        assertThat(change("http://localhost/nodes/55?_tenantView=old-token", "tree"))
                .isEqualTo("redirect:/tree?nodeId=55&_tenantView=old-token");
    }

    @Test void legacyChoicesWriteOnlyMaintainedCookieValues() {
        for (String oldMode : new String[]{"desktop", "mobile"}) {
            var request = new MockHttpServletRequest();
            request.addHeader("Referer", "http://localhost/nodes/55");
            var response = new MockHttpServletResponse();
            new ViewController().changeView(oldMode, response, request);
            assertThat(response.getCookie("viewMode").getValue())
                    .isEqualTo(oldMode.equals("desktop") ? "tree" : "reader");
        }
    }

    @Test void preferenceCookieUsesSecureOnlyForTrustedHttpsAndIsNeverScriptReadable() {
        for(boolean secure:new boolean[]{false,true}) {
            var request=new MockHttpServletRequest();request.setSecure(secure);
            request.addHeader("X-Forwarded-Proto","https"); // A header alone must not mark HTTP secure.
            var response=new MockHttpServletResponse();new ViewController().changeView("tree",response,request);
            var cookie=response.getCookie("viewMode");assertThat(cookie.getSecure()).isEqualTo(secure);
            assertThat(cookie.isHttpOnly()).isTrue();assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
            assertThat(cookie.getPath()).isEqualTo("/");assertThat(cookie.getMaxAge()).isEqualTo(7*24*60*60);
        }
    }
    private String change(String referer, String mode) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", referer);
        return new ViewController().changeView(mode, new MockHttpServletResponse(), request);
    }
}

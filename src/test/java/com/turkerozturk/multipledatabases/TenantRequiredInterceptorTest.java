package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRequiredInterceptorTest {

    private final TenantRequiredInterceptor interceptor = new TenantRequiredInterceptor();

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void redirectsMarkedHandlerWhenNoTenantIsSelected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(new MockHttpServletRequest(), response,
                handlerMethod("tenantPage"));

        assertThat(allowed).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }

    @Test
    void allowsMarkedHandlerWhenTenantIsSelected() throws Exception {
        TenantContext.setCurrentTenant("Demo Database");

        boolean allowed = interceptor.preHandle(new MockHttpServletRequest(),
                new MockHttpServletResponse(), handlerMethod("tenantPage"));

        assertThat(allowed).isTrue();
    }

    @Test
    void allowsUnmarkedHandlerWithoutTenant() throws Exception {
        boolean allowed = interceptor.preHandle(new MockHttpServletRequest(),
                new MockHttpServletResponse(), handlerMethod("publicPage"));

        assertThat(allowed).isTrue();
    }

    @Test
    void oldTabCannotSubmitDeletionAfterTenantSwitch() throws Exception {
        TenantContext.setCurrentTenant("Second Database");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN, "new-view");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/nodes/delete/10");
        request.setSession(session);
        request.addParameter("_tenantView", "old-view");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, handlerMethod("publicPage"))).isFalse();
        assertThat(response.getStatus()).isEqualTo(409);

        request.setParameter("_tenantView", "new-view");
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(),
                handlerMethod("publicPage"))).isTrue();

        MockHttpServletRequest oldLink = new MockHttpServletRequest("GET", "/nodes/10");
        oldLink.setSession(session);
        oldLink.addParameter("_tenantView", "old-view");
        assertThat(interceptor.preHandle(oldLink, new MockHttpServletResponse(),
                handlerMethod("publicPage"))).isFalse();
    }

    @Test
    void moveRequiresCurrentTenantViewEvenWhenTokenIsOmitted() throws Exception {
        TenantContext.setCurrentTenant("Demo");
        var request = new MockHttpServletRequest("POST", "/nodes/move/10");
        var session = new MockHttpSession();
        session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN, "current");
        request.setSession(session);
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod("publicPage"))).isFalse();
        request.setParameter("_tenantView", "old");
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod("publicPage"))).isFalse();
        request.setParameter("_tenantView", "current");
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod("publicPage"))).isTrue();
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new TestHandler(), TestHandler.class.getMethod(methodName));
    }

    static class TestHandler {
        @RequiresTenant
        public void tenantPage() {
        }

        public void publicPage() {
        }
    }
}

package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

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

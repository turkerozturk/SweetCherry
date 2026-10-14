package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.method.HandlerMethod;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TenantListPageGuardTest {
    private final Map<String,Class<?>> pages = Map.of(
            "/nodes", com.turkerozturk.node.NodeController.class,
            "/children", com.turkerozturk.children.ChildrenController.class,
            "/father", com.turkerozturk.children.ChildrenController.class,
            "/images", com.turkerozturk.image.ImageController.class,
            "/codeboxes", com.turkerozturk.codebox.CodeBoxController.class,
            "/grids", com.turkerozturk.grid.GridController.class);

    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test void realListHandlersRedirectBeforeControllerOrRepositoryCallsWithoutTenant() throws Exception {
        TenantContext.clear();
        for (var page : pages.entrySet()) {
            var handler = handler(page.getKey(),page.getValue());
            var request = new MockHttpServletRequest("GET", "/sweet" + page.getKey());
            request.setContextPath("/sweet");
            var response = new MockHttpServletResponse();
            assertThat(new TenantRequiredInterceptor().preHandle(request,response,handler))
                    .as(page.getKey()).isFalse();
            assertThat(response.getRedirectedUrl()).isEqualTo("/sweet/");
            verifyNoInteractions(handler.getBean());
        }
    }

    @Test void realListHandlersRemainAccessibleWithSelectedTenant() throws Exception {
        TenantContext.setCurrentTenant("Demo");
        for (var page : pages.entrySet()) {
            assertThat(new TenantRequiredInterceptor().preHandle(
                    new MockHttpServletRequest("GET",page.getKey()),new MockHttpServletResponse(),
                    handler(page.getKey(),page.getValue()))).as(page.getKey()).isTrue();
        }
    }

    private HandlerMethod handler(String path,Class<?> type) {
        for (var method : type.getMethods()) {
            var mapping = method.getAnnotation(GetMapping.class);
            if (mapping != null && java.util.Arrays.asList(mapping.value()).contains(path)) {
                return new HandlerMethod(mock(type),method);
            }
        }
        throw new AssertionError("No GET handler for " + path);
    }
}

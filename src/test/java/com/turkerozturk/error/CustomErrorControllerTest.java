package com.turkerozturk.error;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomErrorControllerTest {
    @Test
    void neverExposesExceptionMessageTraceBindingErrorsOrPath() {
        var attributes = mock(ErrorAttributes.class);
        var request = new ServletWebRequest(new MockHttpServletRequest());
        when(attributes.getErrorAttributes(eq(request), any(ErrorAttributeOptions.class)))
                .thenReturn(Map.of("status", 403, "message", "secret", "trace", "private stack",
                        "exception", "private class", "errors", "private binding", "path", "/private"));
        when(attributes.getError(request)).thenReturn(new IllegalArgumentException("test failure"));
        var model = new ExtendedModelMap();
        assertThat(new CustomErrorController(attributes).handleError(request, model)).isEqualTo("error");
        assertThat(model.keySet()).containsExactlyInAnyOrder("status", "error", "errorId");
        assertThat(model.get("status")).isEqualTo(403);
        assertThat(model.get("error")).isEqualTo("Forbidden");
        assertThat(model.get("errorId").toString()).matches("[0-9a-f-]{36}");
    }

    @Test
    void handlesMissingStatusAndProducesDistinctReferences() {
        var attributes = mock(ErrorAttributes.class);
        var request = new ServletWebRequest(new MockHttpServletRequest());
        when(attributes.getErrorAttributes(eq(request), any(ErrorAttributeOptions.class))).thenReturn(Map.of());
        var controller = new CustomErrorController(attributes);
        var first = new ExtendedModelMap();
        var second = new ExtendedModelMap();
        controller.handleError(request, first);
        controller.handleError(request, second);
        assertThat(first.get("status")).isEqualTo(500);
        assertThat(first.get("errorId")).isNotEqualTo(second.get("errorId"));
    }
}

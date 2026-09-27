package com.turkerozturk.info;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;

class SessionInfoControllerTest {

    @Test
    void normalViewDoesNotCollectSessionOrCookieValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("privateValue", "secret");
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(new SessionInfoController(false).getNodeAsHtml(model, request)).isEqualTo("sessionInfo");
        assertThat(model.get("sessionMaxInactiveInterval")).isNotNull();
        assertThat(model.get("debug")).isEqualTo(false);
        assertThat(model).doesNotContainKeys("sessionId", "sessionAttributes", "cookies",
                "servletContextAttributes");
    }

    @Test
    void debugViewIncludesSessionValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("privateValue", "secret");
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(new SessionInfoController(true).getNodeAsHtml(model, request)).isEqualTo("sessionInfo");
        assertThat(model.get("debug")).isEqualTo(true);
        assertThat(model.get("sessionId")).isEqualTo(request.getSession().getId());
        assertThat(((java.util.Map<?, ?>) model.get("sessionAttributes")).get("privateValue"))
                .isEqualTo("secret");
    }
}

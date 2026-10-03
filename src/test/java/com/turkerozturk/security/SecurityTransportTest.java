package com.turkerozturk.security;

import com.turkerozturk.login.LoginAttemptLimiter;
import com.turkerozturk.login.LoginClientAddressResolver;
import com.turkerozturk.login.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityTransportTest {
    @Test
    void loginPostStillRequiresCsrf() throws Exception {
        try (var context = context(false)) {
            var mvc = mvc(context);
            // With no stored token, Spring Security invokes the configured expired-session handler.
            mvc.perform(post("/login")).andExpect(status().isFound())
                    .andExpect(redirectedUrl("/login?expired"));

            // Store an expected token but omit it from the POST: the request must be forbidden.
            var session = new MockHttpSession(context.getServletContext());
            var tokenRequest = new MockHttpServletRequest(context.getServletContext());
            tokenRequest.setSession(session);
            var repository = new HttpSessionCsrfTokenRepository();
            repository.saveToken(repository.generateToken(tokenRequest), tokenRequest, new MockHttpServletResponse());
            mvc.perform(post("/login").session(session)).andExpect(status().isForbidden());
            mvc.perform(post("/login").with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        }
    }

    @Test
    void httpsLoginRedirectAndHeadersUseSecureRequest() throws Exception {
        try (var context = context(true)) {
            mvc(context).perform(get("/settings").secure(true).with(request -> {
                request.setScheme("https");
                request.setServerName("notes.example.org");
                request.setServerPort(443);
                return request;
            })).andExpect(status().isFound())
                    .andExpect(redirectedUrl("https://notes.example.org/login"))
                    .andExpect(header().string("Referrer-Policy", "same-origin"))
                    .andExpect(header().string("Strict-Transport-Security", "max-age=31536000"));
        }
    }

    @Test
    void hstsIsAbsentOnHttpAndWhenExplicitlyDisabledForSelfSignedTls() throws Exception {
        try (var context = context(true)) {
            mvc(context).perform(get("/settings")).andExpect(status().isFound())
                    .andExpect(header().doesNotExist("Strict-Transport-Security"));
        }
        try (var context = context(false)) {
            mvc(context).perform(get("/settings").secure(true))
                    .andExpect(header().doesNotExist("Strict-Transport-Security"));
        }
    }

    private MockMvc mvc(AnnotationConfigWebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private AnnotationConfigWebApplicationContext context(boolean hsts) {
        var context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "test.hsts=" + hsts);
        context.register(TestSecurity.class);
        context.refresh();
        return context;
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    static class TestSecurity {
        @Bean InMemoryUserDetailsManager users() { return new InMemoryUserDetailsManager(); }

        @Bean SecurityFilterChain chain(HttpSecurity http, @Value("${test.hsts}") boolean hsts) throws Exception {
            var config = new SecurityConfig();
            ReflectionTestUtils.setField(config, "loginUserName", "user");
            ReflectionTestUtils.setField(config, "loginAdminName", "admin");
            ReflectionTestUtils.setField(config, "hstsEnabled", hsts);
            return config.filterChain(http, new LoginAttemptLimiter(), new LoginClientAddressResolver(""));
        }
    }
}

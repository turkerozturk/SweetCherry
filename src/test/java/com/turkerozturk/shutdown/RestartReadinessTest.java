package com.turkerozturk.shutdown;

import com.turkerozturk.login.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestartReadinessTest {
    @Test void eachContextHasItsOwnIdentityAndOnlyBecomesReadyAfterStartup() {
        var first = new ApplicationReadiness(); var next = new ApplicationReadiness();
        assertThat(first.status().getBody().ready()).isFalse();
        assertThat(first.instance()).isNotEqualTo(next.instance());
        first.ready(); assertThat(first.status().getBody().ready()).isTrue();
        assertThat(next.status().getBody().ready()).isFalse();
        assertThat(first.status().getHeaders().getCacheControl()).isEqualTo("no-store");
    }
    @Test void readinessIsAnonymousWithoutTenantWhileRestartStillRequiresAdminAndCsrf() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext()); context.register(Config.class); context.refresh();
            var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            mvc.perform(get("/application-ready")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("\"ready\":false")));
            context.getBean(ApplicationReadiness.class).ready();
            mvc.perform(get("/application-ready")).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("\"ready\":true")));
            mvc.perform(post("/restartContext").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post("/restartContext").with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
            mvc.perform(post("/restartContext").with(user("admin").roles("ADMIN"))).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?expired"));
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config {
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean ApplicationReadiness readiness(){return new ApplicationReadiness();}
        @Bean RestartController controller(ApplicationReadiness readiness){return new RestartController(readiness);}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

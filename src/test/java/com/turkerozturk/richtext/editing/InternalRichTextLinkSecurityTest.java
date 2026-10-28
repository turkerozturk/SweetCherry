package com.turkerozturk.richtext.editing;

import com.turkerozturk.login.*;
import com.turkerozturk.multipledatabases.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

class InternalRichTextLinkSecurityTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void adminCanReadTargetsButReadersStaleAndClosedTenantsNeverQuery() throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
            var service=context.getBean(InternalRichTextTargets.class);
            when(service.target(11)).thenReturn(new InternalRichTextTargets.Target("11","Name","ct_code",false,"#000000",java.util.List.of()));
            TenantContext.setCurrentTenant("Read only");
            mvc.perform(get("/nodes/richtext/links/target").session(session).param("nodeId","11").param("name","Name").param("_tenantView","current")
                    .with(user("reader").roles("ADMIN"))).andExpect(status().isOk());
            mvc.perform(get("/nodes/richtext/links/matches").session(session).param("name","Name").param("_tenantView","current")
                    .with(user("reader").roles("ADMIN"))).andExpect(status().isOk());
            verify(service).target(11);verify(service).matches("Name");clearInvocations(service);
            mvc.perform(get("/nodes/richtext/links/target").session(session).param("nodeId","11").param("name","Name").param("_tenantView","old")
                    .with(user("reader").roles("ADMIN"))).andExpect(status().isConflict());
            mvc.perform(get("/nodes/richtext/links/matches").session(session).param("name","Name").param("_tenantView","old")
                    .with(user("reader").roles("ADMIN"))).andExpect(status().isConflict());
            TenantContext.clear();
            mvc.perform(get("/nodes/richtext/links/target").session(session).param("nodeId","11").param("name","Name").param("_tenantView","current")
                    .with(user("reader").roles("ADMIN"))).andExpect(redirectedUrl("/"));
            mvc.perform(get("/nodes/richtext/links/matches").session(session).param("name","Name").param("_tenantView","current")
                    .with(user("reader").roles("ADMIN"))).andExpect(redirectedUrl("/"));
            TenantContext.setCurrentTenant("Read only");
            mvc.perform(get("/nodes/richtext/links/target").session(session).param("nodeId","11").param("_tenantView","current")
                    .with(user("reader").roles("USER"))).andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean InternalRichTextTargets service(){return mock(InternalRichTextTargets.class);}
        @Bean InternalRichTextLinkController controller(InternalRichTextTargets service){return new InternalRichTextLinkController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

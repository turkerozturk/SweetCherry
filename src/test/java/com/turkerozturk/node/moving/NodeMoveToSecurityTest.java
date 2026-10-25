package com.turkerozturk.node.moving;

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

class NodeMoveToSecurityTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void requiresAdminCsrfAndCurrentTenantTokenBeforeWriting() throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
            TenantContext.setCurrentTenant("Demo");
            mvc.perform(post("/nodes/move/2/to").session(session).param("parentId","9").param("revision","snapshot").param("_tenantView","current")
                .with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post("/nodes/move/2/to").session(session).param("parentId","9").param("revision","snapshot").param("_tenantView","current")
                .with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
            mvc.perform(post("/nodes/move/2/to").session(session).param("parentId","9").param("revision","snapshot").param("_tenantView","old")
                .with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
            var service=context.getBean(NodeMoveService.class);verifyNoInteractions(service);
            mvc.perform(post("/nodes/move/2/to").session(session).param("parentId","9").param("revision","snapshot").param("_tenantView","current")
                .with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
            verify(service).moveTo(2,9,"snapshot");
            clearInvocations(service);TenantContext.clear();
            mvc.perform(post("/nodes/move/2/to").session(session).param("parentId","9").param("revision","snapshot").param("_tenantView","current")
                .with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
            verifyNoInteractions(service);
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean NodeMoveService service(){return mock(NodeMoveService.class);}
        @Bean NodeMoveController controller(NodeMoveService service){return new NodeMoveController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

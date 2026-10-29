package com.turkerozturk.markdown;

import com.turkerozturk.login.*;
import com.turkerozturk.multipledatabases.*;
import com.turkerozturk.node.contentediting.*;
import com.turkerozturk.richtext.editing.*;
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

class MarkdownSecurityTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void previewKeepsAdminCsrfAndSourceGuardsAndDoesNotWrite()throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
            var service=context.getBean(com.turkerozturk.node.contentediting.NodeContentEditingService.class);var node=new com.turkerozturk.node.Node();node.setSyntax("markdown");when(service.editableNode(1)).thenReturn(node);TenantContext.setCurrentTenant("Notes");
            mvc.perform(post("/nodes/markdown/1/preview").session(session).param("_tenantView","current").param("text","# Hello").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post("/nodes/markdown/1/preview").session(session).param("_tenantView","stale").param("text","# Hello").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
            mvc.perform(post("/nodes/markdown/1/preview").session(session).param("_tenantView","current").param("text","# Hello").with(user("admin").roles("ADMIN"))).andExpect(result->org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(302,403));verifyNoInteractions(service);
            mvc.perform(post("/nodes/markdown/1/preview").session(session).param("_tenantView","current").param("text","# Hello").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());verify(service).editableNode(1);verify(service,never()).update(anyLong(),anyString());
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean com.turkerozturk.node.contentediting.NodeContentEditingService service(){return mock(com.turkerozturk.node.contentediting.NodeContentEditingService.class);}
        @Bean MarkdownController controller(com.turkerozturk.node.contentediting.NodeContentEditingService service){return new MarkdownController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

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

class RichTextWikiSecurityTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void readersAndReadOnlySourcesCanViewButStaleOrClosedSourcesNeverScan()throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
            var service=context.getBean(RichTextEditingService.class);
            TenantContext.setCurrentTenant("Read only");
            mvc.perform(post("/nodes/richtext/edit/1/wiki-links").param("xml","<node/>").session(session).param("_tenantView","current").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post("/nodes/richtext/edit/1/wiki-links").param("xml","<node/>").session(session).param("_tenantView","current").with(user("admin").roles("ADMIN"))).andExpect(result->org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(302,403));
            mvc.perform(post("/nodes/richtext/edit/1/wiki-links").param("xml","<node/>").session(session).param("_tenantView","old").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());verifyNoInteractions(service);
            when(service.wikiPreview(1,"<node/>")).thenReturn(new RichTextWikiLinks.Preview(java.util.Map.of("Unique","1"),java.util.List.of(),1));
            mvc.perform(post("/nodes/richtext/edit/1/wiki-links").param("xml","<node/>").session(session).param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.choices.Unique").value("1"));
            verify(service).wikiPreview(1,"<node/>");clearInvocations(service);TenantContext.clear();
            mvc.perform(post("/nodes/richtext/edit/1/wiki-links").param("xml","<node/>").session(session).param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());verifyNoInteractions(service);
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return (name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean RichTextEditingService service(){return mock(RichTextEditingService.class);}
        @Bean RichTextEditingController controller(RichTextEditingService service){return new RichTextEditingController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

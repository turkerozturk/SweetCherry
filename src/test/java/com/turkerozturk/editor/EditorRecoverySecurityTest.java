package com.turkerozturk.editor;

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

class EditorRecoverySecurityTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void receiptRoutesKeepAdminCsrfAndTenantGuards()throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
            var plain=context.getBean(NodeContentEditingService.class);var rich=context.getBean(RichTextEditingService.class);
            TenantContext.setCurrentTenant("Notes");
            for(String path:java.util.List.of("/nodes/content/2","/nodes/richtext/edit/2")) {
                mvc.perform(post(path).session(session).header("X-SweetCherry-Editor-Save","1").param("text","new").param("xml","<node/>").param("revision","revision").param("_tenantView","current").with(user("user").roles("USER")).with(csrf())).andExpect(status().isForbidden());
                mvc.perform(post(path).session(session).header("X-SweetCherry-Editor-Save","1").param("text","new").param("xml","<node/>").param("revision","revision").param("_tenantView","old").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
                mvc.perform(post(path).session(session).header("X-SweetCherry-Editor-Save","1").param("_tenantView","current").with(user("admin").roles("ADMIN"))).andExpect(result->org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(302,403));
            }
            verifyNoInteractions(plain,rich);
            mvc.perform(post("/nodes/content/2").session(session).header("X-SweetCherry-Editor-Save","1").param("text","new").param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(jsonPath("$.saved").value(true));
            verify(plain).update(2,"new");
            mvc.perform(post("/nodes/richtext/edit/2").session(session).header("X-SweetCherry-Editor-Save","1").param("xml","<node/>").param("revision","revision").param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(jsonPath("$.saved").value(true));
            verify(rich).save(2,"<node/>","revision","{}","{}","{}","[]",false,"{}");
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean NodeContentEditingService plain(){return mock(NodeContentEditingService.class);}
        @Bean RichTextEditingService rich(){return mock(RichTextEditingService.class);}
        @Bean NodeContentEditingController plainController(NodeContentEditingService service){return new NodeContentEditingController(service);}
        @Bean RichTextEditingController richController(RichTextEditingService service){return new RichTextEditingController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

package com.turkerozturk.node.tags;

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

class NodeTagSecurityTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void readersAndReadOnlySourcesCanViewButStaleOrClosedSourcesNeverScan()throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
            var service=context.getBean(NodeTagService.class);
            when(service.inspect(any())).thenAnswer(call->new NodeTagService.View(java.util.List.of(),java.util.List.of(),0,0,0,call.getArgument(0)));
            TenantContext.setCurrentTenant("Read only");
            mvc.perform(get("/nodes/tags").session(session).param("_tenantView","current").with(user("reader").roles("USER")))
                    .andExpect(status().isOk()).andExpect(view().name("node/tags"));
            mvc.perform(get("/nodes/tags/catalog").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk());
            verify(service).catalogue();
            verify(service).inspect(new NodeTagService.Options(java.util.List.of(),"any",50,1));clearInvocations(service);
            mvc.perform(get("/nodes/tags").session(session).param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict());
            mvc.perform(get("/nodes/tags/catalog").session(session).param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict());
            TenantContext.clear();
            mvc.perform(get("/nodes/tags").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(redirectedUrl("/"));
            verifyNoInteractions(service);
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return (name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean NodeTagService service(){return mock(NodeTagService.class);}
        @Bean com.turkerozturk.node.properties.NodePropertiesService properties(){return mock(com.turkerozturk.node.properties.NodePropertiesService.class);}
        @Bean NodeTagController controller(NodeTagService service,com.turkerozturk.node.properties.NodePropertiesService properties){return new NodeTagController(service,properties);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

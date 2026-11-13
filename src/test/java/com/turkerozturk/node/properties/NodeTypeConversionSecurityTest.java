package com.turkerozturk.node.properties;
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
class NodeTypeConversionSecurityTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void previewAndConversionRequireAdminCurrentTenantAndCsrf() throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
            var service=context.getBean(NodePropertiesService.class);TenantContext.setCurrentTenant("Notes");
            mvc.perform(get("/nodes/properties/2/conversion").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isForbidden());
            mvc.perform(get("/nodes/properties/2/conversion").session(session).param("_tenantView","stale").with(user("admin").roles("ADMIN"))).andExpect(status().isConflict());
            mvc.perform(post("/nodes/properties/2").session(session).with(user("admin").roles("ADMIN"))).andExpect(result->org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(302,403));
            verifyNoInteractions(service);
            String revision="a".repeat(64);when(service.conversionPreview(2)).thenReturn(new NodeTypeConversion.Preview(revision,4));
            mvc.perform(get("/nodes/properties/2/conversion").session(session).param("_tenantView","current").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.revision").value(revision));
            clearInvocations(service);
            mvc.perform(post("/nodes/properties/2").session(session).param("_tenantView","current").param("name","Note").param("color","NONE").param("icon","OTHER").param("nodeType","PLAIN_TEXT").param("confirmRichTextConversion","true").param("conversionRevision",revision).with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
            verifyNoInteractions(service);
            mvc.perform(post("/nodes/properties/2").session(session).param("_tenantView","current").param("name","Note").param("color","NONE").param("icon","OTHER").param("nodeType","PLAIN_TEXT").param("confirmRichTextConversion","true").param("conversionRevision",revision).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(view().name("redirect:/nodes/2"));
            verify(service).update(2,"Note",false,TitleColor.NONE,com.turkerozturk.helpers.NodeIcon.OTHER,false,NodeType.PLAIN_TEXT,null,null,null,null,true,revision);
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean NodePropertiesService service(){return mock(NodePropertiesService.class);}
        @Bean NodePropertiesController controller(NodePropertiesService service){return new NodePropertiesController(service);}
        @Bean org.springframework.web.servlet.ViewResolver resolver(){return (name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-calendar-view");}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

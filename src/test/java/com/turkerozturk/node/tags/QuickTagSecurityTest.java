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

class QuickTagSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void quickEditingRequiresAdminWritableTenantTokenAndCsrf()throws Exception{
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
   var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Notes");var service=context.getBean(QuickTagService.class);
   mvc.perform(get("/nodes/tags/quick/1").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isForbidden());
   mvc.perform(post("/nodes/tags/quick/1").session(session).param("_tenantView","current").param("tags","a").param("revision","r").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
   mvc.perform(post("/nodes/tags/quick/1").session(session).param("_tenantView","current").with(user("admin").roles("ADMIN"))).andExpect(result->org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(302,403));
   mvc.perform(get("/nodes/tags/quick/1").session(session).param("_tenantView","old").with(user("admin").roles("ADMIN"))).andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith("application/json"));
   mvc.perform(post("/nodes/tags/quick/1").session(session).param("_tenantView","old").param("tags","a").param("revision","r").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());verifyNoInteractions(service);
   var snapshot=new QuickTagService.Snapshot("1","1","Name","a","a".repeat(64),java.util.List.of("a"));when(service.open(1)).thenReturn(snapshot);when(service.save(1,"C++ one,two","r")).thenReturn(snapshot);
   mvc.perform(get("/nodes/tags/quick/1").session(session).param("_tenantView","current").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
   mvc.perform(post("/nodes/tags/quick/1").session(session).param("_tenantView","current").param("tags","C++ one,two").param("revision","r").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());verify(service).save(1,"C++ one,two","r");
  }
 }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return (name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean QuickTagService service(){return mock(QuickTagService.class);}
        @Bean com.turkerozturk.node.properties.NodePropertiesService properties(){return mock(com.turkerozturk.node.properties.NodePropertiesService.class);}
        @Bean QuickTagController controller(QuickTagService service){return new QuickTagController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

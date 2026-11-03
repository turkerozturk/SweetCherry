package com.turkerozturk.customlists;
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

class CustomListsSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void creationRequiresAdminCsrfAndCurrentSelectedSource()throws Exception {
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
   var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();var service=context.getBean(CustomListsService.class);
   var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("A");
   mvc.perform(post("/cherrytemplatetasks/create").session(session).param("main","true").param("_tenantView","current").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
   mvc.perform(post("/cherrytemplatetasks/create").session(session).param("main","true").param("_tenantView","current").with(user("admin").roles("ADMIN"))).andExpect(redirectedUrl("/login?expired"));
   for(String token:java.util.List.of("old",""))mvc.perform(post("/cherrytemplatetasks/create").session(session).param("main","true").param("_tenantView",token).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
   TenantContext.clear();mvc.perform(post("/cherrytemplatetasks/create").session(session).param("main","true").param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(redirectedUrl("/"));verifyNoInteractions(service);
   TenantContext.setCurrentTenant("A");when(service.createMain()).thenReturn(8L);
   mvc.perform(post("/cherrytemplatetasks/create").session(session).param("main","true").param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(view().name("redirect:/cherrytemplatetasks"));verify(service).createMain();
  }
 }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return (name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean CustomListsService service(){return mock(CustomListsService.class);}
        @Bean CustomListsController controller(CustomListsService service){return new CustomListsController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

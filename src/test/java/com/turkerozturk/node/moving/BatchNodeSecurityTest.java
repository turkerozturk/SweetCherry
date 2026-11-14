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

class BatchNodeSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void batchWritesAndPreviewRequireAdminSourceAndCsrf()throws Exception{
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
   var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Notes");var service=context.getBean(BatchNodeService.class);
   for(String endpoint:java.util.List.of("preview","apply")){
    mvc.perform(post("/nodes/batch/"+endpoint).session(session).param("_tenantView","current").param("ids","1").param("revision","r").param("operation","tag").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
    mvc.perform(post("/nodes/batch/"+endpoint).session(session).param("_tenantView","current").param("ids","1").with(user("admin").roles("ADMIN"))).andExpect(result->org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(302,403));
    mvc.perform(post("/nodes/batch/"+endpoint).session(session).param("_tenantView","old").param("ids","1").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith("application/json"));
   }verifyNoInteractions(service);
   when(service.apply(java.util.List.of(1L,9L),"revision","tag","","C++")).thenReturn(new BatchNodeService.Result(1));
   mvc.perform(post("/nodes/batch/apply").session(session).param("_tenantView","current").param("ids","1","9").param("revision","revision").param("operation","tag").param("tag","C++").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(content().string("{\"changed\":1}"));verify(service).apply(java.util.List.of(1L,9L),"revision","tag","","C++");
   mvc.perform(get("/nodes/batch/apply").session(session).with(user("admin").roles("ADMIN"))).andExpect(status().isMethodNotAllowed());
  }
 }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return (name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean BatchNodeService service(){return mock(BatchNodeService.class);}
        @Bean com.turkerozturk.node.properties.NodePropertiesService properties(){return mock(com.turkerozturk.node.properties.NodePropertiesService.class);}
        @Bean BatchNodeController controller(BatchNodeService service){return new BatchNodeController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

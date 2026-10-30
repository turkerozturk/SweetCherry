package com.turkerozturk.node.quick;

import com.turkerozturk.login.*;
import com.turkerozturk.editor.EditorRecoveryContext;
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

class QuickNodeSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void readersSearchButOnlyCurrentAdminWithCsrfMayCreate() throws Exception {
  try(var context=new AnnotationConfigWebApplicationContext()) {
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
   var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
   var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
   var service=context.getBean(QuickNodeService.class);
   when(service.search("Node")).thenReturn(new QuickNodeService.Search(java.util.List.of(),false,false));
   TenantContext.setCurrentTenant("Demo");
   mvc.perform(get("/nodes/quick/search").session(session).param("_tenantView","current").param("q","Node")
    .with(user("reader").roles("USER"))).andExpect(status().isOk());
   verify(service).search("Node");clearInvocations(service);
   mvc.perform(post("/nodes/quick/create").session(session).with(csrf()).with(user("reader").roles("USER"))
    .param("_tenantView","current").param("parentId","0").param("title","T")).andExpect(status().isForbidden());
   mvc.perform(post("/nodes/quick/create").session(session).with(csrf().useInvalidToken()).with(user("admin").roles("ADMIN"))
    .param("_tenantView","current").param("parentId","0").param("title","T")).andExpect(status().isForbidden());
   for(String token:new String[]{"old",""})
    mvc.perform(post("/nodes/quick/create").session(session).with(csrf()).with(user("admin").roles("ADMIN"))
     .param("_tenantView",token).param("parentId","0").param("title","T")).andExpect(status().isConflict());
   mvc.perform(get("/nodes/quick/search").session(session).with(user("reader").roles("USER"))
    .param("_tenantView","old").param("q","Node")).andExpect(status().isConflict());
   verifyNoInteractions(service);
   when(service.create(11,"T","body","plain-text")).thenReturn(20L);
   mvc.perform(post("/nodes/quick/create").session(session).with(csrf()).with(user("admin").roles("ADMIN"))
    .param("_tenantView","current").param("parentId","11").param("title","T").param("text","body"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("20"));
   verify(service).create(11,"T","body","plain-text");clearInvocations(service);
   TenantContext.clear();
   mvc.perform(get("/nodes/quick/search").session(session).with(user("reader").roles("USER"))
    .param("_tenantView","current").param("q","Node")).andExpect(status().isConflict())
    .andExpect(content().contentTypeCompatibleWith("application/json"));
   verifyNoInteractions(service);
  }
 }
 @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
 static class Config implements WebMvcConfigurer {
  @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
  @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
  @Bean QuickNodeService service(){return mock(QuickNodeService.class);}
  @Bean EditorRecoveryContext scope(){return mock(EditorRecoveryContext.class);}
  @Bean QuickNodeController controller(QuickNodeService service,EditorRecoveryContext scope){return new QuickNodeController(service,scope);}
  @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
  @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
   var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
   return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
  }
 }
}

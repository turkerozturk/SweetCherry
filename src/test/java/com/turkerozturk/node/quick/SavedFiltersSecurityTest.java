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

class SavedFiltersSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void preferencesRequireCsrfAndCurrentSourceWhileReadersMayRead()throws Exception{
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
   var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
   var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Demo");
   var store=context.getBean(com.turkerozturk.node.filter.saved.SavedFiltersStore.class);
   var scope=context.getBean(EditorRecoveryContext.class);when(scope.scope(any())).thenReturn("a".repeat(64));
   when(store.read(anyString())).thenReturn(new com.turkerozturk.node.filter.saved.SavedFiltersStore.Snapshot("a".repeat(64),"revision",java.util.List.of()));
   mvc.perform(get("/nodes/saved-filters").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk());verify(store).read("a".repeat(64));clearInvocations(store);
   mvc.perform(post("/nodes/saved-filters").session(session).param("_tenantView","current").contentType("application/json").content("{}").with(user("reader").roles("USER"))).andExpect(status().is3xxRedirection());
   mvc.perform(post("/nodes/saved-filters").session(session).param("_tenantView","old").contentType("application/json").content("{}").with(csrf()).with(user("reader").roles("USER"))).andExpect(status().isConflict());
   mvc.perform(get("/nodes/backlinks/1").session(session).param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict());verifyNoInteractions(store);
  }
 }
 @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
 static class Config implements WebMvcConfigurer {
  @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
  @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
  @Bean com.turkerozturk.node.filter.saved.SavedFiltersStore service(){return mock(com.turkerozturk.node.filter.saved.SavedFiltersStore.class);}
  @Bean EditorRecoveryContext scope(){return mock(EditorRecoveryContext.class);}
  @Bean com.turkerozturk.node.filter.saved.SavedFiltersController controller(com.turkerozturk.node.filter.saved.SavedFiltersStore service,EditorRecoveryContext scope){return new com.turkerozturk.node.filter.saved.SavedFiltersController(service,scope);}
 @Bean com.turkerozturk.node.links.NodeBacklinksController backlinks(){return new com.turkerozturk.node.links.NodeBacklinksController(mock(com.turkerozturk.node.links.InternalLinkReportService.class));}
  @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
  @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
   var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
   return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
  }
 }
}

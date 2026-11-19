package com.turkerozturk.node.filter.saved;

import com.turkerozturk.editor.EditorRecoveryContext;
import com.turkerozturk.login.*;
import com.turkerozturk.multipledatabases.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SavedSearchTreeSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void readerQueriesUseCurrentUserSourceAndCannotBypassTenantOrDisabledSetting()throws Exception {
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
   var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
   var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Demo");
   var service=context.getBean(SavedSearchTreeService.class);var scopes=context.getBean(EditorRecoveryContext.class);
   when(scopes.scope(any())).thenReturn("reader-source");when(service.list("reader-source")).thenReturn(List.of());String url="/nodes/saved-filters/tree";
   mvc.perform(get(url).session(session).param("_tenantView","current").param("scope","another-user").with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
   verify(service).list("reader-source");clearInvocations(service);
   mvc.perform(get(url).session(session).param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict());
   mvc.perform(get(url).session(session).with(user("reader").roles("USER"))).andExpect(status().isBadRequest());
   mvc.perform(get(url).session(session).param("_tenantView","current").with(anonymous())).andExpect(status().is3xxRedirection());
   mvc.perform(post(url).session(session).param("_tenantView","current").with(csrf()).with(user("reader").roles("USER"))).andExpect(status().isMethodNotAllowed());
   var controller=context.getBean(SavedSearchTreeController.class);ReflectionTestUtils.setField(controller,"enabled",false);
   mvc.perform(get(url).session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isNotFound());verifyNoInteractions(service);
   ReflectionTestUtils.setField(controller,"enabled",true);when(scopes.scope(any())).thenReturn("");
   mvc.perform(get(url).session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isServiceUnavailable());verifyNoInteractions(service);
   when(scopes.scope(any())).thenReturn("reader-source");
   ReflectionTestUtils.setField(controller,"enabled",true);String id="12345678-1234-1234-1234-123456789abc";
   when(service.results("reader-source",id,1)).thenReturn(new SavedSearchTreeService.Results(id,"Tasks",1,0,false,List.of()));
   mvc.perform(get(url+"/"+id).session(session).param("page","1").param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
   verify(service).results("reader-source",id,1);clearInvocations(service);TenantContext.clear();
   mvc.perform(get(url).session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isConflict());verifyNoInteractions(service);
  }
 }
 @Configuration @EnableWebSecurity @EnableWebMvc static class Config implements WebMvcConfigurer {
  @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
  @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
  @Bean SavedSearchTreeService service(){return mock(SavedSearchTreeService.class);}
  @Bean EditorRecoveryContext scope(){return mock(EditorRecoveryContext.class);}
  @Bean SavedSearchTreeController controller(SavedSearchTreeService service,EditorRecoveryContext scopes){return new SavedSearchTreeController(service,scopes);}
  @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
  @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));}
 }
}

package com.turkerozturk.richtext.editing;

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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WebBookmarkSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void readersMayPreviewButAnonymousStaleMissingAndDisabledRequestsCannotFetch()throws Exception{
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
   var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Read-only source");String url="/nodes/web-bookmarks/55";
   var service=context.getBean(WebBookmarkService.class);when(service.load(55)).thenReturn(new WebBookmarkService.Preview("<p>Safe</p>"));
   mvc.perform(get(url).session(session).param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict());
   mvc.perform(get(url).session(session).with(user("reader").roles("USER"))).andExpect(status().isBadRequest());
   mvc.perform(get(url).session(session).param("_tenantView","current").with(anonymous())).andExpect(status().is3xxRedirection());verifyNoInteractions(service);
   mvc.perform(get(url).session(session).param("_tenantView","current").param("url","http://127.0.0.1/").with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));verify(service).load(55);clearInvocations(service);
   mvc.perform(post(url).session(session).param("_tenantView","current").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isMethodNotAllowed());
   ReflectionTestUtils.setField(context.getBean(WebBookmarkController.class),"enabled",false);
   mvc.perform(get(url).session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isNotFound());verifyNoInteractions(service);
   ReflectionTestUtils.setField(context.getBean(WebBookmarkController.class),"enabled",true);when(service.load(55)).thenThrow(new PublicWebFetcher.Problem("blocked"));
   mvc.perform(get(url).session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").value("blocked"));
  }
 }
 @Configuration @EnableWebSecurity @EnableWebMvc static class Config implements WebMvcConfigurer{
  @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
  @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
  @Bean WebBookmarkService service(){return mock(WebBookmarkService.class);}
  @Bean WebBookmarkController controller(WebBookmarkService service){return new WebBookmarkController(service);}
  @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
  @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));}
 }
}

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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SortChildrenSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void sortingRequiresAdminCsrfConfirmationAndTheCurrentTenant()throws Exception{
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
   String route="/nodes/children/1/sort";var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Notes");
   for(String method:java.util.List.of("GET","POST")){
    var request=method.equals("GET")?get(route):post(route);
    mvc.perform(request.session(session).param("key","NAME").param("order","ASC").param("revision","r").param("confirmed","true").param("_tenantView","current").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
   }
   mvc.perform(post(route).session(session).param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
   for(String token:java.util.List.of("old",""))mvc.perform(post(route).session(session).param("key","NAME").param("order","ASC").param("revision","r").param("confirmed","true").param("_tenantView",token).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
   mvc.perform(post(route).session(session).param("key","NAME").param("order","ASC").param("revision","r").param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest());
   SortChildrenService service=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(context.getBean(SortChildrenService.class));verifyNoInteractions(service);
   mvc.perform(post(route).session(session).param("key","NAME").param("order","ASC").param("revision","r").param("confirmed","true").param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());verify(service).sort(1L,SortChildrenService.Key.NAME,SortChildrenService.Order.ASC,"r",true);
   clearInvocations(service);TenantContext.clear();
   mvc.perform(post(route).session(session).param("key","NAME").param("order","ASC").param("revision","r").param("confirmed","true").param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());verifyNoInteractions(service);
  }
 }
 @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity static class Config implements WebMvcConfigurer{
  @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
  @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
  @Bean SortChildrenService service(){return mock(SortChildrenService.class);}
  @Bean SortChildrenController controller(SortChildrenService service){return new SortChildrenController(service);}
  @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
  @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));}
 }
}

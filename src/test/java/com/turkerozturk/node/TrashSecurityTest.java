package com.turkerozturk.node;
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
class TrashSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void recoveryRequiresAdminCurrentSourceAndCsrf()throws Exception{
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
   var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Notes");var service=context.getBean(TrashService.class);
   mvc.perform(get("/nodes/trash").session(session).with(user("reader").roles("USER"))).andExpect(status().isForbidden());
   for(String action:java.util.List.of("restore","purge")){
    mvc.perform(post("/nodes/trash/100/"+action).session(session).param("_tenantView","current").param("revision","r").param("destination","original").param("confirmed","true").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
    mvc.perform(post("/nodes/trash/100/"+action).session(session).param("_tenantView","current").param("revision","r").param("destination","original").with(user("admin").roles("ADMIN"))).andExpect(r->org.assertj.core.api.Assertions.assertThat(r.getResponse().getStatus()).isIn(302,403));
    mvc.perform(post("/nodes/trash/100/"+action).session(session).param("_tenantView","old").param("revision","r").param("destination","original").param("confirmed","true").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
   }
   verifyNoInteractions(service);
   when(service.restore(100,"r",false)).thenReturn(2L);
   mvc.perform(post("/nodes/trash/100/restore").session(session).param("_tenantView","current").param("revision","r").param("destination","original").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/tree?nodeId=2"));verify(service).restore(100,"r",false);
   doThrow(TrashSql.problem(org.springframework.http.HttpStatus.BAD_REQUEST,"trash.confirmRequired")).when(service).purge(100,"r",false);
   mvc.perform(post("/nodes/trash/100/purge").session(session).param("_tenantView","current").param("revision","r").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest()).andExpect(view().name("node/trashError"));
   mvc.perform(post("/nodes/trash/100/purge").session(session).param("_tenantView","current").param("revision","r").param("confirmed","true").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(redirectedUrl("/nodes/trash"));verify(service).purge(100,"r",true);
  }
 }
 @Test void disabledModeNeverAcceptsNewTrashWrites(){var properties=mock(com.turkerozturk.node.properties.NodePropertiesService.class);when(properties.writable()).thenReturn(true);var service=new TrashService(properties,false);org.assertj.core.api.Assertions.assertThatThrownBy(()->service.trash(2,"r")).hasMessageContaining("trash.disabled");org.assertj.core.api.Assertions.assertThatThrownBy(()->service.preview(2)).hasMessageContaining("trash.disabled");}
 @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
 static class Config implements WebMvcConfigurer {
  @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return (name,locale)->name.startsWith("redirect:")?new org.springframework.web.servlet.view.RedirectView(name.substring(9)):new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
  @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
  @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
  @Bean TrashService service(){return mock(TrashService.class);}
  @Bean TrashController controller(TrashService service){return new TrashController(service);}
  @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
  @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));}
 }
}

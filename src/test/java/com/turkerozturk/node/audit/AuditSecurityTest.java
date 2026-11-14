package com.turkerozturk.node.audit;
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
import java.util.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
class AuditSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void historyAndCompleteDownloadsRequireAdminAndTheCurrentSource()throws Exception{
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Notes");var service=context.getBean(AuditReadService.class);String id=UUID.randomUUID().toString();
   for(String path:List.of("/nodes/audit","/nodes/audit/"+id,"/nodes/audit/"+id+"/source","/nodes/audit/"+id+"/download")){
    mvc.perform(get(path).session(session).param("_tenantView","current").param("nodeId","2").param("stage","before").with(user("reader").roles("USER"))).andExpect(status().isForbidden());
    mvc.perform(get(path).session(session).param("_tenantView","old").param("nodeId","2").param("stage","before").with(user("admin").roles("ADMIN"))).andExpect(status().isConflict());
   }verifyNoInteractions(service);
   var e=new AuditSql.Event(1,id,"2026-11-14T12:00:00Z","admin","content.edit",List.of(2L),List.of(),List.of(),List.of(),List.of(),List.of());String raw="<script>unsafe()</script>😀".repeat(1000);when(service.event(id)).thenReturn(e);when(service.source(e,2,"before",true)).thenReturn(raw);
   mvc.perform(get("/nodes/audit/"+id+"/source").session(session).param("_tenantView","current").param("nodeId","2").param("stage","before").param("raw","true").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("Content-Disposition","attachment; filename=\"ctblogs-2-before-source.txt\"")).andExpect(content().contentTypeCompatibleWith("text/plain")).andExpect(content().string(raw));verify(service).source(e,2,"before",true);
   mvc.perform(get("/nodes/audit/"+id+"/source").session(session).param("_tenantView","current").param("nodeId","2").param("stage","../../secret").with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
   mvc.perform(get("/nodes/audit").session(session).param("_tenantView","current").param("day","invalid").with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
  }
 }
 @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
 static class Config implements WebMvcConfigurer {
  @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return (name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
  @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
  @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
  @Bean AuditReadService service(){return mock(AuditReadService.class);}
  @Bean AuditController controller(AuditReadService service){return new AuditController(service,false);}
  @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
  @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));}
 }
}

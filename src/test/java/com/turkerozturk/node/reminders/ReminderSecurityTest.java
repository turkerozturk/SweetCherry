package com.turkerozturk.node.reminders;

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

class ReminderSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder save(String token){
  var request=post("/nodes/reminders/save").param("nodeId","1").param("contentId","1").param("localTime","2026-11-18T10:00").param("zone","UTC").param("message","Call").param("revision","r");
  if(token!=null)request.param("_tenantView",token);return request;
 }
 @Test void readsAllowReadersAndEveryMutationRequiresAdminCsrfAndCurrentSource()throws Exception {
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
   var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
   var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Notes");
   var service=context.getBean(ReminderService.class);
   mvc.perform(get("/nodes/reminders")).andExpect(status().is3xxRedirection());
   mvc.perform(get("/nodes/reminders").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(model().attribute("canEditReminders",false));
   verify(service).inspect(new ReminderService.Options("","pending",50,1));clearInvocations(service);
   mvc.perform(get("/nodes/reminders/new/1").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isForbidden());
   mvc.perform(save("current").session(session).with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
   mvc.perform(save("current").session(session).with(user("admin").roles("ADMIN"))).andExpect(result->org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(302,403));
   for(String token:java.util.Arrays.asList("old","",null))mvc.perform(save(token).session(session).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
   mvc.perform(post("/nodes/reminders/11111111-1111-4111-8111-111111111111/state").session(session).param("state","done").param("revision","r").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
   mvc.perform(get("/nodes/reminders").session(session).param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict());verifyNoInteractions(service);
   mvc.perform(save("current").session(session).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/nodes/reminders?q=&status=all&limit=50&page=1&_tenantView=current"));
   verify(service).save("",1,1,"2026-11-18T10:00","UTC","Call","r");clearInvocations(service);
   mvc.perform(post("/nodes/reminders/11111111-1111-4111-8111-111111111111/state").session(session).param("_tenantView","current").param("state","done").param("revision","r").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection());
   verify(service).changeState("11111111-1111-4111-8111-111111111111","done","r");clearInvocations(service);
   when(service.save("",1,1,"2026-11-18T10:00","UTC","Call","r")).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"readonly"));
   mvc.perform(save("current").session(session).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isForbidden()).andExpect(view().name("node/reminder-error"));
   TenantContext.clear();clearInvocations(service);mvc.perform(get("/nodes/reminders").session(session).with(user("reader").roles("USER"))).andExpect(redirectedUrl("/"));verifyNoInteractions(service);
  }
 }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean org.springframework.web.servlet.ViewResolver viewResolver(){
            return (name,locale)->name.startsWith("redirect:")
                    ? new org.springframework.web.servlet.view.RedirectView(name.substring(9))
                    : new org.springframework.web.servlet.view.InternalResourceView("/test-view");
        }
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean ReminderService service(){return mock(ReminderService.class);}
        @Bean com.turkerozturk.node.properties.NodePropertiesService properties(){return mock(com.turkerozturk.node.properties.NodePropertiesService.class);}
        @Bean ReminderController controller(ReminderService service,com.turkerozturk.node.properties.NodePropertiesService properties){return new ReminderController(service,properties);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

package com.turkerozturk.customlists.views;
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

class RecordViewSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void readViewsAndAdminCreationRespectCsrfAndSelectedConnection()throws Exception {
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();var service=context.getBean(RecordViewService.class);
   var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
   mvc.perform(get("/cherrytemplatenode/1/views").session(session).with(user("reader").roles("USER"))).andExpect(redirectedUrl("/"));verifyNoInteractions(service);
   TenantContext.setCurrentTenant("A");mvc.perform(get("/cherrytemplatenode/1/views").session(session).param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict());verifyNoInteractions(service);
   mvc.perform(get("/cherrytemplatenode/1/views").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk());verify(service).catalog(1);clearInvocations(service);
   mvc.perform(get("/cherrytemplatenode/1/view/11/pdf").session(session).param("recordId","2").param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict());verifyNoInteractions(service);
   String path="/cherrytemplatenode/1/views/example";
   mvc.perform(post(path).session(session).param("_tenantView","current").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
   mvc.perform(post(path).session(session).param("_tenantView","current").with(user("admin").roles("ADMIN"))).andExpect(redirectedUrl("/login?expired"));
   mvc.perform(post(path).session(session).param("_tenantView","old").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
   mvc.perform(post(path).session(session).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());verifyNoInteractions(service);
   mvc.perform(post(java.net.URI.create("/cherrytemplatenode/%31/views/example")).session(session).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());verifyNoInteractions(service);
   when(service.createExample(1)).thenReturn(11L);mvc.perform(post(path).session(session).param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(view().name("redirect:/cherrytemplatenode/1/views"));verify(service).createExample(1);
   var record=RecordViewRendererTest.record();var page=new RecordViewService.Page(null,new RecordViewService.Choice(11,11,"Design",true),record,new RecordViewRenderer.Output("safe",java.util.List.of()));
   when(service.render(1,11,2,"current",java.util.Locale.ENGLISH)).thenReturn(page);when(context.getBean(RecordViewPdf.class).export(page)).thenReturn(new byte[]{37,80,68,70});
   mvc.perform(get("/cherrytemplatenode/1/view/11/pdf").session(session).param("recordId","2").param("_tenantView","current").locale(java.util.Locale.ENGLISH).with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")));

  }
 }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return (name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean RecordViewPdf pdf(){return mock(RecordViewPdf.class);}
        @Bean RecordViewService service(){return mock(RecordViewService.class);}
        @Bean RecordViewController controller(RecordViewService service){return new RecordViewController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

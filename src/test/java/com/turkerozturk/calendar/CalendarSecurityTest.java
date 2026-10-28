package com.turkerozturk.calendar;

import com.turkerozturk.login.*;
import com.turkerozturk.multipledatabases.*;
import com.turkerozturk.node.contentediting.*;
import com.turkerozturk.richtext.editing.*;
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

class CalendarSecurityTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void calendarReadersCanViewButWritesNeedAdminCsrfAndTheCurrentSource()throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
            var service=context.getBean(CalendarService.class);TenantContext.setCurrentTenant("Notes");
            mvc.perform(get("/nodes/calendar/month").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk());verify(service).view(eq("month"),any(),any(),eq("any"),any());clearInvocations(service);
            mvc.perform(post("/nodes/calendar/11/created").session(session).param("_tenantView","current").param("expectedCreated","1").param("expectedModified","2").param("local","2030-01-01T00:00").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post("/nodes/calendar/11/created").session(session).param("_tenantView","stale").param("expectedCreated","1").param("expectedModified","2").param("local","2030-01-01T00:00").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
            mvc.perform(post("/nodes/calendar/11/created").session(session).param("_tenantView","current").with(user("admin").roles("ADMIN"))).andExpect(result->org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(302,403));
            verifyNoInteractions(service);
            mvc.perform(post("/nodes/calendar/11/created").session(session).param("_tenantView","current").param("expectedCreated","1").param("expectedModified","2").param("local","2030-01-01T00:00").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(jsonPath("$.saved").value(true));verify(service).changeCreated(11,1,2,"2030-01-01T00:00");clearInvocations(service);
            TenantContext.clear();mvc.perform(get("/nodes/calendar/week").session(session).with(user("admin").roles("ADMIN"))).andExpect(redirectedUrl("/"));verifyNoInteractions(service);
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean CalendarService service(){return mock(CalendarService.class);}
        @Bean CalendarSettings settings(){return new CalendarSettings("MONDAY","UTC",10000);}
        @Bean com.turkerozturk.node.properties.NodePropertiesService properties(){return mock(com.turkerozturk.node.properties.NodePropertiesService.class);}
        @Bean com.turkerozturk.node.tags.NodeTagService tags(){return mock(com.turkerozturk.node.tags.NodeTagService.class);}
        @Bean CalendarController controller(CalendarService service,CalendarSettings settings,com.turkerozturk.node.properties.NodePropertiesService properties,com.turkerozturk.node.tags.NodeTagService tags){return new CalendarController(service,settings,properties,tags);}
        @Bean org.springframework.web.servlet.ViewResolver resolver(){return (name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-calendar-view");}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

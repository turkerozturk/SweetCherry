package com.turkerozturk.security;

import com.turkerozturk.login.*;
import com.turkerozturk.multipledatabases.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

class TenantWizardSecurityTest {
    @Test void wizardAndBrowseRejectUserAndSaveRejectsInvalidCsrf()throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            mvc.perform(get("/tenants/wizard").with(user("reader").roles("USER"))).andExpect(status().isForbidden());
            mvc.perform(get("/tenants/wizard/browse").with(user("reader").roles("USER"))).andExpect(status().isForbidden());
            mvc.perform(post("/tenants/wizard").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post("/tenants/wizard").with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
            mvc.perform(post("/tenants/wizard").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isFound()).andExpect(redirectedUrl("/"));
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config {
        @Bean TenantService tenants(){return mock(TenantService.class);}
        @Bean javax.sql.DataSource datasource(){return mock(javax.sql.DataSource.class);}
        @Bean CustomPropertiesHolder properties(){return new CustomPropertiesHolder();}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean TenantWizardService service()throws Exception{var s=mock(TenantWizardService.class);when(s.save(any())).thenReturn("Demo");return s;}
        @Bean DatabaseSwitchController selection(){var c=mock(DatabaseSwitchController.class);when(c.setTenant(eq("Demo"),any())).thenReturn("redirect:/");return c;}
        @Bean TenantWizardController controller(TenantWizardService service,DatabaseSwitchController selection){return new TenantWizardController(service,selection);}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

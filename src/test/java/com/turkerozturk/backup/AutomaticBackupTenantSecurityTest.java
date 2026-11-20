package com.turkerozturk.backup;

import com.turkerozturk.login.*;
import com.turkerozturk.multipledatabases.TenantService;
import com.turkerozturk.settings.editing.ApplicationSettingsService;
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

class AutomaticBackupTenantSecurityTest {
    @org.junit.jupiter.api.AfterEach void clear(){com.turkerozturk.multipledatabases.TenantContext.clear();}
    @Test void enablingUsesOnlyTheCurrentTenantAndRequiresAdminCsrfAndViewToken()throws Exception{
        try(var context=new AnnotationConfigWebApplicationContext()){
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();String route="/system/backups/automatic/enable";
            var session=new org.springframework.mock.web.MockHttpSession();session.setAttribute(com.turkerozturk.multipledatabases.TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
            com.turkerozturk.multipledatabases.TenantContext.setCurrentTenant("Notes");
            mvc.perform(post(route).session(session).param("revision","r").param("_tenantView","current").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post(route).session(session).param("revision","r").param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
            mvc.perform(post(route).session(session).param("revision","r").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
            mvc.perform(post(route).session(session).param("revision","r").param("_tenantView","old").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
            var scheduler=context.getBean(CtbBackupScheduler.class);verifyNoInteractions(scheduler);
            mvc.perform(post(route).session(session).param("revision","r").param("_tenantView","current").param("tenant","Other").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(redirectedUrl("/system/backups")).andExpect(flash().attribute("automaticBackupNotice","backupTenant.enabled"));
            verify(scheduler).enableTenant(eq("Notes"),any());
        }
    }
    @Test void onlyAdminWithCsrfCanRemoveAndNoSelectedDatabaseIsRequired()throws Exception{
        try(var context=new AnnotationConfigWebApplicationContext()){
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();String route="/tenants/backups/automatic/remove";
            mvc.perform(post(route).param("tenant","Notes").param("revision","r").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post(route).param("tenant","Notes").param("revision","r").with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
            var scheduler=context.getBean(CtbBackupScheduler.class);verifyNoInteractions(scheduler);
            mvc.perform(post(route).param("tenant","Notes").param("revision","r").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/tenants")).andExpect(flash().attribute("automaticBackupNotice","backupTenant.removed"));
            verify(scheduler).removeTenant(eq("Notes"),any());
            mvc.perform(post(route).param("tenant","Missing").param("revision","r").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(flash().attribute("automaticBackupNotice","backupTenant.changed"));
            verifyNoMoreInteractions(scheduler);
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
        @Override public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry){registry.addInterceptor(new com.turkerozturk.multipledatabases.TenantRequiredInterceptor());}
        @Bean com.turkerozturk.multipledatabases.CustomPropertiesHolder customPropertiesHolder(){return new com.turkerozturk.multipledatabases.CustomPropertiesHolder();}
        @Bean javax.sql.DataSource dataSource(){return mock(javax.sql.DataSource.class);}
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean ApplicationSettingsService settings(){return mock(ApplicationSettingsService.class);}
        @Bean CtbBackupScheduler scheduler(){return mock(CtbBackupScheduler.class);}
        @Bean TenantService tenants(){var value=mock(TenantService.class);when(value.getAllTenants()).thenReturn(java.util.Map.of("Notes",mock(javax.sql.DataSource.class)));return value;}
        @Bean AutomaticBackupTenantController controller(ApplicationSettingsService settings,CtbBackupScheduler scheduler,TenantService tenants){return new AutomaticBackupTenantController(settings,scheduler,tenants);}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));}
    }
}

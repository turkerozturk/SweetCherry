package com.turkerozturk.backup;

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

class CtbBackupSecurityTest {
    @AfterEach void clear() {TenantContext.clear();}
    @Test void onlyAdminCanListStartDownloadOrPrepareAWorkingCopyAndCsrfIsRequired() throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            for(String path:new String[]{"/system/backups","/system/backups/status/id","/system/backups/id/download"})
                mvc.perform(get(path).with(user("user").roles("USER"))).andExpect(status().isForbidden());
            for(String path:new String[]{"/system/backups/start","/system/backups/id/open"}) {
                mvc.perform(post(path).with(user("user").roles("USER")).with(csrf())).andExpect(status().isForbidden());
                mvc.perform(post(path).with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
            }
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
            TenantContext.setCurrentTenant("Selected");
            mvc.perform(post("/system/backups/start").session(session).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
            mvc.perform(post("/system/backups/start").session(session).param("_tenantView","old").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
            verifyNoInteractions(context.getBean(CtbBackupService.class));
            mvc.perform(post("/system/backups/start").session(session).param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isFound()).andExpect(redirectedUrl("/system/backups"));
            verify(context.getBean(CtbBackupService.class)).start(any(),eq("Selected"));
            TenantContext.clear();
            mvc.perform(post("/system/backups/start").session(session).param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isFound()).andExpect(redirectedUrl("/"));
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        @Override public void addInterceptors(InterceptorRegistry registry) {registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean CtbBackupSettings settings(){return new CtbBackupSettings();}
        @Bean CtbBackupService service(){return mock(CtbBackupService.class);}
        @Bean CtbBackupStore store(){return mock(CtbBackupStore.class);}
        @Bean CtbBackupEngine engine(){return mock(CtbBackupEngine.class);}
        @Bean javax.sql.DataSource datasource(){return mock(javax.sql.DataSource.class);}
        @Bean CustomPropertiesHolder properties(){return new CustomPropertiesHolder();}
        @Bean TenantService tenants(){return mock(TenantService.class);}
        @Bean CtbBackupController controller(CtbBackupSettings settings,CtbBackupService service,CtbBackupStore store,CtbBackupEngine engine,TenantService tenants){return new CtbBackupController(settings,service,store,engine,tenants);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

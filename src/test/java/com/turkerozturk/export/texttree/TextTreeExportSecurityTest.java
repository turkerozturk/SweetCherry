package com.turkerozturk.export.texttree;

import com.turkerozturk.login.*;
import com.turkerozturk.multipledatabases.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
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
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TextTreeExportSecurityTest {
    @TempDir Path dir;
    @AfterEach void clear(){TenantContext.clear();}
    @Test void readonlyReaderCanDownloadButCsrfStaleMissingAndClosedSourcesNeverExport()throws Exception{
        try(var context=new AnnotationConfigWebApplicationContext()){
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();var service=context.getBean(TextTreeExportService.class);
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Readonly");
            Path path=dir.resolve("archive.zip");Files.write(path,new byte[]{1,2,3});when(service.export(11,true)).thenReturn(new TextTreeExportService.Archive(path,"Türkçe.zip",3));
            mvc.perform(post("/nodes/export/text/11").session(session).param("includeDescendants","true").param("_tenantView","current").with(user("reader").roles("USER")).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1,2,3})).andExpect(header().string("Content-Type","application/zip")).andExpect(header().string("Cache-Control","no-store"));
            assertThat(path).doesNotExist();verify(service).export(11,true);clearInvocations(service);
            mvc.perform(post("/nodes/export/text/11").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(redirectedUrl("/login?expired"));
            for(String token:new String[]{"old",""})mvc.perform(post("/nodes/export/text/11").session(session).param("_tenantView",token).with(user("reader").roles("USER")).with(csrf())).andExpect(status().isConflict());
            TenantContext.clear();mvc.perform(post("/nodes/export/text/11").session(session).param("_tenantView","current").with(user("reader").roles("ADMIN")).with(csrf())).andExpect(redirectedUrl("/"));
            verifyNoInteractions(service);
        }
    }
    @Test void limitFailureRedirectsWithTranslatedFlashAndPreservesScopeAndTenantToken()throws Exception{
        var service=mock(TextTreeExportService.class);when(service.export(11,false)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE));
        var mvc=MockMvcBuilders.standaloneSetup(new TextTreeExportController(service,new TextTreeExportLimits())).build();
        mvc.perform(post("/nodes/export/text/11").param("_tenantView","current"))
            .andExpect(redirectedUrl("/nodes/export/text/11?_tenantView=current"))
            .andExpect(flash().attribute("textTreeError","textTree.error.limit")).andExpect(flash().attribute("includeDescendants",false));
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer{
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean TextTreeExportService service(){return mock(TextTreeExportService.class);}
        @Bean TextTreeExportController controller(TextTreeExportService s){return new TextTreeExportController(s,new TextTreeExportLimits());}
        @Override public void addInterceptors(InterceptorRegistry r){r.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));}
    }
}

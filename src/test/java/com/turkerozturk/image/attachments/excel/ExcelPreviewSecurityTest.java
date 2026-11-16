package com.turkerozturk.image.attachments.excel;
import com.turkerozturk.image.attachments.*;
import com.turkerozturk.login.*;
import com.turkerozturk.multipledatabases.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
class ExcelPreviewSecurityTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void authenticatedReadersCanPreviewButAnonymousStaleMissingTokensAndWritesAreBlocked()throws Exception{
        try(var context=new AnnotationConfigWebApplicationContext()){
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Read-only notes");
            var service=context.getBean(ExcelPreviewService.class);String url="/attachments/1/0/excel";
            mvc.perform(get(url).session(session).param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict()).andExpect(header().string("Cache-Control","no-store"));
            mvc.perform(get(url).session(session).param("_tenantView","current").with(anonymous())).andExpect(status().is3xxRedirection());
            mvc.perform(get(url).session(session).with(user("reader").roles("USER"))).andExpect(status().isBadRequest());verifyNoInteractions(service);
            mvc.perform(get(url).session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(view().name("attachment/excel")).andExpect(header().string("Cache-Control","no-store"));
            mvc.perform(get(url).session(session).param("_tenantView","current").cookie(new jakarta.servlet.http.Cookie("viewMode","mobile")).with(user("reader").roles("USER"))).andExpect(view().name("attachment/excelMobile"));
            when(service.preview(eq(1L),eq(0),anyInt(),anyInt(),any())).thenThrow(new ExcelPreviewService.Problem("encrypted"));
            mvc.perform(get(url).session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isUnprocessableEntity()).andExpect(model().attribute("excelError","encrypted"));
            clearInvocations(service);TenantContext.clear();
            mvc.perform(get(url).session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isConflict());verifyNoInteractions(service);
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc
    static class Config implements WebMvcConfigurer{
        @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return(name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean ExcelPreviewService previews(){return mock(ExcelPreviewService.class);}

        @Bean ExcelPreviewController previewController(ExcelPreviewService service){return new ExcelPreviewController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));}
    }
}

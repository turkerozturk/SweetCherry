package com.turkerozturk.image.gallery;

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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

class GallerySecurityTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void readersCanBrowseButStaleMissingOrUnauthenticatedRequestsCannotReadAnotherDatabase() throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()){
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
            TenantContext.setCurrentTenant("Read-only notes");var service=context.getBean(GalleryService.class);
            for(String path:new String[]{"/image-gallery","/image-gallery/1/0/preview","/image-gallery/1/0/original"}){
                mvc.perform(get(path).session(session).param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict());
                mvc.perform(get(path).session(session).param("_tenantView","current").with(anonymous())).andExpect(status().is3xxRedirection());
            }
            mvc.perform(get("/image-gallery/1/0/preview").session(session).with(user("reader").roles("USER"))).andExpect(status().isBadRequest());
            verifyNoInteractions(service);
            when(service.list(any())).thenReturn(new GalleryService.Page(java.util.List.of(),0,new GalleryService.Options(1,"","newest")));
            when(service.payload(1,0)).thenReturn(new GalleryService.Payload(GalleryPreviewTest.picture(),"<unsafe>.png"));
            mvc.perform(get("/image-gallery").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(view().name("gallery/gallery")).andExpect(header().string("Cache-Control","no-store"));
            mvc.perform(get("/image-gallery/1/0/preview").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(header().string("Cache-Control","no-store"));
            mvc.perform(get("/image-gallery/1/0/original").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(content().contentType("application/octet-stream")).andExpect(header().string("Content-Disposition","attachment; filename=\"image-1-0.png\""));
            mvc.perform(post("/image-gallery").session(session).with(user("reader").roles("USER")).with(csrf())).andExpect(status().isMethodNotAllowed());
            clearInvocations(service);TenantContext.clear();
            mvc.perform(get("/image-gallery/1/0/original").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isConflict()).andExpect(header().string("Cache-Control","no-store"));verifyNoInteractions(service);
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc
    static class Config implements WebMvcConfigurer {
        @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return (name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean GalleryService service(){return mock(GalleryService.class);}
        @Bean GalleryController controller(GalleryService service){return new GalleryController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));}
    }
}

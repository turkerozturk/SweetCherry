package com.turkerozturk.codebox.gallery;
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
class CodeAndAttachmentSecurityTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void readersCanBrowseDownloadAndUseTheMobileListButStaleTabsCannotReadAnotherSource()throws Exception{
        try(var context=new AnnotationConfigWebApplicationContext()){
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Read-only notes");
            var code=context.getBean(CodeGalleryService.class);var files=context.getBean(AttachmentBrowseService.class);
            for(String path:List.of("/codebox-gallery/1/0/data","/codebox-gallery/1/0/source","/attachments/1/0/download")){
                mvc.perform(get(path).session(session).param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict()).andExpect(header().string("Cache-Control","no-store"));
                mvc.perform(get(path).session(session).param("_tenantView","current").with(anonymous())).andExpect(status().is3xxRedirection());
                mvc.perform(get(path).session(session).with(user("reader").roles("USER"))).andExpect(status().isBadRequest());
            }verifyNoInteractions(code,files);
            var data=new CodeGalleryService.Data("<script>literal()</script>😀","javascript","javascript",30,false);when(code.data(1,0)).thenReturn(data);when(code.source(1,0)).thenReturn(data);
            mvc.perform(get("/codebox-gallery/1/0/data").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(jsonPath("$.text").value(data.text())).andExpect(header().string("Cache-Control","no-store"));
            mvc.perform(get("/codebox-gallery/1/0/source").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(content().contentType("application/octet-stream")).andExpect(content().bytes(data.text().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            when(files.list(any())).thenReturn(new AttachmentBrowseService.Page(List.of(),0,new AttachmentBrowseService.Options(1,"","newest")));
            mvc.perform(get("/attachments").session(session).param("_tenantView","current").cookie(new jakarta.servlet.http.Cookie("viewMode","mobile")).with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(view().name("attachmentsMobile"));
            doAnswer(call->{java.util.function.Consumer<AttachmentBrowseService.Download> headers=call.getArgument(2);headers.accept(new AttachmentBrowseService.Download("ä\".txt",3));((java.io.OutputStream)call.getArgument(3)).write(new byte[]{0,1,(byte)255});return null;}).when(files).stream(eq(1L),eq(0),any(),any());
            mvc.perform(get("/attachments/1/0/download").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk()).andExpect(content().bytes(new byte[]{0,1,(byte)255})).andExpect(content().contentType("application/octet-stream")).andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(header().string("Content-Length","3"));
            clearInvocations(code,files);TenantContext.clear();mvc.perform(get("/attachments/1/0/download").session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isConflict());verifyNoInteractions(code,files);
        }
    }
    @Configuration @EnableWebSecurity @EnableWebMvc
    static class Config implements WebMvcConfigurer{
        @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return(name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean CodeGalleryService codes(){return mock(CodeGalleryService.class);}
        @Bean AttachmentBrowseService files(){return mock(AttachmentBrowseService.class);}
        @Bean CodeGalleryController codeController(CodeGalleryService service){return new CodeGalleryController(service,CodeGalleryServiceTest.settings());}
        @Bean AttachmentBrowseController filesController(AttachmentBrowseService service){return new AttachmentBrowseController(service);}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));}
    }
}

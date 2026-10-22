package com.turkerozturk.richtext.editing;

import com.turkerozturk.login.*;
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
import static org.assertj.core.api.Assertions.*;

class WebContentImportSecurityTest {
    @Test void authenticatedAdminAndCsrfAreRequiredBeforeNetworkAccess() throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            mvc.perform(post("/nodes/richtext/import/1").param("url","https://example.org").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post("/nodes/richtext/import/1").param("url","https://example.org").with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
            verifyNoInteractions(context.getBean(WebContentImportService.class),Config.editingMock);
            mvc.perform(post("/nodes/richtext/import/1").param("url","https://example.org").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Page"));
            verify(Config.editingMock).open(1);
        }
    }
    @Test void readOnlyOrDisabledImportDoesNotFetch() {
        var editing=mock(RichTextEditingService.class);var importer=mock(WebContentImportService.class);var controller=new WebContentImportController(editing,importer);
        when(editing.open(1)).thenThrow(new org.springframework.security.access.AccessDeniedException("read only"));
        assertThatThrownBy(()->controller.load(1,"https://example.org")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoInteractions(importer);
        ReflectionTestUtils.setField(controller,"enabled",false);
        assertThatThrownBy(()->controller.load(1,"https://example.org")).isInstanceOf(PublicWebFetcher.Problem.class).hasMessage("disabled");verifyNoInteractions(importer);
    }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config {
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        static final RichTextEditingService editingMock=mock(RichTextEditingService.class);
        @Bean WebContentImportService importer(){var s=mock(WebContentImportService.class);when(s.load(anyString())).thenReturn(new WebContentImportService.Result("Page","https://example.org","<p>Text</p>",java.util.Map.of(),0));return s;}
        @Bean WebContentImportController controller(WebContentImportService importer){return new WebContentImportController(editingMock,importer);}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}

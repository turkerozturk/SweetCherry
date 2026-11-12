package com.turkerozturk.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/** Exercises the APIs affected by the maintenance updates, rather than comparing version strings. */
class DependencyCompatibilityTest {
    public record Example(String name, java.time.LocalDate date) { }
    @Test void jacksonModulesRoundTripExistingRecordAndJavaTimeData() throws Exception {
        var mapper=tools.jackson.databind.json.JsonMapper.builder().findAndAddModules().build();
        var input=new Example("Türkçe 🍒",java.time.LocalDate.of(2026,11,12));
        assertThat(mapper.readValue(mapper.writeValueAsString(input),Example.class)).isEqualTo(input);
    }
    @Test void readsJsonWrittenBeforeTheJacksonMigration() {
        var mapper=tools.jackson.databind.json.JsonMapper.builder().build();
        assertThat(mapper.readValue("{\"name\":\"Türkçe 🍒\",\"date\":\"2026-11-12\"}",Example.class))
                .isEqualTo(new Example("Türkçe 🍒",java.time.LocalDate.of(2026,11,12)));
    }
    @Test void securityDialectShowsAdminControlsOnlyForAdministrators() {
        try(var application=new org.springframework.web.context.support.AnnotationConfigWebApplicationContext()) {
            var servlet=new org.springframework.mock.web.MockServletContext();
            application.setServletContext(servlet);application.register(WebSecurity.class);application.refresh();
            servlet.setAttribute(org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,application);
            var request=new org.springframework.mock.web.MockHttpServletRequest(servlet);
            request.setAttribute(org.springframework.web.servlet.DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE,application);
            var engine=new org.thymeleaf.spring6.SpringTemplateEngine();
            engine.setTemplateResolver(new org.thymeleaf.templateresolver.StringTemplateResolver());
            engine.addDialect(new org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect());
            var context=new org.thymeleaf.context.WebContext(org.thymeleaf.web.servlet.JakartaServletWebApplication
                    .buildApplication(servlet).buildExchange(request,new org.springframework.mock.web.MockHttpServletResponse()));
            String template="<button sec:authorize=\"hasRole('ADMIN')\">Admin action</button>";
            try {
                for(String role:java.util.List.of("USER","ADMIN")) {
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                            org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated("test","unused",
                                    java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+role))));
                    assertThat(engine.process(template,context).contains("Admin action")).isEqualTo(role.equals("ADMIN"));
                }
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
                assertThat(engine.process(template,context)).doesNotContain("Admin action");
            } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        }
    }
    @org.springframework.context.annotation.Configuration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class WebSecurity { }
    @Test void standaloneThymeleafStillEvaluatesOgnlExpressions() {
        var engine=new org.thymeleaf.TemplateEngine();engine.setTemplateResolver(new org.thymeleaf.templateresolver.StringTemplateResolver());
        var context=new org.thymeleaf.context.Context();context.setVariable("note",java.util.Map.of("name","Türkçe & text"));
        assertThat(engine.process("<p th:text=\"${note.name}\">old</p>",context)).isEqualTo("<p>Türkçe &amp; text</p>");
    }
}

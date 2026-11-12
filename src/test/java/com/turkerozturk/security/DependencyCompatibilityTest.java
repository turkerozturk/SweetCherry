package com.turkerozturk.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/** Exercises the APIs affected by the maintenance updates, rather than comparing version strings. */
class DependencyCompatibilityTest {
    public record Example(String name, java.time.LocalDate date) { }
    @Test void jacksonModulesRoundTripExistingRecordAndJavaTimeData() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        var input=new Example("Türkçe 🍒",java.time.LocalDate.of(2026,11,12));
        assertThat(mapper.readValue(mapper.writeValueAsString(input),Example.class)).isEqualTo(input);
    }
    @Test void standaloneThymeleafStillEvaluatesOgnlExpressions() {
        var engine=new org.thymeleaf.TemplateEngine();engine.setTemplateResolver(new org.thymeleaf.templateresolver.StringTemplateResolver());
        var context=new org.thymeleaf.context.Context();context.setVariable("note",java.util.Map.of("name","Türkçe & text"));
        assertThat(engine.process("<p th:text=\"${note.name}\">old</p>",context)).isEqualTo("<p>Türkçe &amp; text</p>");
    }
}

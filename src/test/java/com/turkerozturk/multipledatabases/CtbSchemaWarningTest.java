package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class CtbSchemaWarningTest {
    @Test void instructionsRenderInBothLanguagesWithEscapedConfigName() throws Exception {
        String fragment;
        try (var stream = getClass().getResourceAsStream("/templates/common/tenantFragment.html")) {
            String html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            fragment = html.substring(html.indexOf("    <div class=\"alert alert-warning\""), html.indexOf("    <div th:replace=\"~{common/backupWarningFragment"));
        }
        for (String language : List.of("en", "tr")) {
            Locale locale = Locale.forLanguageTag(language);
            var properties = new Properties();
            String resource = language.equals("tr") ? "/messages_tr.properties" : "/messages.properties";
            try (var stream = getClass().getResourceAsStream(resource)) {
                properties.load(new java.io.InputStreamReader(stream, StandardCharsets.UTF_8));
            }
            var messages = new StaticMessageSource();
            properties.stringPropertyNames().forEach(key -> messages.addMessage(key, locale, properties.getProperty(key)));
            var engine = new SpringTemplateEngine(); engine.setTemplateResolver(new StringTemplateResolver()); engine.setMessageSource(messages);
            var warning = Map.of("missing", "children.master_id", "supported", true, "sql", CtbSchemaCompatibility.UPGRADE_SQL, "config", "<private>.txt");
            String html = engine.process(fragment, new Context(locale, Map.of("ctbSchemaWarning", warning)));
            assertThat(html).contains("children.master_id", "INTEGER DEFAULT 0", "custom.allowLegacySchemaUpgrade=true", "&lt;private&gt;.txt").doesNotContain("<private>.txt", "??tenant.");
        }
    }
}

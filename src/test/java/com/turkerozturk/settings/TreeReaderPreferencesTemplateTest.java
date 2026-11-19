package com.turkerozturk.settings;

import com.turkerozturk.settings.editing.NodeBackgroundAdvice;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class TreeReaderPreferencesTemplateTest {
    @Test void standaloneTreeLoadsPreferencesAndConfiguredBackgroundInBothLanguagesAndContextPaths() {
        for (var language : List.of("en", "tr")) for (var path : List.of("", "/app")) {
            var engine = new SpringTemplateEngine(); var resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
            engine.setTemplateResolver(resolver);
            var messages = new ResourceBundleMessageSource(); messages.setBasename("messages"); messages.setDefaultEncoding("UTF-8");
            engine.setTemplateEngineMessageSource(messages);
            var servlet = new MockServletContext(); var request = new MockHttpServletRequest(servlet);
            request.setContextPath(path); request.setRequestURI(path + "/tree");
            var context = new WebContext(JakartaServletWebApplication.buildApplication(servlet)
                    .buildExchange(request, new MockHttpServletResponse()), Locale.forLanguageTag(language));
            context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"));
            context.setVariable("tenantNames", List.of()); context.setVariable("currentTenantName", "Demo");
            context.setVariable("currentDatabaseIcon", "zero"); context.setVariable("tenantViewToken", "current");
            context.setVariable("treeDefaultWidth", 600); context.setVariable("treeFontSize", 16);
            context.setVariable("treeVerticalPadding", 1); context.setVariable("initialNodeId", 9L);
            for (var color : List.of("ivory", "#fff8dc")) for (var theme : List.of("light", "dark")) {
                context.setVariable("nodeBackgroundColor", new NodeBackgroundAdvice(color).color());
                context.setVariable("uiTheme", theme);
                var html = engine.process("node/treeWorkspace", context); var page = org.jsoup.Jsoup.parse(html);
                assertThat(page.selectFirst("html").attr("style")).isEqualTo("--sc-node-background:" + color);
                assertThat(page.selectFirst("html").attr("data-bs-theme")).isEqualTo(theme);
                assertThat(page.select("link[href='" + path + "/css/node/readerPreferences.css']")).hasSize(1);
                for (var script : List.of("readerPreferencesState.js", "readerPreferences.js")) {
                    var loaded = page.select("head script[src='" + path + "/js/common/" + script + "']");
                    assertThat(loaded).hasSize(1); assertThat(loaded.first().hasAttr("defer")).isTrue();
                }
                assertThat(page.select("[data-confirm-toggle]").eachAttr("data-confirm-toggle"))
                        .containsExactly("creation", "duplication", "deletion");
                assertThat(page.select("[data-node-background-dialog]")).hasSize(1);
                assertThat(page.select("script[src='" + path + "/js/node/treeKeyboard.js']")).hasSize(1);
                assertThat(page.selectFirst("#workspace-tree").attr("aria-label")).isNotBlank();
                assertThat(html).doesNotContain("??preferences.");
            }
        }
    }
}

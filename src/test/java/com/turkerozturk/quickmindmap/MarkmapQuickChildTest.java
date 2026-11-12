package com.turkerozturk.quickmindmap;

import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeRepository;
import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.quickmindmapmarkmap.MarkmapQuickMindMapController;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.*;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.ui.ExtendedModelMap;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarkmapQuickChildTest {
    @Test void onlyWritableAdminsReceiveCreationControls() {
        var properties = mock(NodePropertiesService.class);
        var controller = new MarkmapQuickMindMapController(mock(ChildrenRepository.class),
                mock(NodeRepository.class), tools.jackson.databind.json.JsonMapper.builder().build(), properties);
        var request = new MockHttpServletRequest();
        when(properties.writable()).thenReturn(true);
        assertThat(controller.canAddChildren(request)).isFalse();
        request.addUserRole("ADMIN");
        assertThat(controller.canAddChildren(request)).isTrue();
        when(properties.writable()).thenReturn(false);
        assertThat(controller.canAddChildren(request)).isFalse();
    }

    @Test void sharedParentsKeepTheirExactOccurrenceIdForCreation() throws Exception {
        var children = mock(ChildrenRepository.class);
        var nodes = mock(NodeRepository.class);
        long id = Long.MAX_VALUE;
        var alias = new Children().setNodeId(id).setFatherId(0);
        alias.setMasterId(10L);
        var display = new Node(); display.setName("<Shared parent>");
        when(children.findByNodeId(id)).thenReturn(alias);
        when(nodes.findById(10L)).thenReturn(display);
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var controller = new MarkmapQuickMindMapController(children, nodes, mapper, mock(NodePropertiesService.class));
        var model = new ExtendedModelMap();
        controller.showMarkmapQuickMindMap(id, 0, false, false, false, model);
        var data = mapper.readTree((String) model.get("markmapDataJson"));
        assertThat(data.path("payload").path("parentId").asText()).isEqualTo(Long.toString(id));
        assertThat(data.path("content").asText()).isEqualTo("<Shared parent>");
    }

    @Test void bilingualDialogIsAbsentForReadersAndPostsOnlyNameAndFixedEmptyPlainText() throws Exception {
        for (var locale : List.of(Locale.ENGLISH, Locale.forLanguageTag("tr"))) {
            var engine = new SpringTemplateEngine();
            var resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/"); resolver.setSuffix(".html");
            engine.setTemplateResolver(resolver);
            var messages = new StaticMessageSource(); var values = new Properties();
            try (var input = Files.newBufferedReader(Path.of("src/main/resources/messages_" + locale.getLanguage() + ".properties"))) { values.load(input); }
            values.forEach((key, value) -> messages.addMessage(key.toString(), locale, value.toString()));
            engine.setTemplateEngineMessageSource(messages);
            var servlet = new MockServletContext(); var request = new MockHttpServletRequest(servlet);
            var context = new WebContext(JakartaServletWebApplication.buildApplication(servlet)
                    .buildExchange(request, new MockHttpServletResponse()), locale);
            context.setVariables(Map.of("markmapCanAdd", true, "rootNodeId", 11L, "rootNodeName", "Parent",
                    "rootParentNodeId", 0L, "maximumLevel", 3, "icons", true, "colors", true, "foldChat", true,
                    "markmapDataJson", "{}", "tenantViewToken", "current"));
            context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "csrf-value"));
            String html = engine.process("markmap-quick-mind-map", context);
            var dialog = org.jsoup.Jsoup.parse(html).selectFirst("#markmap-child-dialog");
            assertThat(dialog).isNotNull();
            assertThat(dialog.select("input:not([type=hidden])")).hasSize(1);
            assertThat(dialog.selectFirst("form").attr("action")).isEqualTo("/nodes/quick/create");
            assertThat(dialog.selectFirst("input[name=_tenantView]").val()).isEqualTo("current");
            assertThat(dialog.selectFirst("input[name=_csrf]").val()).isEqualTo("csrf-value");
            assertThat(dialog.selectFirst("input[name=text]").val()).isEmpty();
            assertThat(dialog.selectFirst("input[name=syntax]").val()).isEqualTo("plain-text");
            assertThat(html).doesNotContain("??markmap.child.", "??quick.");
            context.setVariable("markmapCanAdd", false);
            html = engine.process("markmap-quick-mind-map", context);
            assertThat(org.jsoup.Jsoup.parse(html).select("#markmap-child-dialog")).isEmpty();
            assertThat(html).doesNotContain("src=\"/js/markmap/quickChild.js\"");
        }
    }
}

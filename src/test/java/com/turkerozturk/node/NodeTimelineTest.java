package com.turkerozturk.node;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.context.support.StaticMessageSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeTimelineTest {
    @Test void createdTimelineRendersListAndEscapesNodeName() throws Exception {
        var controller = new NodeController(); var service = mock(NodeService.class);
        ReflectionTestUtils.setField(controller, "nodeService", service);
        var node = new Node(); node.setNodeId(55); node.setName("<script>unsafe</script>");
        node.setCreationTimestamp(1790624738L); node.setLastSaveTimestamp(1790624738L);
        when(service.findRecentlyCreatedNodes()).thenReturn(List.of(node));
        var model = new ExtendedModelMap(); model.addAttribute("tenantViewToken", "old-token");
        assertThat(controller.getTimelineCreated(model)).isEqualTo("node/timeline");
        assertThat(render(model)).contains("&lt;script&gt;unsafe&lt;/script&gt;", "_tenantView=old-token", "/nodes/55");
    }
    @Test void modifiedTimelineRendersAnEmptyList() throws Exception {
        var controller = new NodeController(); var service = mock(NodeService.class);
        ReflectionTestUtils.setField(controller, "nodeService", service);
        when(service.findRecentlyModifiedNodes()).thenReturn(List.of());
        var model = new ExtendedModelMap();
        assertThat(controller.getTimelineModified(model)).isEqualTo("node/timeline");
        assertThat(render(model)).contains("Modified").doesNotContain("getTotalElements");
    }
    @Test void retiredSearchRedirectsToBindingSearch() {
        assertThat(new NodeController().getAllNodesAdvancedAsHtml(new ExtendedModelMap()))
                .isEqualTo("redirect:/nodesadvancedwithbinding");
    }
    private String render(ExtendedModelMap model) throws Exception {
        var messages = new StaticMessageSource();
        messages.addMessage("timeline.created", Locale.ENGLISH, "Created");
        messages.addMessage("timeline.modified", Locale.ENGLISH, "Modified");
        messages.addMessage("timeline.name", Locale.ENGLISH, "Name");
        messages.addMessage("timeline.count", Locale.ENGLISH, "Nodes: {0}");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(new StringTemplateResolver());
        engine.setTemplateEngineMessageSource(messages);
        var servletContext = new MockServletContext();
        var request = new MockHttpServletRequest(servletContext);
        var exchange = JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(request, new MockHttpServletResponse());
        return engine.process(Files.readString(Path.of("src/main/resources/templates/node/timeline.html")),
                new WebContext(exchange, Locale.ENGLISH, model));
    }
}

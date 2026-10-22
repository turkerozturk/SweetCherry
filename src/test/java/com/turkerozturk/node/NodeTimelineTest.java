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
        node.setNodeIcon(com.turkerozturk.helpers.NodeIcon.CT_HOME);
        node.setCreationTimestamp(1790624738L); node.setLastSaveTimestamp(1790624738L);
        when(service.findRecentlyCreatedNodes(50)).thenReturn(List.of(node));
        var model = new ExtendedModelMap(); model.addAttribute("tenantViewToken", "old-token");
        assertThat(controller.getTimelineCreated(model,"50")).isEqualTo("node/timeline");
        assertThat(render(model)).contains("&lt;script&gt;unsafe&lt;/script&gt;", "_tenantView=old-token", "/nodes/55/info", "/img/ctbicons/ct_home.png", "max=\"5000\"");
    }
    @Test void modifiedTimelineRendersAnEmptyList() throws Exception {
        var controller = new NodeController(); var service = mock(NodeService.class);
        ReflectionTestUtils.setField(controller, "nodeService", service);
        when(service.findRecentlyModifiedNodes(50)).thenReturn(List.of());
        var model = new ExtendedModelMap();
        assertThat(controller.getTimelineModified(model,"50")).isEqualTo("node/timeline");
        assertThat(render(model)).contains("Modified").doesNotContain("getTotalElements");
    }
    @Test void retiredSearchRedirectsToBindingSearch() {
        assertThat(new NodeController().getAllNodesAdvancedAsHtml(new ExtendedModelMap()))
                .isEqualTo("redirect:/nodesadvancedwithbinding");
    }
    @Test void rowLimitIsAppliedAndMalformedLimitsSafelyFallBackToFifty() {
        var controller=new NodeController();var service=mock(NodeService.class);
        ReflectionTestUtils.setField(controller,"nodeService",service);
        var model=new ExtendedModelMap();controller.getTimelineCreated(model,"125");
        verify(service).findRecentlyCreatedNodes(125);assertThat(model.get("timelineLimit")).isEqualTo(125);
        for(String value:List.of("0","-1","5001","9999999999999999999","abc","")) {
            model=new ExtendedModelMap();controller.getTimelineModified(model,value);
            assertThat(model.get("timelineLimit")).isEqualTo(50);assertThat(model.get("timelineLimitError")).isEqualTo(true);
        }
        verify(service,times(6)).findRecentlyModifiedNodes(50);
    }
    @Test void servicePassesRequestedPageSizeToRepository() {
        var service=new NodeService();var repository=mock(NodeRepository.class);
        ReflectionTestUtils.setField(service,"nodeRepository",repository);
        service.findRecentlyCreatedNodes(125);service.findRecentlyModifiedNodes(77);
        verify(repository).findRecentlyCreatedNodes(org.springframework.data.domain.PageRequest.of(0,125));
        verify(repository).findRecentlyModifiedNodes(org.springframework.data.domain.PageRequest.of(0,77));
        assertThatThrownBy(()->service.findRecentlyCreatedNodes(5001)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void metadataUsesTheExistingFragmentWithoutRenderingNodeContent() {
        var controller=new NodeController();var nodes=mock(NodeService.class);
        var children=mock(com.turkerozturk.children.ChildrenService.class);
        ReflectionTestUtils.setField(controller,"nodeService",nodes);ReflectionTestUtils.setField(controller,"childrenService",children);
        var alias=new com.turkerozturk.children.Children().setNodeId(10);alias.setMasterId(1L);
        when(children.findById(10L)).thenReturn(alias);var master=new Node();master.setNodeId(1);
        when(nodes.findById(1L)).thenReturn(master);when(children.getSharedNodeIdsByMasterId(1L)).thenReturn(List.of(10L));
        var model=new ExtendedModelMap();assertThat(controller.timelineNodeInfo(10,model)).isEqualTo("node/nodeInfoFragment :: info");
        assertThat(model.get("node")).isSameAs(master);assertThat(model.get("sharedNodeIds")).isEqualTo(List.of(10L));
        verify(nodes).findById(1L);verifyNoMoreInteractions(nodes);
        assertThatThrownBy(()->controller.timelineNodeInfo(-1,new ExtendedModelMap())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->controller.timelineNodeInfo(999,new ExtendedModelMap())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    private String render(ExtendedModelMap model) throws Exception {
        var messages = new StaticMessageSource();
        messages.addMessage("timeline.created", Locale.ENGLISH, "Created");
        messages.addMessage("timeline.modified", Locale.ENGLISH, "Modified");
        messages.addMessage("timeline.recentCreated", Locale.ENGLISH, "Recently Added");
        messages.addMessage("timeline.recentModified", Locale.ENGLISH, "Recently Modified");
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

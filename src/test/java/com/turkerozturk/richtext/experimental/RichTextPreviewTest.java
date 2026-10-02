package com.turkerozturk.richtext.experimental;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeRepository;
import com.turkerozturk.node.NodeContentParserService;
import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.image.ImageService;
import com.turkerozturk.multipledatabases.TenantContext;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class RichTextPreviewTest {
    private final NodeRepository nodes = mock(NodeRepository.class);
    private final ChildrenRepository children = mock(ChildrenRepository.class);
    private final ImageService images = mock(ImageService.class);
    private final NodeContentParserService legacy = mock(NodeContentParserService.class);
    private final RichTextPreviewService service = new RichTextPreviewService(nodes, children, images, legacy);

    private Node node(long treeId, long contentId) {
        var child = mock(Children.class);
        when(child.getMasterId()).thenReturn(treeId == contentId ? 0L : contentId);
        when(children.findByNodeId(treeId)).thenReturn(child);
        var node = mock(Node.class);
        when(nodes.findById(contentId)).thenReturn(node);
        when(node.getSyntax()).thenReturn("custom-colors");
        when(node.getTxt()).thenReturn("<node><rich_text>Example</rich_text></node>");
        when(node.getName()).thenReturn("Title");
        when(node.getCodeBoxes()).thenReturn(Set.of());
        when(node.getGrids()).thenReturn(Set.of());
        when(images.getImagesByNodeId(contentId)).thenReturn(List.of());
        when(legacy.parseNodeTxt(node)).thenReturn("<p>Old</p>");
        return node;
    }

    @Test void rendersBothPipelinesWithoutSaving() {
        node(53, 53);
        var result = service.compare(53, "token");
        assertThat(result.legacy().html()).contains("<p>Old</p>");
        assertThat(result.experimental().html()).contains("Example");
        assertThat(result.experimental().failed()).isFalse();
        verify(nodes, never()).save(any());
    }

    @Test void resolvesAliasWhileKeepingItsTreeId() {
        node(55, 53);
        var result = service.compare(55, "token");
        assertThat(result.treeNodeId()).isEqualTo(55);
        assertThat(result.contentNodeId()).isEqualTo(53);
        verify(images).getImagesByNodeId(53L);
    }

    @Test void isolatesLegacyParserFailure() {
        var node = node(53, 53);
        when(legacy.parseNodeTxt(node)).thenThrow(new IllegalArgumentException("test"));
        var result = service.compare(53, "token");
        assertThat(result.legacy().failed()).isTrue();
        assertThat(result.experimental().failed()).isFalse();
    }

    @Test void isolatesExperimentalParserFailure() {
        var node = node(53, 53);
        when(node.getTxt()).thenReturn("<unexpected/>");
        var result = service.compare(53, "token");
        assertThat(result.legacy().failed()).isFalse();
        assertThat(result.experimental().failed()).isTrue();
    }

    @Test void rejectsMissingNodeAndNonRichContent() {
        assertThatThrownBy(() -> service.compare(99, "token")).isInstanceOf(ResponseStatusException.class);
        var node = node(53, 53);
        when(node.getSyntax()).thenReturn("plain-text");
        assertThatThrownBy(() -> service.compare(53, "token")).isInstanceOf(ResponseStatusException.class);
    }

    @Test void directVisitRedirectsBeforeReadingDatabase() {
        var service = mock(RichTextPreviewService.class);
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN, "token");
        assertThat(new RichTextPreviewController(service).preview(53, request, new ExtendedModelMap()))
                .isEqualTo("redirect:/nodes/richtext-preview/53?_tenantView=token");
        verifyNoInteractions(service);
    }

    @Test void controllerSuppliesPreviewAndToken() {
        var service = mock(RichTextPreviewService.class);
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN, "token");
        request.setParameter("_tenantView", "token");
        var model = new ExtendedModelMap();
        assertThat(new RichTextPreviewController(service).preview(53, request, model)).isEqualTo("node/richtext-preview");
        assertThat(model.get("tenantViewToken")).isEqualTo("token");
        verify(service).compare(53, "token");
    }

    @Test void controllerWithoutSessionReturnsHome() {
        var service = mock(RichTextPreviewService.class);
        assertThat(new RichTextPreviewController(service).preview(53, new MockHttpServletRequest(), new ExtendedModelMap()))
                .isEqualTo("redirect:/");
        verifyNoInteractions(service);
    }
    @Test void previewKeepsNewlinesAndRepeatedSpacesInBothPanes() {
        var node = node(53, 53);
        when(node.getTxt()).thenReturn("<node><rich_text>first\n\n  second\nthird</rich_text></node>");
        when(legacy.parseNodeTxt(node)).thenReturn("<span>first\n\n  second\nthird</span>");
        var result = service.compare(53, "token");
        for (var pane : List.of(result.legacy(), result.experimental())) {
            assertThat(pane.failed()).isFalse();
            assertThat(pane.html()).contains("first\n\n  second\nthird", "white-space:pre-wrap");
        }
    }

}

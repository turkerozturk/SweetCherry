package com.turkerozturk.richtext.experimental;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.turkerozturk.node.Node;
import com.turkerozturk.image.ImageService;

class RichTextRenderingServiceTest {
    private final ImageService images = mock(ImageService.class);
    private final RichTextRenderingService service = new RichTextRenderingService(images);
    private Node node(String xml) {
        var node = mock(Node.class);
        when(node.getNodeId()).thenReturn(53L);
        when(node.getTxt()).thenReturn(xml);
        when(node.getCodeBoxes()).thenReturn(Set.of());
        when(node.getGrids()).thenReturn(Set.of());
        when(images.getImagesByNodeId(53L)).thenReturn(List.of());
        return node;
    }

    @Test void retainsFormattingAndLocalAnchorInLiveContent() {
        var node = node("<node><rich_text link='node 53 anchor' weight='heavy'>Here</rich_text></node>");
        assertThat(service.renderLive(node, "token")).contains("href=\"#anchor\"", "font-weight:bold").doesNotContain("target=\"_blank\"");
    }

    @Test void preservesDifferentNodeNavigationAndTenantToken() {
        var node = node("<node><rich_text link='node 25'>Other</rich_text></node>");
        assertThat(service.renderLive(node, "token")).contains("/nodes/25?_tenantView=token", "target=\"_blank\"");
    }

    @Test void keepsImageEndpointForExistingZoomAndTenantGuard() {
        var node = node("<node><rich_text>AB</rich_text></node>");
        var image = mock(com.turkerozturk.image.Image.class);
        when(image.getNodeId()).thenReturn(53L);
        when(image.getOffset()).thenReturn(1);
        when(image.getPng()).thenReturn(new byte[]{(byte)137,80,78,71,13,10,26,10});
        when(images.getImagesByNodeId(53L)).thenReturn(List.of(image));
        assertThat(service.renderLive(node, "token")).contains("/images/53/1?_tenantView=token", "max-width:100%").doesNotContain("data:image/png");
    }
}

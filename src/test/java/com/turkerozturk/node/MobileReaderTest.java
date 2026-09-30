package com.turkerozturk.node;

import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.multipledatabases.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import java.util.List;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MobileReaderTest {
    @AfterEach void clearTenant() { TenantContext.clear(); }

    /** The virtual root opens the optional reader without needing a real node row. */
    @Test void readerUsesExistingRootPermissionsAndChildren() {
        NodeController controller = controller();
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.getRootNodesAsHtml(model, new MockHttpServletRequest(), Locale.ENGLISH, null, "reader"))
                .isEqualTo("node/mobileReader");
        assertThat(model.get("isRootNode")).isEqualTo(true);
        assertThat(model.get("canDeleteNode")).isEqualTo(false);
        assertThat(model.get("childNodes")).isEqualTo(List.of());
    }

    @Test void legacyMobileRemainsAvailable() {
        assertThat(controller().getRootNodesAsHtml(new ExtendedModelMap(), new MockHttpServletRequest(), Locale.ENGLISH, null, "mobile"))
                .isEqualTo("node/nodeMobile");
    }

    private NodeController controller() {
        TenantContext.setCurrentTenant("Demo");
        NodeController controller = new NodeController();
        ReflectionTestUtils.setField(controller, "myappIsDebugEnabled", false);
        ChildrenService children = mock(ChildrenService.class);
        when(children.getNaviNodesByFatherId(0)).thenReturn(List.of());
        ReflectionTestUtils.setField(controller, "childrenService", children);
        ReflectionTestUtils.setField(controller, "nodeDeletionService", mock(NodeDeletionService.class));
        return controller;
    }
}

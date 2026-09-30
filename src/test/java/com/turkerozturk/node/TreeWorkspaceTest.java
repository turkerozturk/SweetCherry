package com.turkerozturk.node;

import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.multipledatabases.TenantRequiredInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.method.HandlerMethod;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TreeWorkspaceTest {
    private final NodeController controller = new NodeController();
    private final TenantRequiredInterceptor guard = new TenantRequiredInterceptor();

    @AfterEach
    void clearTenant() { TenantContext.clear(); }

    @Test
    void workspaceRequiresDatabaseAndFragmentRejectsOldViewToken() throws Exception {
        HandlerMethod shell = new HandlerMethod(controller, NodeController.class.getMethod(
                "treeWorkspace", org.springframework.ui.Model.class, long.class));
        assertThat(guard.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), shell)).isFalse();
        TenantContext.setCurrentTenant("New database");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN, "new-token");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tree/content/55");
        request.setSession(session);
        request.setParameter("_tenantView", "old-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod fragment = new HandlerMethod(controller, NodeController.class.getMethod(
                "treeContent", long.class, org.springframework.ui.Model.class, jakarta.servlet.http.HttpServletRequest.class));
        assertThat(guard.preHandle(request, response, fragment)).isFalse();
        assertThat(response.getStatus()).isEqualTo(409);
    }

    @Test
    void virtualRootDoesNotRequireARealNodeAndUsesTenantWritableFlag() {
        NodeDeletionService deletion = mock(NodeDeletionService.class);
        when(deletion.isCurrentTenantWritable()).thenReturn(true);
        ReflectionTestUtils.setField(controller, "nodeDeletionService", deletion);
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.treeContent(0, model, new MockHttpServletRequest()))
                .isEqualTo("node/treeContentFragment :: treeContent");
        assertThat(model.get("isRootNode")).isEqualTo(true);
        assertThat(model.get("canDeleteNode")).isEqualTo(true);
        assertThat(model.containsKey("node")).isFalse();
    }

    @Test
    void nodeNavigationKeepsTheRequestedSharedReferenceIdInTreeMode() {
        assertThat(controller.getNodeAsHtml(55, new ExtendedModelMap(), new MockHttpServletRequest(), "tree"))
                .isEqualTo("redirect:/tree?nodeId=55");
    }
}

package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseSwitchControllerTest {
    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test void validatesSourceAndRotatesViewForEverySelection() {
        TenantService service = mock(TenantService.class);
        when(service.getAllTenants()).thenReturn(Map.of("Demo", mock(DataSource.class)));
        DatabaseSwitchController controller = new DatabaseSwitchController();
        ReflectionTestUtils.setField(controller, "tenantService", service);
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(controller.setTenant("Unknown", request)).isEqualTo("redirect:/");
        assertThat(request.getSession().getAttribute("dataSourceOperationError")).isEqualTo("missing");
        assertThat(request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT)).isNull();
        controller.setTenant("Demo", request);
        Object first = request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN);
        controller.setTenant("Demo", request);
        Object second = request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN);
        assertThat(first).isNotNull().isNotEqualTo(second);
    }
    @Test void removedSelectionPreservesAnotherValidSourceAndGetNeverChangesIt() {
        var service=mock(TenantService.class);
        when(service.getAllTenants()).thenReturn(Map.of("Demo",mock(DataSource.class)));
        var controller=new DatabaseSwitchController(); ReflectionTestUtils.setField(controller,"tenantService",service);
        var request=new MockHttpServletRequest(); controller.setTenant("Demo",request);
        Object token=request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN);
        controller.setTenant("Deleted",request);
        assertThat(request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT)).isEqualTo("Demo");
        assertThat(request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN)).isEqualTo(token);
        assertThat(controller.selectionPage()).isEqualTo("redirect:/");
    }
    @Test void schemaProblemPreservesSelectionAndExposesOnlySafeInstructions() throws Exception {
        var service = mock(TenantService.class);
        var source = mock(ManagedTenantDataSource.class);
        when(service.getAllTenants()).thenReturn(Map.of("Old", source));
        when(service.getCustomProperties("Old")).thenReturn(Map.of("propertyFileName", "old.txt", "datasource.password", "secret"));
        when(source.activate(false)).thenThrow(new CtbSchemaCompatibility.Problem(java.util.List.of("children.master_id"), true));
        var controller = new DatabaseSwitchController(); ReflectionTestUtils.setField(controller, "tenantService", service);
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT, "Previous");
        request.getSession().setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN, "previous-token");
        assertThat(controller.setTenant("Old", request)).isEqualTo("redirect:/");
        assertThat(request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT)).isEqualTo("Previous");
        assertThat(request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN)).isEqualTo("previous-token");
        assertThat(request.getSession().getAttribute("dataSourceOperationError")).isEqualTo("schema");
        assertThat(request.getSession().getAttribute("ctbSchemaConfig")).isEqualTo("old.txt");
        assertThat(request.getSession().getAttribute("ctbSchemaSql")).isEqualTo(CtbSchemaCompatibility.UPGRADE_SQL);
    }

}

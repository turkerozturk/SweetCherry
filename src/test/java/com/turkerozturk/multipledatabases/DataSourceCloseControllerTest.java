package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataSourceCloseControllerTest {
    @AfterEach void clear() { TenantContext.clear(); }
    @Test void closesPoolAndClearsSelectionWithoutLoggingOut() {
        var pool = mock(ManagedTenantDataSource.class);
        var service = mock(TenantService.class);
        when(service.getAllTenants()).thenReturn(Map.of("Demo", pool));
        var controller = new DataSourceCloseController();
        ReflectionTestUtils.setField(controller, "tenantService", service);
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT, "Demo");
        request.getSession().setAttribute("loginMarker", "retained");
        controller.closeDataSources(request);
        verify(pool).close();
        assertThat(request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT)).isNull();
        assertThat(request.getSession().getAttribute("loginMarker")).isEqualTo("retained");
    }
    @Test void busyPoolPreservesSelectionAndReportsWarning() {
        var pool = mock(ManagedTenantDataSource.class);
        doThrow(new IllegalStateException("busy")).when(pool).close();
        var service = mock(TenantService.class);
        when(service.getAllTenants()).thenReturn(Map.of("Demo", pool));
        var controller = new DataSourceCloseController();
        ReflectionTestUtils.setField(controller, "tenantService", service);
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT, "Demo");
        controller.closeDataSources(request);
        assertThat(request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT)).isEqualTo("Demo");
        assertThat(request.getSession().getAttribute("dataSourceOperationError")).isEqualTo("busy");
    }
}

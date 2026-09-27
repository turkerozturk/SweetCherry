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

        assertThatThrownBy(() -> controller.setTenant("Unknown", request))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(request.getSession(false)).isNull();
        controller.setTenant("Demo", request);
        Object first = request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN);
        controller.setTenant("Demo", request);
        Object second = request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN);
        assertThat(first).isNotNull().isNotEqualTo(second);
    }
}

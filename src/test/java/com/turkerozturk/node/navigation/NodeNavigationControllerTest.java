package com.turkerozturk.node.navigation;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

class NodeNavigationControllerTest {
    @Test void returnsContextRelativeUrlsForEachViewAndKeepsSharedOccurrenceId() throws Exception {
        var service=mock(NodeNavigationService.class);when(service.exists(11)).thenReturn(true);
        var mvc=MockMvcBuilders.standaloneSetup(new NodeNavigationController(service)).build();
        for(String mode:new String[]{"tree","reader","desktop","mobile"}) {
            String url=(mode.equals("tree")||mode.equals("desktop"))?"/app/tree?nodeId=11&_tenantView=current":"/app/nodes/11?_tenantView=current";
            mvc.perform(get("/app/nodes/go-to").contextPath("/app").param("nodeId","11").param("_tenantView","current")
                    .cookie(new Cookie("viewMode",mode))).andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.url").value(url));
        }
    }
    @Test void malformedZeroNegativeOrOverflowingIdsNeverQueryDatabase() throws Exception {
        var service=mock(NodeNavigationService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new NodeNavigationController(service)).build();
        for(String id:new String[]{"m","-1","0","01","1.5","9223372036854775808","","1 OR 1=1"})
            mvc.perform(get("/nodes/go-to").param("nodeId",id).param("_tenantView","current"))
                    .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void missingNodeReturns404WithoutErrorDispatchOrRedirect() throws Exception {
        var service=mock(NodeNavigationService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new NodeNavigationController(service)).build();
        var result=mvc.perform(get("/nodes/go-to").param("nodeId","999").param("_tenantView","current"))
                .andExpect(status().isNotFound()).andReturn();
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getErrorMessage()).isNull();
        verify(service).exists(999);
    }
}

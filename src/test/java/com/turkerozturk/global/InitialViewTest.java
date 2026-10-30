package com.turkerozturk.global;

import com.turkerozturk.node.navigation.NodeNavigationController;
import com.turkerozturk.node.navigation.NodeNavigationService;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.Cookie;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InitialViewTest {
    @Test void defaultsToTreeButPreservesReaderAndHistoricalMobileChoice() throws Exception {
        assertThat(ViewController.normalizeMode(null)).isEqualTo("tree");
        var service = mock(NodeNavigationService.class); when(service.exists(11)).thenReturn(true);
        var mvc = MockMvcBuilders.standaloneSetup(new NodeNavigationController(service)).build();
        mvc.perform(get("/nodes/go-to").param("nodeId", "11").param("_tenantView", "current"))
                .andExpect(jsonPath("$.url").value("/tree?nodeId=11&_tenantView=current"));
        for (String mode : new String[]{"reader", "mobile"})
            mvc.perform(get("/nodes/go-to").param("nodeId", "11").param("_tenantView", "current")
                    .cookie(new Cookie("viewMode", mode)))
                    .andExpect(jsonPath("$.url").value("/nodes/11?_tenantView=current"));
    }
}

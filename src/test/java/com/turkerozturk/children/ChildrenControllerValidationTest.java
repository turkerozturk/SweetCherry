package com.turkerozturk.children;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChildrenControllerValidationTest {
    @Test void invalidParentIdsNeverReachDatabase() throws Exception {
        var service = mock(ChildrenService.class);
        var controller = new ChildrenController();
        ReflectionTestUtils.setField(controller,"childrenService",service);
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setSingleView(new org.springframework.web.servlet.view.json.MappingJackson2JsonView())
                .build();
        for (String id : List.of("m","-1","1.5","9223372036854775808")) {
            mvc.perform(get("/father").param("fatherId",id)).andExpect(status().isOk())
                    .andExpect(view().name("father"))
                    .andExpect(model().attribute("invalidFatherId",true))
                    .andExpect(model().attribute("nodes",List.of()));
        }
        verifyNoInteractions(service);
    }
    @Test void zeroMissingAndValidParentIdsRemainSupported() throws Exception {
        var service = mock(ChildrenService.class);
        var controller = new ChildrenController();
        ReflectionTestUtils.setField(controller,"childrenService",service);
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setSingleView(new org.springframework.web.servlet.view.json.MappingJackson2JsonView())
                .build();
        mvc.perform(get("/father")).andExpect(status().isOk());
        mvc.perform(get("/father").param("fatherId","0")).andExpect(status().isOk());
        mvc.perform(get("/father").param("fatherId","12")).andExpect(status().isOk());
        verify(service,times(2)).getNaviNodesByFatherId(0L);
        verify(service).getNaviNodesByFatherId(12L);
    }
}

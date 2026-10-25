package com.turkerozturk.node.moving;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

class NodeMoveToControllerTest {
    @Test void acceptsChosenParentAndRootWithoutRedirecting() throws Exception {
        var service=mock(NodeMoveService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new NodeMoveController(service)).build();
        for(String parent:new String[]{"9","0"})
            mvc.perform(post("/nodes/move/2/to").param("parentId",parent).param("revision","snapshot"))
                .andExpect(status().isNoContent());
        verify(service).moveTo(2,9,"snapshot");verify(service).moveTo(2,0,"snapshot");
    }
    @Test void malformedOrOverflowingParentNeverReachesService() throws Exception {
        var service=mock(NodeMoveService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new NodeMoveController(service)).build();
        for(String parent:new String[]{"m","-1","1.5","9223372036854775808","","1 OR 1=1"})
            mvc.perform(post("/nodes/move/2/to").param("parentId",parent).param("revision","snapshot"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void expectedTreeErrorsReturnStatusWithoutServletErrorDispatch() throws Exception {
        var service=mock(NodeMoveService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new NodeMoveController(service)).build();
        for(var status:java.util.List.of(org.springframework.http.HttpStatus.NOT_FOUND,org.springframework.http.HttpStatus.CONFLICT,org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)) {
            doThrow(new org.springframework.web.server.ResponseStatusException(status)).when(service).moveTo(2,9,"snapshot");
            var result=mvc.perform(post("/nodes/move/2/to").param("parentId","9").param("revision","snapshot"))
                .andExpect(status().is(status.value())).andReturn();
            org.assertj.core.api.Assertions.assertThat(result.getResponse().getErrorMessage()).isNull();
        }
    }

}

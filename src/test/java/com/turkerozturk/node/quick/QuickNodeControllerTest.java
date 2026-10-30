package com.turkerozturk.node.quick;

import com.turkerozturk.editor.EditorRecoveryContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuickNodeControllerTest {
 @Test void validatesIdsBeforeServiceAndReturnsExactLongIdAsString() throws Exception {
  var service=mock(QuickNodeService.class);var scope=mock(EditorRecoveryContext.class);
  var mvc=MockMvcBuilders.standaloneSetup(new QuickNodeController(service,scope)).build();
  for(String id:new String[]{"","-1","01","m","9223372036854775808","1.0"})
   mvc.perform(post("/nodes/quick/create").param("_tenantView","v").param("parentId",id).param("title","T"))
    .andExpect(status().isBadRequest());
  verifyNoInteractions(service);
  when(service.create(0,"T","","plain-text")).thenReturn(Long.MAX_VALUE);
  mvc.perform(post("/nodes/quick/create").param("_tenantView","v").param("parentId","0").param("title","T"))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
   .andExpect(jsonPath("$.id").value("9223372036854775807"));
 }
 @Test void optionsExposeOnlyWritableFlagAndOpaqueScope() throws Exception {
  var service=mock(QuickNodeService.class);var scope=mock(EditorRecoveryContext.class);
  when(service.writable()).thenReturn(true);when(scope.scope(any())).thenReturn("a".repeat(64));
  var mvc=MockMvcBuilders.standaloneSetup(new QuickNodeController(service,scope)).build();
  mvc.perform(get("/nodes/quick/options").param("_tenantView","v"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.writable").value(true))
   .andExpect(jsonPath("$.scope").value("a".repeat(64))).andExpect(jsonPath("$.password").doesNotExist());
 }
}

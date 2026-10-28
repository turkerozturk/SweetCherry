package com.turkerozturk.richtext.editing;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
class InternalRichTextLinkControllerTest {
 @Test void validatesIdsAndNamesBeforeDatabaseAndReturnsNotFoundWithoutErrorDispatch()throws Exception {
  var service=mock(InternalRichTextTargets.class);var mvc=MockMvcBuilders.standaloneSetup(new InternalRichTextLinkController(service)).build();
  for(String id:java.util.List.of("","0","-2","abc","9223372036854775808"))mvc.perform(get("/nodes/richtext/links/target").param("nodeId",id).param("_tenantView","v")).andExpect(status().isBadRequest());
  mvc.perform(get("/nodes/richtext/links/matches").param("name"," ").param("_tenantView","v")).andExpect(status().isBadRequest());
  verifyNoInteractions(service);
  mvc.perform(get("/nodes/richtext/links/target").param("nodeId","11").param("_tenantView","v")).andExpect(status().isNotFound()).andExpect(header().string("Cache-Control","no-store"));
  verify(service).target(11);
 }
 @Test void returnsMetadataWithStringIdsAndRequiresViewToken()throws Exception {
  var service=mock(InternalRichTextTargets.class);var mvc=MockMvcBuilders.standaloneSetup(new InternalRichTextLinkController(service)).build();
  when(service.target(11)).thenReturn(new InternalRichTextTargets.Target("11","<Name>","ct_code",true,"#000000",java.util.List.of("a")));
  mvc.perform(get("/nodes/richtext/links/target").param("nodeId","11").param("_tenantView","v")).andExpect(status().isOk()).andExpect(jsonPath("$.nodeId").value("11")).andExpect(jsonPath("$.anchors[0]").value("a"));
  mvc.perform(get("/nodes/richtext/links/target").param("nodeId","11")).andExpect(status().isBadRequest());
 }
}

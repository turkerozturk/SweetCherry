package com.turkerozturk.markdown;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.contentediting.NodeContentEditingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class MarkdownControllerTest {
 @Test void previewIsReadOnlyAndRequiresAnEditableMarkdownNode()throws Exception{
  var service=mock(NodeContentEditingService.class);var node=new Node();node.setSyntax("markdown");when(service.editableNode(1)).thenReturn(node);
  var mvc=MockMvcBuilders.standaloneSetup(new MarkdownController(service)).build();mvc.perform(post("/nodes/markdown/1/preview").param("text","# Title").param("_tenantView","v")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.html").value(org.hamcrest.Matchers.containsString("<h1")));
  verify(service).editableNode(1);verify(service,never()).update(anyLong(),anyString());node.setSyntax("java");mvc.perform(post("/nodes/markdown/1/preview").param("text","# Title").param("_tenantView","v")).andExpect(status().isBadRequest());
 }
}

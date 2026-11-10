package com.turkerozturk.html;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.contentediting.NodeContentEditingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class HtmlControllerTest {
 @Test void previewIsReadOnlyAndRequiresAnEditableHtmlNode()throws Exception{
  var service=mock(NodeContentEditingService.class);var node=new Node();node.setSyntax("html");when(service.editableNode(1)).thenReturn(node);
  var mvc=MockMvcBuilders.standaloneSetup(new HtmlController(service)).build();mvc.perform(post("/nodes/html/1/preview").param("text","<h1>Title</h1>").param("_tenantView","v")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.document").value(org.hamcrest.Matchers.containsString("<h1")));
  verify(service).editableNode(1);verify(service,never()).update(anyLong(),anyString());node.setSyntax("java");mvc.perform(post("/nodes/html/1/preview").param("text","<h1>Title</h1>").param("_tenantView","v")).andExpect(status().isBadRequest());
 }
}

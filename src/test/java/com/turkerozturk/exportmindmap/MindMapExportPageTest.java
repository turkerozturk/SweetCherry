package com.turkerozturk.exportmindmap;

import com.turkerozturk.children.*;
import com.turkerozturk.node.*;
import com.turkerozturk.multipledatabases.*;
import org.junit.jupiter.api.*;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MindMapExportPageTest {
    @AfterEach void clear() {TenantContext.clear();}
    @Test void noTenantRedirectsBeforeRepositoriesAreQueried() throws Exception {
        var children=mock(ChildrenRepository.class); var nodes=mock(NodeRepository.class);
        var mvc=MockMvcBuilders.standaloneSetup(new MindMapExportController(children,nodes)).addInterceptors(new TenantRequiredInterceptor()).build();
        mvc.perform(get("/mindmap-export")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        verifyNoInteractions(children,nodes);
    }
    @Test void aliasPageAndDownloadUseOccurrenceIdAndNeverExpandMasterChildren() throws Exception {
        var children=mock(ChildrenRepository.class); var nodes=mock(NodeRepository.class);
        var alias=new Children().setNodeId(12).setFatherId(0); alias.setMasterId(1L);
        var master=new Node(); master.setNodeId(1); master.setName("Master");
        when(children.findByNodeId(12L)).thenReturn(alias); when(nodes.findById(1L)).thenReturn(master);
        var controller=new MindMapExportController(children,nodes); var model=new ExtendedModelMap();
        assertThat(controller.showExportPage(12L,model)).isEqualTo("mindmap-export");
        assertThat(model.get("nodeId")).isEqualTo(12L); assertThat(model.get("rootName")).isEqualTo("Master");
        String xml=new String(controller.exportMindMap(12,null,"all-unfolded",false,true).getBody(),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(xml).contains("ID_12", "TEXT=\"Master\"");
        verify(children,never()).findByFatherIdOrderBySequenceAsc(anyLong());
    }
    @Test void pageWithoutIdAllowsManualSelection() {
        var children=mock(ChildrenRepository.class); var nodes=mock(NodeRepository.class);
        var model=new ExtendedModelMap(); new MindMapExportController(children,nodes).showExportPage(null,model);
        assertThat(model.get("nodeId")).isNull(); verifyNoInteractions(children,nodes);
    }
}

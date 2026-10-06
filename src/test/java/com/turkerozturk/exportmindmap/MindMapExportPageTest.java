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

    @Test void wholeDatabaseHasNamedRootAndPreservesSharedOccurrence() throws Exception {
        TenantContext.setCurrentTenant("Demo & Notes");
        var children=mock(ChildrenRepository.class); var nodes=mock(NodeRepository.class);
        var first=new Children().setNodeId(1).setFatherId(0);
        var alias=new Children().setNodeId(12).setFatherId(0); alias.setMasterId(1L);
        var node=new Node(); node.setNodeId(1); node.setName("First");
        when(children.findByFatherIdOrderBySequenceAsc(0L)).thenReturn(java.util.List.of(first,alias));
        when(children.findByFatherIdOrderBySequenceAsc(1L)).thenReturn(java.util.List.of());
        when(nodes.findById(1L)).thenReturn(node);
        var response=new MindMapExportController(children,nodes).exportMindMap(0,null,"all-unfolded",false,true);
        var document=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(response.getBody()));
        var root=(org.w3c.dom.Element)document.getElementsByTagName("node").item(0);
        assertThat(root.getAttribute("TEXT")).isEqualTo("Demo & Notes");
        assertThat(root.getAttribute("ID")).isEqualTo("ID_0");
        assertThat(document.getElementsByTagName("node").getLength()).isEqualTo(3);
        assertThat(new String(response.getBody(),java.nio.charset.StandardCharsets.UTF_8)).contains("ID_12");
        verify(children,never()).findByNodeId(0L);
        verify(children,never()).findByFatherIdOrderBySequenceAsc(12L);
    }

    @Test void blankIdPostExportsEmptyDatabaseAtLevelZero() throws Exception {
        TenantContext.setCurrentTenant("Empty Database");
        var children=mock(ChildrenRepository.class); var nodes=mock(NodeRepository.class);
        var mvc=MockMvcBuilders.standaloneSetup(new MindMapExportController(children,nodes)).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/mindmap-export")
                .param("nodeId", "").param("level", "0"))
                .andExpect(status().isOk()).andExpect(content().contentType("application/x-freemind"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TEXT=\"Empty Database\"")));
        verifyNoInteractions(children,nodes);
    }

    @Test void iconDownloadContainsMapAndRealPngResourcesBesideIt() throws Exception {
        TenantContext.setCurrentTenant("Icons");
        var children=mock(ChildrenRepository.class); var nodes=mock(NodeRepository.class);
        var response=new MindMapExportController(children,nodes).exportMindMap(0,0,"all-unfolded",true,true);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/zip");
        assertThat(response.getHeaders().getContentDisposition().getFilename()).endsWith(".zip");
        var entries=new java.util.HashMap<String,byte[]>();
        try(var zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(response.getBody()))) {
            java.util.zip.ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) entries.put(entry.getName(),zip.readAllBytes());
        }
        assertThat(entries).containsKeys("Icons-0.mm","ctbicons/readme.txt","ctbicons/cherry_red.png");
        assertThat(new String(entries.get("ctbicons/readme.txt"),java.nio.charset.StandardCharsets.UTF_8))
                .contains("https://github.com/giuspen/cherrytree", "converted and resized", "missing");
        assertThat(entries.get("ctbicons/cherry_red.png")).startsWith(new byte[]{(byte)137,80,78,71});
    }

    @Test void invalidNodeOrLevelNeverQueriesRepositories() {
        var children=mock(ChildrenRepository.class); var nodes=mock(NodeRepository.class);
        var controller=new MindMapExportController(children,nodes);
        assertThatThrownBy(()->controller.exportMindMap(-1,null,"chat",false,true))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->controller.exportMindMap(0,-1,"chat",false,true))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(children,nodes);
    }

    @Test void referenceModeReturnsOnlyMapWithFreeplaneRecognizableStart() throws Exception {
        var children=mock(ChildrenRepository.class); var nodes=mock(NodeRepository.class);
        var occurrence=new Children().setNodeId(1).setFatherId(0);
        var node=mock(Node.class);
        when(node.getName()).thenReturn("Icon node");
        when(node.getNodeIcon()).thenReturn(java.util.Arrays.stream(com.turkerozturk.helpers.NodeIcon.values())
                .filter(icon -> icon.getIconName()!=null && !"zero".equals(icon.getIconName())).findFirst().orElseThrow());
        when(children.findByNodeId(1L)).thenReturn(occurrence);
        when(children.findByFatherIdOrderBySequenceAsc(1L)).thenReturn(java.util.List.of());
        when(nodes.findById(1L)).thenReturn(node);
        var controller=new MindMapExportController(children,nodes);
        var response=controller.exportMindMap(1,null,"chat",false,true,"references");
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/x-freemind");
        assertThat(response.getHeaders().getContentDisposition().getFilename()).endsWith(".mm");
        String xml=new String(response.getBody(),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(xml).startsWith("<map version=\"freeplane ").contains("URI=\"ctbicons/", "Exported by SweetCherry");
        javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(response.getBody()));
        String noIcons=new String(controller.exportMindMap(1,null,"chat",true,true,"none").getBody(),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(noIcons).doesNotContain("URI=\"ctbicons/");
    }

    @Test void unknownIconModeIsRejectedBeforeQueryingDatabase() {
        var children=mock(ChildrenRepository.class); var nodes=mock(NodeRepository.class);
        assertThatThrownBy(()->new MindMapExportController(children,nodes)
                .exportMindMap(0,null,"chat",false,true,"invalid"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(children,nodes);
    }
}

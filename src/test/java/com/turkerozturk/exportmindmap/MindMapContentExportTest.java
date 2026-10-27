package com.turkerozturk.exportmindmap;

import com.turkerozturk.children.*;
import com.turkerozturk.node.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MindMapContentExportTest {
    @Test void defaultsToTitlesWithoutReadingObjectsAndCheckboxCreatesEscapedNoteForSharedMaster() throws Exception {
        var children=mock(ChildrenRepository.class);var nodes=mock(NodeRepository.class);var text=mock(MindMapTextContent.class);
        var shared=new Children().setNodeId(11).setFatherId(0);shared.setMasterId(2L);
        var master=new Node();master.setNodeId(2);master.setName("Master");master.setSyntax("plain-text");master.setTxt("raw");master.setIsRichText((0x3584E4L<<3)|1);master.parseNodeTitleColorAndBoldnessAndTextType();
        when(children.findByNodeId(11L)).thenReturn(shared);when(nodes.findById(2L)).thenReturn(master);
        when(text.text(2,"plain-text","raw")).thenReturn("Türkçe 😀\n<script>literal</script> & text\nWeb (https://example.org)");
        var controller=new MindMapExportController(children,nodes);ReflectionTestUtils.setField(controller,"textContent",text);
        var mvc=MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/mindmap-export").param("nodeId","11"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("richcontent"))));
        verifyNoInteractions(text);
        var response=mvc.perform(post("/mindmap-export").param("nodeId","11").param("includeContent","true").param("iconMode","references"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        var doc=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.ByteArrayInputStream(response));
        assertThat(((org.w3c.dom.Element)doc.getElementsByTagName("node").item(0)).getAttribute("ID")).isEqualTo("ID_11");
        assertThat(((org.w3c.dom.Element)doc.getElementsByTagName("richcontent").item(0)).getAttribute("TYPE")).isEqualTo("NOTE");
        assertThat(((org.w3c.dom.Element)doc.getElementsByTagName("node").item(0)).getAttribute("COLOR")).isEqualTo("#3584E4");
        assertThat(doc.getElementsByTagName("pre").item(0).getTextContent()).contains("Türkçe 😀\n<script>literal</script> & text");
        assertThat(doc.getElementsByTagName("script").getLength()).isZero();
        verify(text).text(2,"plain-text","raw");verify(children,never()).findByFatherIdOrderBySequenceAsc(2L);
    }
    @Test void wholeDatabaseAndZipKeepNotesOnlyForIncludedOccurrences() throws Exception {
        var children=mock(ChildrenRepository.class);var nodes=mock(NodeRepository.class);var text=mock(MindMapTextContent.class);
        var root=new Children().setNodeId(1).setFatherId(0);var child=new Children().setNodeId(3).setFatherId(1);
        var node=new Node();node.setNodeId(1);node.setName("Title");node.setSyntax("plain-text");node.setTxt("raw");
        when(children.findByFatherIdOrderBySequenceAsc(0L)).thenReturn(java.util.List.of(root));when(nodes.findById(1L)).thenReturn(node);
        when(children.findByFatherIdOrderBySequenceAsc(1L)).thenReturn(java.util.List.of(child));
        when(text.text(1,"plain-text","raw")).thenReturn("Note");
        var controller=new MindMapExportController(children,nodes);ReflectionTestUtils.setField(controller,"textContent",text);
        var response=controller.exportMindMap(0,1,"chat",false,true,"bundle",true);
        try(var zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(response.getBody()))) {
            java.util.zip.ZipEntry entry;boolean found=false;
            while((entry=zip.getNextEntry())!=null) if(entry.getName().endsWith(".mm")) {
                String xml=new String(zip.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                assertThat(xml).contains("TYPE=\"NOTE\"", "Note").doesNotContain("ID_3");found=true;
            }
            assertThat(found).isTrue();
        }
        verify(text).text(1,"plain-text","raw");verify(nodes,never()).findById(3L);
    }
}

package com.turkerozturk.quickmindmap;

import com.turkerozturk.multipledatabases.*;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.node.NodeRepository;
import com.turkerozturk.quickmindmapmarkmap.MarkmapQuickMindMapController;
import com.turkerozturk.exportmindmap.MindMapExportController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.junit.jupiter.api.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseIconMindMapTest {
    @AfterEach void clear(){TenantContext.clear();}
    TenantIcons icons(){var holder=new CustomPropertiesHolder();holder.addCustomProperties("Demo",Map.of(TenantIcons.PROPERTY,"cherry_blue"));TenantContext.setCurrentTenant("Demo");return new TenantIcons(holder);}
    @Test void mermaidAndMarkmapDatabaseRootsRespectIconsSwitch()throws Exception {
        var children=mock(ChildrenRepository.class);var nodes=mock(NodeRepository.class);
        var mermaid=new QuickMindMapController(children,nodes);var markmap=new MarkmapQuickMindMapController(children,nodes,new ObjectMapper());
        var icons=icons();ReflectionTestUtils.setField(mermaid,"tenantIcons",icons);ReflectionTestUtils.setField(markmap,"tenantIcons",icons);
        for(boolean enabled:List.of(true,false)) {
            var model=new ExtendedModelMap();mermaid.showAllRoots(0,enabled,false,false,model);
            var entries=(List<?>)model.get("iconNodes");assertThat(entries).hasSize(enabled?1:0);
            if(enabled)assertThat(entries.get(0).toString()).contains("ct-node-0","cherry_blue");
            model=new ExtendedModelMap();markmap.showAllRoots(0,enabled,false,false,model);
            var root=new ObjectMapper().readTree((String)model.get("markmapDataJson"));
            assertThat(root.path("payload").has("iconName")).isEqualTo(enabled);
            if(enabled)assertThat(root.path("payload").path("iconName").asText()).isEqualTo("cherry_blue");
        }
    }
    @Test void freemindRootReferencesAndBundleContainDatabaseIconOnlyWhenEnabled()throws Exception {
        var controller=new MindMapExportController(mock(ChildrenRepository.class),mock(NodeRepository.class));ReflectionTestUtils.setField(controller,"tenantIcons",icons());
        String plain=new String(controller.exportMindMap(0,0,"chat",false,false,"none").getBody(),StandardCharsets.UTF_8);
        assertThat(plain).doesNotContain("ExternalObject");
        String referenced=new String(controller.exportMindMap(0,0,"chat",true,false,"references").getBody(),StandardCharsets.UTF_8);
        assertThat(referenced).contains("ctbicons/cherry_blue.png");
        byte[] zip=controller.exportMindMap(0,0,"chat",true,false,"bundle").getBody();
        boolean found=false;try(var input=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zip))){java.util.zip.ZipEntry entry;while((entry=input.getNextEntry())!=null)if(entry.getName().equals("ctbicons/cherry_blue.png")){found=true;assertThat(input.readAllBytes()).isNotEmpty();}}
        assertThat(found).isTrue();
    }
}

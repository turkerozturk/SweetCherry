package com.turkerozturk.quickmindmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeRepository;
import com.turkerozturk.quickmindmapmarkmap.MarkmapQuickMindMapController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AllRootsMindMapTest {
    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test void bothMapsShowEveryTopLevelEntryIncludingAliasAndReturnToAllRoots() throws Exception {
        TenantContext.setCurrentTenant("Demo Database");
        ChildrenRepository children = mock(ChildrenRepository.class);
        NodeRepository nodes = mock(NodeRepository.class);
        Children real = new Children().setNodeId(10).setFatherId(0);
        Children alias = new Children().setNodeId(55).setFatherId(0);
        alias.setMasterId(10L);
        when(children.findByFatherIdOrderBySequenceAsc(0)).thenReturn(List.of(real, alias));
        when(children.findByNodeId(10L)).thenReturn(real);
        when(nodes.findById(10L)).thenReturn(displayNode());

        QuickMindMapController mermaid = new QuickMindMapController(children, nodes);
        ExtendedModelMap mermaidModel = new ExtendedModelMap();
        assertThat(mermaid.showAllRoots(1, false, false, false, mermaidModel))
                .isEqualTo("quick-mind-map");
        assertThat((String) mermaidModel.get("mermaidDefinition"))
                .contains("Demo Database", "ctb_10", "ctb_55");
        ExtendedModelMap singleMermaid = new ExtendedModelMap();
        mermaid.showQuickMindMap(10, 0, false, false, false, singleMermaid);
        assertThat(singleMermaid.get("rootParentNodeId")).isEqualTo(0L);

        MarkmapQuickMindMapController markmap = new MarkmapQuickMindMapController(
                children, nodes, new ObjectMapper());
        ExtendedModelMap markmapModel = new ExtendedModelMap();
        assertThat(markmap.showAllRoots(1, false, false, false, markmapModel))
                .isEqualTo("markmap-quick-mind-map");
        var json = new ObjectMapper().readTree((String) markmapModel.get("markmapDataJson"));
        assertThat(json.get("content").asText()).isEqualTo("Demo Database");
        assertThat(json.get("children").size()).isEqualTo(2);
        assertThat(json.get("children").get(1).get("payload").get("nodeId").asLong())
                .isEqualTo(55L);
        ExtendedModelMap singleMarkmap = new ExtendedModelMap();
        markmap.showMarkmapQuickMindMap(10, 0, false, false, false, singleMarkmap);
        assertThat(singleMarkmap.get("rootParentNodeId")).isEqualTo(0L);
    }

    private Node displayNode() {
        Node node = new Node();
        node.setName("Shared content");
        node.setTitleColorAsHtmlHex("#123456");
        return node;
    }
}

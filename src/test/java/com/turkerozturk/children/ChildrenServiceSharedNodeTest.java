package com.turkerozturk.children;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChildrenServiceSharedNodeTest {
    @Test void twoReferencesToSameMasterKeepDistinctTreeIdsAndHaveNoChildren() {
        ChildrenRepository children = mock(ChildrenRepository.class);
        NodeRepository nodes = mock(NodeRepository.class);
        ChildrenService service = new ChildrenService();
        ReflectionTestUtils.setField(service, "childrenRepository", children);
        ReflectionTestUtils.setField(service, "nodeRepository", nodes);
        Children first = new Children().setNodeId(55).setSequence(1);
        first.setMasterId(10L);
        Children second = new Children().setNodeId(56).setSequence(2);
        second.setMasterId(10L);
        when(children.findByFatherId(0L)).thenReturn(List.of(second, first));
        Node master = new Node();
        master.setNodeId(10);
        master.setName("Original");
        when(nodes.findByNodeIdIn(List.of(10L))).thenReturn(List.of(master));

        List<NaviNode> result = service.getNaviNodesByFatherId(0);

        assertThat(result).extracting(NaviNode::nodeId).containsExactly(55L, 56L);
        assertThat(result).extracting(NaviNode::name).containsExactly("Original", "Original");
        assertThat(result).allMatch(item -> !item.hasChildren());
    }
}

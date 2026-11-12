package com.turkerozturk.children;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChildrenServiceSharedNodeTest {
    @Test void twoReferencesToSameMasterKeepDistinctTreeIdsAndOwnChildren() {
        ChildrenRepository children = mock(ChildrenRepository.class);
        NodeRepository nodes = mock(NodeRepository.class);
        ChildrenService service = new ChildrenService();
        ReflectionTestUtils.setField(service, "childrenRepository", children);
        ReflectionTestUtils.setField(service, "nodeRepository", nodes);
        var bookmarks=mock(com.turkerozturk.bookmark.BookmarkRepository.class);
        ReflectionTestUtils.setField(service,"bookmarkRepository",bookmarks);
        when(bookmarks.findExistingIds(List.of(55L,56L))).thenReturn(List.of(56L));
        Children first = new Children().setNodeId(55).setSequence(1);
        first.setMasterId(10L);
        Children second = new Children().setNodeId(56).setSequence(2);
        second.setMasterId(10L);
        when(children.findByFatherId(0L)).thenReturn(List.of(second, first));
        when(children.findByFatherId(55L)).thenReturn(List.of(new Children().setNodeId(57)));
        when(children.findByFatherId(56L)).thenReturn(List.of());
        Node master = new Node();
        master.setNodeId(10);
        master.setName("Original");
        master.setSyntax("plain-text");
        master.setLevel(3);
        master.setIsReadOnly16bit(1);
        master.parseNodeIconAndReadOnlyFlag();
        when(nodes.findByNodeIdIn(List.of(10L))).thenReturn(List.of(master));

        List<NaviNode> result = service.getNaviNodesByFatherId(0);

        assertThat(result).extracting(NaviNode::nodeId).containsExactly(55L, 56L);
        assertThat(result).extracting(NaviNode::name).containsExactly("Original", "Original");
        assertThat(result).extracting(NaviNode::hasChildren).containsExactly(true, false);
        assertThat(result).extracting(NaviNode::bookmarked).containsExactly(false,true);
        assertThat(result).allMatch(NaviNode::excludedFromSearch).allMatch(NaviNode::isReadOnly);
        verify(bookmarks).findExistingIds(List.of(55L,56L));
        verifyNoMoreInteractions(bookmarks);
        verify(children, never()).findByFatherId(10L);
    }
}

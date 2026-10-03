package com.turkerozturk.bookmark;

import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.node.NodeService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BookmarkControllerTest {
    @Test void orphanBookmarkIsReportedWithoutBreakingThePageOrChangingTheDatabase() {
        BookmarkService bookmarks = mock(BookmarkService.class);
        ChildrenService children = mock(ChildrenService.class);
        NodeService nodes = mock(NodeService.class);
        BookmarkController controller = new BookmarkController();
        ReflectionTestUtils.setField(controller,"bookmarkWriteService",mock(BookmarkWriteService.class));
        ReflectionTestUtils.setField(controller, "bookmarkService", bookmarks);
        ReflectionTestUtils.setField(controller, "childrenService", children);
        ReflectionTestUtils.setField(controller, "nodeService", nodes);
        when(bookmarks.getBookmarks()).thenReturn(List.of(new Bookmark().setNodeId(55)));
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.getAllChildrenAsHtml(model, "mobile")).isEqualTo("bookmarksMobile");
        assertThat(model.get("missingBookmarkIds")).isEqualTo(List.of(55L));
        assertThat(model.get("bookmarks")).isEqualTo(List.of());
        verify(children).findById(55L);
        verifyNoInteractions(nodes);
    }
    @Test void sharedBookmarkKeepsItsOccurrenceIdAndLoadsMasterDetails() {
        var bookmarks=mock(BookmarkService.class);var children=mock(ChildrenService.class);var nodes=mock(NodeService.class);
        var writes=mock(BookmarkWriteService.class);var controller=new BookmarkController();
        ReflectionTestUtils.setField(controller,"bookmarkService",bookmarks);
        ReflectionTestUtils.setField(controller,"childrenService",children);
        ReflectionTestUtils.setField(controller,"nodeService",nodes);
        ReflectionTestUtils.setField(controller,"bookmarkWriteService",writes);
        var alias=new com.turkerozturk.children.Children().setNodeId(10);alias.setMasterId(1L);
        var bookmark=new Bookmark().setNodeId(10);var master=mock(com.turkerozturk.node.Node.class);
        when(bookmarks.getBookmarks()).thenReturn(List.of(bookmark));when(children.findById(10L)).thenReturn(alias);
        when(nodes.findById(1L)).thenReturn(master);when(writes.writable()).thenReturn(true);
        var model=new ExtendedModelMap();controller.getAllChildrenAsHtml(model,"reader");
        assertThat(model.get("canWriteBookmarks")).isEqualTo(true);
        assertThat(bookmark.getNodeId()).isEqualTo(10);assertThat(bookmark.getNode()).isSameAs(master);
    }
    @Test void eachAliasKeepsItsOwnBreadcrumbsEvenWhenTheyShareOneMasterEntity() {
        var bookmarks=mock(BookmarkService.class);var children=mock(ChildrenService.class);var nodes=mock(NodeService.class);
        var writes=mock(BookmarkWriteService.class);var controller=new BookmarkController();
        ReflectionTestUtils.setField(controller,"bookmarkService",bookmarks);
        ReflectionTestUtils.setField(controller,"childrenService",children);
        ReflectionTestUtils.setField(controller,"nodeService",nodes);
        ReflectionTestUtils.setField(controller,"bookmarkWriteService",writes);
        var root=new com.turkerozturk.children.Children().setNodeId(1);
        var parent=new com.turkerozturk.children.Children().setNodeId(2);
        var a=new com.turkerozturk.children.Children().setNodeId(10).setFatherId(2);a.setMasterId(1L);
        var b=new com.turkerozturk.children.Children().setNodeId(11);b.setMasterId(1L);
        var master=mock(com.turkerozturk.node.Node.class);var branch=mock(com.turkerozturk.node.Node.class);
        when(master.getName()).thenReturn("Master");when(branch.getName()).thenReturn("Branch");
        when(bookmarks.getBookmarks()).thenReturn(List.of(new Bookmark().setNodeId(1),new Bookmark().setNodeId(10),new Bookmark().setNodeId(11)));
        when(children.findById(1L)).thenReturn(root);when(children.findById(2L)).thenReturn(parent);
        when(children.findById(10L)).thenReturn(a);when(children.findById(11L)).thenReturn(b);
        when(nodes.findById(1L)).thenReturn(master);when(nodes.findById(2L)).thenReturn(branch);
        var model=new ExtendedModelMap();controller.getAllChildrenAsHtml(model,"tree");
        var paths=(java.util.Map<?,?>)model.get("bookmarkPaths");
        assertThat(paths.get(1L)).isEqualTo(java.util.Map.of(1L,"Master"));
        assertThat(paths.get(10L)).isEqualTo(java.util.Map.of(2L,"Branch",10L,"Master"));
        assertThat(paths.get(11L)).isEqualTo(java.util.Map.of(11L,"Master"));
        assertThat(new java.util.ArrayList<>(((java.util.Map<?,?>)paths.get(10L)).keySet())).isEqualTo(List.of(2L,10L));
        verify(master,never()).setBreadcrumbs(any());
    }
    @Test void cyclicParentDoesNotHangTheBookmarkList() {
        var bookmarks=mock(BookmarkService.class);var children=mock(ChildrenService.class);var nodes=mock(NodeService.class);
        var controller=new BookmarkController();
        ReflectionTestUtils.setField(controller,"bookmarkService",bookmarks);
        ReflectionTestUtils.setField(controller,"childrenService",children);
        ReflectionTestUtils.setField(controller,"nodeService",nodes);
        ReflectionTestUtils.setField(controller,"bookmarkWriteService",mock(BookmarkWriteService.class));
        var child=new com.turkerozturk.children.Children().setNodeId(1).setFatherId(1);
        var node=mock(com.turkerozturk.node.Node.class);when(node.getName()).thenReturn("Node");
        when(bookmarks.getBookmarks()).thenReturn(List.of(new Bookmark().setNodeId(1)));
        when(children.findById(1L)).thenReturn(child);when(nodes.findById(1L)).thenReturn(node);
        var model=new ExtendedModelMap();controller.getAllChildrenAsHtml(model,"tree");
        assertThat(((java.util.Map<?,?>)model.get("bookmarkPaths")).get(1L)).isEqualTo(java.util.Map.of(1L,"Node"));
    }
}

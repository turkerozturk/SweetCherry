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
}

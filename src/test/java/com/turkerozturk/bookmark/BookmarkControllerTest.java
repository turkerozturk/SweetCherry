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
}

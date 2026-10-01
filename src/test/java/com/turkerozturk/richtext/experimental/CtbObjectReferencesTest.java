package com.turkerozturk.richtext.experimental;

import org.junit.jupiter.api.Test;
import com.turkerozturk.image.Image;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import com.turkerozturk.richtext.experimental.RichTextLayout.ObjectKind;

class CtbObjectReferencesTest {
    @Test void classifiesImageTableRowsWithoutReadingBinaryPayload() {
        var image = mock(Image.class);
        when(image.getNodeId()).thenReturn(53L);
        when(image.getOffset()).thenReturn(4);
        var adapter = new CtbObjectReferences();
        assertThat(adapter.fromImage(image).kind()).isEqualTo(ObjectKind.IMAGE);
        when(image.getFileName()).thenReturn("notes.txt");
        assertThat(adapter.fromImage(image).kind()).isEqualTo(ObjectKind.ATTACHMENT);
        when(image.getAnchor()).thenReturn("section");
        assertThat(adapter.fromImage(image).kind()).isEqualTo(ObjectKind.ANCHOR);
        verify(image, never()).getPng();
    }
}

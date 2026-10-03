package com.turkerozturk.richtext.editing;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class RichTextObjectDeletionsTest {
    private final List<ProtectedRichTextCodec.Reference> refs = List.of(new ProtectedRichTextCodec.Reference("image", 1));
    @Test void acceptsOnlyExplicitUnlockedReferences() {
        assertThat(RichTextObjectDeletions.decode("[\"image:1\"]", true, refs)).containsExactly("image:1");
        assertThat(RichTextObjectDeletions.decode("[]", false, refs)).isEmpty();
    }
    @Test void refusesLockedDeletion() {
        assertThatThrownBy(() -> RichTextObjectDeletions.decode("[\"image:1\"]", false, refs)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void refusesUnknownDuplicateAndMalformedRequests() {
        for (String json : List.of("[\"grid:1\"]", "[\"image:1\",\"image:1\"]", "{}", "[1]", "invalid"))
            assertThatThrownBy(() -> RichTextObjectDeletions.decode(json, true, refs)).isInstanceOf(IllegalArgumentException.class);
    }
}

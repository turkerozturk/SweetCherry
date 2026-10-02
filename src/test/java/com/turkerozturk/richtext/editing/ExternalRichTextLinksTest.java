package com.turkerozturk.richtext.editing;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.turkerozturk.richtext.experimental.RichTextXmlReader;

class ExternalRichTextLinksTest {
    private final RichTextXmlReader reader = new RichTextXmlReader();
    @Test void acceptsAbsoluteHttpAndHttpsWithQueryAndFragment() {
        for (String value : java.util.List.of("webs https://example.org/a?q=1#part", "webs http://127.0.0.1:8080/", "webs https://example.org/%C3%BC")) {
            assertThatCode(() -> ExternalRichTextLinks.validate(value)).doesNotThrowAnyException();
        }
    }
    @Test void rejectsScriptFileRelativeCredentialsAndControlCharacters() {
        for (String value : java.util.List.of("webs javascript:alert(1)", "webs file:///tmp/a", "webs //example.org/", "node 1", "webs https://user:pass@example.org/", "webs https://example.org/\n", "webs https://", "webs https://example.org/" + "a".repeat(4096))) {
            assertThatThrownBy(() -> ExternalRichTextLinks.validate(value)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void preservesLegacyInternalAndUnknownLinkMetadata() {
        var original = reader.read("<node><rich_text link='node 12 anchor'>Node</rich_text><rich_text link='file /tmp/a'>File</rich_text></node>");
        assertThatCode(() -> ExternalRichTextLinks.validateChanges(original, original)).doesNotThrowAnyException();
    }
    @Test void validatesIntroducedLinksAndAllowsRemoval() {
        var original = reader.read("<node><rich_text link='webs https://example.org/'>A</rich_text></node>");
        var edited = reader.read("<node><rich_text link='webs https://example.net/'>A</rich_text></node>");
        assertThatCode(() -> ExternalRichTextLinks.validateChanges(edited, original)).doesNotThrowAnyException();
        assertThatCode(() -> ExternalRichTextLinks.validateChanges(reader.read("<node><rich_text>A</rich_text></node>"), original)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ExternalRichTextLinks.validateChanges(reader.read("<node><rich_text link='webs javascript:alert(1)'>A</rich_text></node>"), original)).isInstanceOf(IllegalArgumentException.class);
    }
}

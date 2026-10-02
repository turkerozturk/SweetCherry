package com.turkerozturk.richtext.editing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.properties.NodePropertiesService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RichTextEditingServiceTest {
    private final NodePropertiesService properties = mock(NodePropertiesService.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final Node node = mock(Node.class);
    private final RichTextEditingService service = new RichTextEditingService(properties);
    RichTextEditingServiceTest() {
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        when(properties.writable()).thenReturn(true);
        when(properties.realNode(12)).thenReturn(node);
        when(node.getSyntax()).thenReturn("plain-text");
        when(node.getTxt()).thenReturn("Text");
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(0L);
        when(query.executeUpdate()).thenReturn(1);
    }

    @Test void opensPlainTextAsTextModelWithoutWriting() {
        var editor = service.open(12);
        assertThat(editor.document().runs().get(0).text()).isEqualTo("Text");
        verify(query, never()).executeUpdate();
    }

    @Test void savesRichXmlAndTimestampWithConditionalUpdate() {
        String revision = service.open(12).revision();
        service.save(12, "<node><rich_text weight='heavy'>New</rich_text></node>", revision);
        verify(entityManager).createNativeQuery(contains("is_richtxt = (is_richtxt | 1)"));
        verify(query).setParameter(eq("timestamp"), any(Long.class));
        verify(query).executeUpdate();
    }

    @Test void rejectsReadonlyTenantAndReadonlyNode() {
        when(properties.writable()).thenReturn(false);
        assertThatThrownBy(() -> service.open(12)).isInstanceOf(AccessDeniedException.class);
        when(properties.writable()).thenReturn(true);
        when(node.getIsReadOnly16bit()).thenReturn(1L);
        assertThatThrownBy(() -> service.open(12)).isInstanceOf(AccessDeniedException.class);
    }

    @Test void rejectsEmbeddedObjectsEvenWhenFlagsAreMissing() {
        when(query.getSingleResult()).thenReturn(1L);
        assertThatThrownBy(() -> service.open(12)).isInstanceOf(ResponseStatusException.class);
        verify(query, never()).executeUpdate();
    }

    @Test void rejectsUnsupportedTypeAndAliasFromRealNodeGuard() {
        when(node.getSyntax()).thenReturn("java");
        assertThatThrownBy(() -> service.open(12)).isInstanceOf(ResponseStatusException.class);
        when(properties.realNode(12)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.open(12)).isInstanceOf(ResponseStatusException.class);
    }

    @Test void rejectsStaleRevisionAndInvalidXml() {
        String revision = service.open(12).revision();
        assertThatThrownBy(() -> service.save(12, "<node/>", "old")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.save(12, "<!DOCTYPE node><node/>", revision)).isInstanceOf(ResponseStatusException.class);
        verify(query, never()).executeUpdate();
    }

    @Test void rejectsConcurrentUpdateAndPreservesUnknownAttributes() {
        when(node.getSyntax()).thenReturn("custom-colors");
        when(node.getTxt()).thenReturn("<node><rich_text future='keep'>Text</rich_text></node>");
        var editor = service.open(12);
        assertThat(editor.document().runs().get(0).attributes()).containsEntry("future", "keep");
        when(query.executeUpdate()).thenReturn(0);
        assertThatThrownBy(() -> service.save(12, node.getTxt(), editor.revision())).isInstanceOf(ResponseStatusException.class);
    }
}

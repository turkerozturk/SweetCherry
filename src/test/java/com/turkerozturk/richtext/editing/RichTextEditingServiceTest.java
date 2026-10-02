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
    private final java.sql.Connection connection;
    RichTextEditingServiceTest() throws Exception {
        connection = java.sql.DriverManager.getConnection("jdbc:sqlite::memory:");
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE image (node_id INTEGER, offset INTEGER, justification TEXT, anchor TEXT, png BLOB, filename TEXT, link TEXT, time INTEGER)");
            statement.execute("CREATE TABLE grid (node_id INTEGER, offset INTEGER, txt TEXT)");
            statement.execute("CREATE TABLE codebox (node_id INTEGER, offset INTEGER, txt TEXT)");
        }
        var session = mock(org.hibernate.Session.class);
        when(entityManager.unwrap(org.hibernate.Session.class)).thenReturn(session);
        when(session.doReturningWork(any())).thenAnswer(call -> {
            org.hibernate.jdbc.ReturningWork<?> work = call.getArgument(0);
            return work.execute(connection);
        });
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

    @org.junit.jupiter.api.AfterEach void closeConnection() throws Exception { connection.close(); }

    private void addImage() {
        try (var statement = connection.createStatement()) {
            statement.execute("INSERT INTO image VALUES (12, 1, 'left', '', X'0102', '', '', 0)");
        } catch (java.sql.SQLException error) { throw new IllegalStateException(error); }
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

    @Test void protectsExistingObjectsEvenWhenFlagsAreMissing() {
        addImage();
        var editor = service.open(12);
        assertThat(editor.document().runs()).anySatisfy(run -> assertThat(run.attributes()).containsEntry("__sweet_object", "image:1"));
        assertThatThrownBy(() -> service.save(12, "<node><rich_text>Removed</rich_text></node>", editor.revision()))
                .isInstanceOf(ResponseStatusException.class);
        verify(query, never()).executeUpdate();
    }

    @Test void savesTextAndMovesObjectKeysThroughTemporaryOffsets() {
        addImage();
        var editor = service.open(12);
        service.save(12, "<node><rich_text>ZZT</rich_text><rich_text __sweet_object='image:1'>\uFFFC</rich_text><rich_text>ext</rich_text></node>", editor.revision());
        verify(query).setParameter("from", 1);
        verify(query).setParameter("to", -2);
        verify(query).setParameter("from", -2);
        verify(query).setParameter("to", 3);
        verify(query, times(3)).executeUpdate();
        verify(entityManager).clear();
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

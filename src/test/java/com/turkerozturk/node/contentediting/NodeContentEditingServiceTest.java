package com.turkerozturk.node.contentediting;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NodeContentEditingServiceTest {
    @Test void editsPlainAndCodeTextWithoutChangingTheSyntaxOrProperties() {
        NodePropertiesService properties = mock(NodePropertiesService.class);
        when(properties.writable()).thenReturn(true);
        Node node = new Node();
        node.setSyntax("java");
        when(properties.realNode(15L)).thenReturn(node);
        EntityManager manager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);
        NodeContentEditingService service = new NodeContentEditingService(properties);
        ReflectionTestUtils.setField(service, "entityManager", manager);

        service.update(15L, "first\nsecond <tag>");

        verify(query).setParameter("text", "first\nsecond <tag>");
        verify(query).setParameter("id", 15L);
        verify(query).setParameter(eq("timestamp"), any(Long.class));
        verify(query).executeUpdate();
    }

    @Test void refusesReadOnlyTenantNodeAndCherryTreeRichText() {
        NodePropertiesService properties = mock(NodePropertiesService.class);
        NodeContentEditingService service = new NodeContentEditingService(properties);
        assertThatThrownBy(() -> service.update(15L, "new"))
                .isInstanceOf(AccessDeniedException.class);
        when(properties.writable()).thenReturn(true);
        Node node = new Node();
        node.setSyntax("plain-text");
        node.setIsReadOnly16bit(1);
        when(properties.realNode(15L)).thenReturn(node);
        assertThatThrownBy(() -> service.update(15L, "new"))
                .isInstanceOf(AccessDeniedException.class);
        node.setIsReadOnly16bit(0);
        node.setSyntax("custom-colors");
        assertThatThrownBy(() -> service.update(15L, "<rich>"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test void refusesSharedNodeEvenWhenWritable() {
        NodePropertiesService properties = mock(NodePropertiesService.class);
        when(properties.writable()).thenReturn(true);
        when(properties.realNode(15L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        NodeContentEditingService service = new NodeContentEditingService(properties);
        assertThatThrownBy(() -> service.update(15L, "new"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test void permitsPlainTextWithoutReadOnlyFlag() {
        NodePropertiesService properties = mock(NodePropertiesService.class);
        when(properties.writable()).thenReturn(true);
        Node node = new Node();
        node.setSyntax("plain-text");
        when(properties.realNode(15L)).thenReturn(node);

        org.assertj.core.api.Assertions.assertThat(new NodeContentEditingService(properties).editableNode(15L))
                .isSameAs(node);
    }

    @Test void refusesSaveIfNodeChangedSinceOpeningEditor() {
        NodePropertiesService properties = mock(NodePropertiesService.class);
        when(properties.writable()).thenReturn(true);
        Node node = new Node();
        node.setSyntax("plain-text");
        when(properties.realNode(15L)).thenReturn(node);
        EntityManager manager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(0);
        NodeContentEditingService service = new NodeContentEditingService(properties);
        ReflectionTestUtils.setField(service, "entityManager", manager);

        assertThatThrownBy(() -> service.update(15L, "new"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }
}

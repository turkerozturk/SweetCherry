package com.turkerozturk.node.properties;

import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NodePropertiesServiceTest {
    @AfterEach void clear() { TenantContext.clear(); }

    @Test void savesSelectedBitsAndClearsIconAndReadOnly() {
        TenantContext.setCurrentTenant("test");
        CustomPropertiesHolder settings = new CustomPropertiesHolder();
        settings.addCustomProperties("test", Map.of("custom.isWritable", "true"));
        NodeRepository nodes = mock(NodeRepository.class);
        ChildrenRepository children = mock(ChildrenRepository.class);
        Children real = mock(Children.class);
        when(real.getMasterId()).thenReturn(0L);
        when(children.findByNodeId(12L)).thenReturn(real);
        Node node = new Node();
        node.setIsRichText(1L | (0x3584E4L << 3));
        node.setIsReadOnly16bit(1L | NodeIcon.CT_HOME.getIconIdIn16bit());
        when(nodes.findById(12L)).thenReturn(node);
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        NodePropertiesService service = new NodePropertiesService(nodes, children, settings);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        service.update(12L, "Yeni isim", true, TitleColor.RED, NodeIcon.OTHER, false);

        verify(query).setParameter("rich", (0xE01B24L << 3) | 7L);
        verify(query).setParameter("icon", 0L);
        verify(query).setParameter("name", "Yeni isim");
        verify(query).executeUpdate();
    }

    @Test void doesNotChangeReadOnlyCtbOrSharedNode() {
        TenantContext.setCurrentTenant("test");
        CustomPropertiesHolder settings = new CustomPropertiesHolder();
        NodePropertiesService readOnlyService = new NodePropertiesService(mock(NodeRepository.class),
                mock(ChildrenRepository.class), settings);
        assertThatThrownBy(() -> readOnlyService.update(12, "Name", false, TitleColor.NONE, NodeIcon.OTHER, false))
                .isInstanceOf(AccessDeniedException.class);
        settings.addCustomProperties("test", Map.of("custom.isWritable", "true"));
        Children shared = mock(Children.class);
        when(shared.getMasterId()).thenReturn(9L);
        ChildrenRepository children = mock(ChildrenRepository.class);
        when(children.findByNodeId(12L)).thenReturn(shared);
        NodePropertiesService writableService = new NodePropertiesService(mock(NodeRepository.class), children, settings);
        assertThatThrownBy(() -> writableService.update(12, "Name", false, TitleColor.NONE, NodeIcon.OTHER, false))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test void keepsExistingCustomColorAndRichTextFlagWhenChangingIcon() {
        TenantContext.setCurrentTenant("test");
        CustomPropertiesHolder settings = new CustomPropertiesHolder();
        settings.addCustomProperties("test", Map.of("custom.isWritable", "true"));
        NodeRepository nodes = mock(NodeRepository.class);
        ChildrenRepository children = mock(ChildrenRepository.class);
        Children real = mock(Children.class);
        when(real.getMasterId()).thenReturn(0L);
        when(children.findByNodeId(12L)).thenReturn(real);
        Node node = new Node();
        node.setSyntax("custom-colors");
        node.setIsRichText((0x123456L << 3) | 5L);
        when(nodes.findById(12L)).thenReturn(node);
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        NodePropertiesService service = new NodePropertiesService(nodes, children, settings);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        service.update(12L, "Name", false, null, NodeIcon.CT_HOME, true);

        verify(query).setParameter("rich", (0x123456L << 3) | 5L);
        verify(query).setParameter("icon", 29L);
        verify(query, never()).setParameter(eq("syntax"), any());
    }

    @Test void choosingNoColorClearsItsMarkerOnPlainTextNode() {
        TenantContext.setCurrentTenant("test");
        CustomPropertiesHolder settings = new CustomPropertiesHolder();
        settings.addCustomProperties("test", Map.of("custom.isWritable", "true"));
        NodeRepository nodes = mock(NodeRepository.class);
        ChildrenRepository children = mock(ChildrenRepository.class);
        Children real = mock(Children.class);
        when(real.getMasterId()).thenReturn(0L);
        when(children.findByNodeId(12L)).thenReturn(real);
        Node node = new Node();
        node.setIsRichText(4L); // Old no-color marker must not survive the save.
        when(nodes.findById(12L)).thenReturn(node);
        EntityManager manager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        NodePropertiesService service = new NodePropertiesService(nodes, children, settings);
        ReflectionTestUtils.setField(service, "entityManager", manager);

        service.update(12L, "Name", false, TitleColor.NONE, NodeIcon.OTHER, false);

        verify(query).setParameter("rich", 0L);
    }

    @Test void changesPlainTextToCodeWithoutChangingContentFlags() {
        Node node = new Node();
        node.setSyntax("plain-text");
        Query query = mock(Query.class);
        when(query.executeUpdate()).thenReturn(1);
        NodePropertiesService service = serviceWith(node, query);

        service.update(12L, "Name", false, TitleColor.NONE, NodeIcon.OTHER, false,
                NodeType.CODE, "java");

        verify(query).setParameter("syntax", "java");
        verify(query).setParameter("oldSyntax", "plain-text");
        verify(query).setParameter("rich", 0L);
        verify(query).executeUpdate();
    }

    @Test void changesCodeToPlainTextAndRejectsUnknownNewLanguages() {
        Node node = new Node();
        node.setSyntax("java");
        Query query = mock(Query.class);
        when(query.executeUpdate()).thenReturn(1);
        NodePropertiesService service = serviceWith(node, query);

        service.update(12L, "Name", false, TitleColor.NONE, NodeIcon.OTHER, false,
                NodeType.PLAIN_TEXT, null);
        verify(query).setParameter("syntax", "plain-text");

        assertThatThrownBy(() -> service.update(12L, "Name", false, TitleColor.NONE,
                NodeIcon.OTHER, false, NodeType.CODE, "unknown<script>"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test void lockedOrRichTextContentCannotChangeTypeEvenIfUnlockedInSameRequest() {
        Node node = new Node();
        node.setSyntax("plain-text");
        node.setIsReadOnly16bit(1);
        Query query = mock(Query.class);
        NodePropertiesService service = serviceWith(node, query);

        assertThatThrownBy(() -> service.update(12L, "Name", false, TitleColor.NONE,
                NodeIcon.OTHER, false, NodeType.CODE, "java"))
                .isInstanceOf(AccessDeniedException.class);
        verify(query, never()).executeUpdate();

        node.setIsReadOnly16bit(0);
        node.setSyntax("custom-colors");
        node.setIsRichText(1);
        assertThatThrownBy(() -> service.update(12L, "Name", false, TitleColor.NONE,
                NodeIcon.OTHER, false, NodeType.PLAIN_TEXT, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.update(12L, "Name", false, TitleColor.NONE,
                NodeIcon.OTHER, false, NodeType.RICH_TEXT, null))
                .isInstanceOf(ResponseStatusException.class);
        verify(query, never()).executeUpdate();
    }

    @Test void unknownLegacyCodeSyntaxRemainsWhenOtherPropertiesChange() {
        Node node = new Node();
        node.setSyntax("older-cherrytree-language");
        Query query = mock(Query.class);
        NodePropertiesService service = serviceWith(node, query);

        service.update(12L, "Name", false, TitleColor.NONE, NodeIcon.OTHER, false,
                NodeType.CODE, "older-cherrytree-language");

        verify(query, never()).setParameter(eq("syntax"), any());
        verify(query).executeUpdate();
    }

    @Test void concurrentReadOnlyChangePreventsTypeUpdate() {
        Node node = new Node();
        node.setSyntax("plain-text");
        Query query = mock(Query.class);
        when(query.executeUpdate()).thenReturn(0);
        NodePropertiesService service = serviceWith(node, query);

        assertThatThrownBy(() -> service.update(12L, "Name", false, TitleColor.NONE,
                NodeIcon.OTHER, false, NodeType.CODE, "java"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(org.springframework.http.HttpStatus.CONFLICT));
    }

    /** Creates a writable real-node fixture for syntax-change checks. */
    private NodePropertiesService serviceWith(Node node, Query query) {
        TenantContext.setCurrentTenant("test");
        CustomPropertiesHolder settings = new CustomPropertiesHolder();
        settings.addCustomProperties("test", Map.of("custom.isWritable", "true"));
        NodeRepository nodes = mock(NodeRepository.class);
        when(nodes.findById(12L)).thenReturn(node);
        ChildrenRepository children = mock(ChildrenRepository.class);
        Children real = mock(Children.class);
        when(real.getMasterId()).thenReturn(0L);
        when(children.findByNodeId(12L)).thenReturn(real);
        EntityManager manager = mock(EntityManager.class);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        NodePropertiesService service = new NodePropertiesService(nodes, children, settings);
        ReflectionTestUtils.setField(service, "entityManager", manager);
        return service;
    }
}

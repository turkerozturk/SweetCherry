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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NodePropertiesServiceTest {
    @AfterEach void clear() { TenantContext.clear(); }

    @Test void savesOnlySelectedBitsAndKeepsRichTextAndUnknownIconBits() {
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
        node.setIsReadOnly16bit(0x8001L | NodeIcon.CT_HOME.getIconIdIn16bit());
        when(nodes.findById(12L)).thenReturn(node);
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        NodePropertiesService service = new NodePropertiesService(nodes, children, settings);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        service.update(12L, "Yeni isim", true, TitleColor.RED, NodeIcon.OTHER, false);

        verify(query).setParameter("rich", (0xE01B24L << 3) | 3L);
        verify(query).setParameter("icon", 1L);
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
        node.setIsRichText((0x123456L << 3) | 1L);
        when(nodes.findById(12L)).thenReturn(node);
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        NodePropertiesService service = new NodePropertiesService(nodes, children, settings);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        service.update(12L, "Name", false, null, NodeIcon.CT_HOME, true);

        verify(query).setParameter("rich", (0x123456L << 3) | 1L);
        verify(query).setParameter("icon", 0x8000L | NodeIcon.CT_HOME.getIconIdIn16bit());
    }
}

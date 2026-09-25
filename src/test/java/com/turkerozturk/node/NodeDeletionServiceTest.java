package com.turkerozturk.node;

import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NodeDeletionServiceTest {
    private final NodeRepository nodes = mock(NodeRepository.class);
    private final ChildrenRepository children = mock(ChildrenRepository.class);
    private final ChildrenService tree = mock(ChildrenService.class);
    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private NodeDeletionService deletion;

    @BeforeEach void setUp() {
        TenantContext.setCurrentTenant("test");
        CustomPropertiesHolder properties = new CustomPropertiesHolder();
        properties.addCustomProperties("test", Map.of("custom.isWritable", "true"));
        deletion = new NodeDeletionService(properties, nodes, children, tree);
        ReflectionTestUtils.setField(deletion, "entityManager", manager);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), anyLong())).thenReturn(query);
    }

    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test void sharedReferenceDeletionDoesNotTouchMaster() {
        Children alias = new Children().setNodeId(20);
        alias.setMasterId(10L);
        when(children.findByNodeId(20L)).thenReturn(alias);
        when(children.findByFatherId(20L)).thenReturn(List.of());
        deletion.deleteNodeWithSubNodes(20);
        verify(manager).createNativeQuery("DELETE FROM bookmark WHERE node_id = :nodeId");
        verify(manager).createNativeQuery("DELETE FROM children WHERE node_id = :nodeId");
        verify(query, times(2)).setParameter("nodeId", 20L);
        verify(manager, times(2)).createNativeQuery(anyString());
        verifyNoInteractions(nodes, tree);
    }

    @Test void realNodeDeletionRemovesOutsideReferenceAndBookmarkBeforeNode() {
        Children original = new Children().setNodeId(10);
        Children alias = new Children().setNodeId(20);
        alias.setMasterId(10L);
        when(children.findByNodeId(10L)).thenReturn(original);
        when(tree.findAllSubChildren(10L)).thenReturn(List.of(original));
        when(children.findByMasterId(10L)).thenReturn(List.of(alias));
        when(children.findByFatherId(20L)).thenReturn(List.of());
        when(nodes.existsById(10L)).thenReturn(true);
        deletion.deleteNodeWithSubNodes(10);
        var order = inOrder(manager);
        order.verify(manager).createNativeQuery("DELETE FROM bookmark WHERE node_id = :nodeId");
        order.verify(manager).createNativeQuery("DELETE FROM children WHERE node_id = :nodeId");
        order.verify(manager).createNativeQuery("DELETE FROM bookmark WHERE node_id = :nodeId");
        order.verify(manager).createNativeQuery("DELETE FROM codebox WHERE node_id = :nodeId");
        order.verify(manager).createNativeQuery("DELETE FROM image WHERE node_id = :nodeId");
        order.verify(manager).createNativeQuery("DELETE FROM grid WHERE node_id = :nodeId");
        order.verify(manager).createNativeQuery("DELETE FROM children WHERE node_id = :nodeId");
        order.verify(manager).createNativeQuery("DELETE FROM node WHERE node_id = :nodeId");
    }

    @Test void refusesToOrphanChildrenOfSharedReference() {
        Children alias = new Children().setNodeId(20);
        alias.setMasterId(10L);
        when(children.findByNodeId(20L)).thenReturn(alias);
        when(children.findByFatherId(20L)).thenReturn(List.of(new Children().setNodeId(30)));
        assertThatThrownBy(() -> deletion.deleteNodeWithSubNodes(20)).hasMessageContaining("child rows");
        verifyNoInteractions(manager);
    }
}

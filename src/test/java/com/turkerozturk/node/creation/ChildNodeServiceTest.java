package com.turkerozturk.node.creation;

import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChildNodeServiceTest {
    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test void insertsAfterLastSiblingWithAliasSafeIdAndTenantDefaults() {
        TenantContext.setCurrentTenant("demo");
        CustomPropertiesHolder settings = new CustomPropertiesHolder();
        settings.addCustomProperties("demo", Map.of("custom.newNodeName", "", "custom.newNodeTags", "work"));
        NodePropertiesService properties = mock(NodePropertiesService.class);
        when(properties.writable()).thenReturn(true);
        EntityManager manager = mock(EntityManager.class);
        Query parent = mock(Query.class), ids = mock(Query.class), sequence = mock(Query.class), nodeInsert = mock(Query.class), childInsert = mock(Query.class);
        when(manager.createNativeQuery(anyString())).thenReturn(parent, ids, sequence, nodeInsert, childInsert);
        for (Query query : new Query[]{parent, ids, sequence, nodeInsert, childInsert}) {
            when(query.setParameter(anyString(), any())).thenReturn(query);
        }
        when(parent.getSingleResult()).thenReturn(1L);
        when(ids.getSingleResult()).thenReturn(56L);
        when(sequence.getSingleResult()).thenReturn(4L);
        ChildNodeService service = new ChildNodeService(properties, settings);
        ReflectionTestUtils.setField(service, "entityManager", manager);
        long before = Instant.now().getEpochSecond();

        assertThat(service.create(10L)).isEqualTo(56L);

        long after = Instant.now().getEpochSecond();
        verify(ids).getSingleResult();
        verify(sequence).setParameter("parent", 10L);
        verify(nodeInsert).setParameter("name", "");
        verify(nodeInsert).setParameter("tags", "work");
        verify(nodeInsert).setParameter("id", 56L);
        ArgumentCaptor<Long> timestamp = ArgumentCaptor.forClass(Long.class);
        verify(nodeInsert).setParameter(eq("timestamp"), timestamp.capture());
        assertThat(timestamp.getValue()).isBetween(before, after);
        verify(childInsert).setParameter("id", 56L);
        verify(childInsert).setParameter("parent", 10L);
        verify(childInsert).setParameter("sequence", 4L);
        verify(nodeInsert).executeUpdate();
        verify(childInsert).executeUpdate();
    }

    @Test void refusesReadOnlyTenantAndMissingParentBeforeAnyWrite() {
        NodePropertiesService properties = mock(NodePropertiesService.class);
        var manager = mock(EntityManager.class);
        ChildNodeService service = new ChildNodeService(properties, new CustomPropertiesHolder());
        ReflectionTestUtils.setField(service, "entityManager", manager);
        assertThatThrownBy(() -> service.create(10L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(manager);
        when(properties.writable()).thenReturn(true);
        var parent = mock(Query.class);
        when(manager.createNativeQuery(anyString())).thenReturn(parent);
        when(parent.setParameter("id", 10L)).thenReturn(parent);
        when(parent.getSingleResult()).thenReturn(0L);
        assertThatThrownBy(() -> service.create(10L)).isInstanceOf(ResponseStatusException.class);
        verify(parent, never()).executeUpdate();
        verify(properties, never()).realNode(anyLong());
    }

    @Test void createsTopLevelNodeWithoutLookingForRealParent() {
        TenantContext.setCurrentTenant("demo");
        CustomPropertiesHolder settings = new CustomPropertiesHolder();
        NodePropertiesService properties = mock(NodePropertiesService.class);
        when(properties.writable()).thenReturn(true);
        EntityManager manager = mock(EntityManager.class);
        Query ids = mock(Query.class), sequence = mock(Query.class), nodeInsert = mock(Query.class), childInsert = mock(Query.class);
        when(manager.createNativeQuery(anyString())).thenReturn(ids, sequence, nodeInsert, childInsert);
        for (Query query : new Query[]{ids, sequence, nodeInsert, childInsert}) {
            when(query.setParameter(anyString(), any())).thenReturn(query);
        }
        when(ids.getSingleResult()).thenReturn(57L);
        when(sequence.getSingleResult()).thenReturn(6L);
        ChildNodeService service = new ChildNodeService(properties, settings);
        ReflectionTestUtils.setField(service, "entityManager", manager);

        assertThat(service.createTopLevel()).isEqualTo(57L);

        verify(properties, never()).realNode(anyLong());
        verify(sequence).setParameter("parent", 0L);
        verify(nodeInsert).setParameter("name", "New node");
        verify(nodeInsert).setParameter("tags", "");
        verify(childInsert).setParameter("parent", 0L);
        verify(childInsert).setParameter("sequence", 6L);
    }

    @Test void createsSiblingImmediatelyAfterSharedOccurrence() {
        NodePropertiesService properties = mock(NodePropertiesService.class);
        when(properties.writable()).thenReturn(true);
        EntityManager manager = mock(EntityManager.class);
        Query occurrence = mock(Query.class), ids = mock(Query.class), sequence = mock(Query.class),
                insert = mock(Query.class), child = mock(Query.class), shift = mock(Query.class), place = mock(Query.class);
        when(manager.createNativeQuery(anyString())).thenReturn(occurrence, ids, sequence, insert, child, shift, place);
        for (Query query : new Query[]{occurrence, ids, sequence, insert, child, shift, place})
            when(query.setParameter(anyString(), any())).thenReturn(query);
        when(occurrence.getResultList()).thenReturn(java.util.Collections.singletonList(new Object[]{0L, 2L}));
        when(ids.getSingleResult()).thenReturn(99L);
        when(sequence.getSingleResult()).thenReturn(7L);
        var service = new ChildNodeService(properties, new CustomPropertiesHolder());
        ReflectionTestUtils.setField(service, "entityManager", manager);
        assertThat(service.createSibling(72L)).isEqualTo(99L);
        verify(properties, never()).realNode(72L);
        verify(child).setParameter("parent", 0L);
        verify(shift).setParameter("sequence", 2L);
        verify(shift).setParameter("id", 99L);
        verify(place).setParameter("sequence", 3L);
        verify(place).executeUpdate();
    }

    @Test void siblingCreationRejectsMissingOccurrenceAndReadOnlyTenant() {
        var properties = mock(NodePropertiesService.class);
        var manager = mock(EntityManager.class);
        var service = new ChildNodeService(properties, new CustomPropertiesHolder());
        ReflectionTestUtils.setField(service, "entityManager", manager);
        assertThatThrownBy(() -> service.createSibling(72L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(manager);
        when(properties.writable()).thenReturn(true);
        var query = mock(Query.class);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("id", 72L)).thenReturn(query);
        when(query.getResultList()).thenReturn(java.util.List.of());
        assertThatThrownBy(() -> service.createSibling(72L)).isInstanceOf(ResponseStatusException.class);
        verify(query, never()).executeUpdate();
    }
}

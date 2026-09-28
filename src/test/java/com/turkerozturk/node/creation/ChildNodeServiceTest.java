package com.turkerozturk.node.creation;

import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.node.Node;
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
        when(properties.realNode(10L)).thenReturn(new Node());
        EntityManager manager = mock(EntityManager.class);
        Query ids = mock(Query.class), sequence = mock(Query.class), nodeInsert = mock(Query.class), childInsert = mock(Query.class);
        when(manager.createNativeQuery(anyString())).thenReturn(ids, sequence, nodeInsert, childInsert);
        for (Query query : new Query[]{ids, sequence, nodeInsert, childInsert}) {
            when(query.setParameter(anyString(), any())).thenReturn(query);
        }
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

    @Test void refusesReadOnlyTenantAndSharedParent() {
        NodePropertiesService properties = mock(NodePropertiesService.class);
        ChildNodeService service = new ChildNodeService(properties, new CustomPropertiesHolder());
        assertThatThrownBy(() -> service.create(10L)).isInstanceOf(AccessDeniedException.class);
        when(properties.writable()).thenReturn(true);
        when(properties.realNode(10L)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.create(10L)).isInstanceOf(ResponseStatusException.class);
    }
}

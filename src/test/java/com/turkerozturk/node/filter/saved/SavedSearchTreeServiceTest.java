package com.turkerozturk.node.filter.saved;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.filter.*;
import com.turkerozturk.helpers.NodeIcon;
import jakarta.validation.Validation;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SavedSearchTreeServiceTest {
    private static final String ID="12345678-1234-1234-1234-123456789abc";
    private final SavedFiltersStore store=mock(SavedFiltersStore.class);
    private final AdvancedNodeSearch search=mock(AdvancedNodeSearch.class);
    @Test void reusesSavedFieldsAndOrderingButBoundsResultsAndPreservesOccurrenceIds()throws Exception {
        try(var validators=Validation.buildDefaultValidatorFactory()){
            var service=new SavedSearchTreeService(store,search,validators.getValidator());
            var fields=Map.of("query",List.of("two words"),"matchMode",List.of("any"),"tagsFilter",List.of("todo done"),"tagMode",List.of("all"),"sortBy",List.of("name"),"sortDirection",List.of("asc"),"nodeIcons",List.of(NodeIcon.values()[0].name()),"tsCreationStartFilter",List.of("2026-01-01"),"pageSize",List.of("100000"));
            when(store.read("user-source")).thenReturn(new SavedFiltersStore.Snapshot("user-source","revision",List.of(new SavedFiltersStore.Preset(ID,"Tasks",fields))));
            var node=new Node();node.setNodeId(55);node.setName("Shared occurrence");node.setTags("todo");node.setBreadcrumbs(new LinkedHashMap<>(Map.of(55L,"Shared occurrence")));
            when(search.search(any())).thenReturn(new PageImpl<>(List.of(node),PageRequest.of(0,50),101));
            var results=service.results("user-source",ID,0);var form=ArgumentCaptor.forClass(FormSearch.class);verify(search).search(form.capture());
            assertThat(form.getValue().getQuery()).isEqualTo("two words");assertThat(form.getValue().getMatchMode()).isEqualTo("any");
            assertThat(form.getValue().getTagMode()).isEqualTo("all");assertThat(form.getValue().getSortBy()).isEqualTo("name");
            assertThat(form.getValue().getSortDirection()).isEqualTo("asc");assertThat(form.getValue().getNodeIcons()).containsExactly(NodeIcon.values()[0]);
            assertThat(form.getValue().getTsCreationStartFilter()).isNotNull();assertThat(form.getValue().getPageSize()).isEqualTo(50);
            assertThat(results.results().get(0).nodeId()).isEqualTo(55);assertThat(results.results().get(0).hasChildren()).isFalse();assertThat(results.hasNext()).isTrue();
            assertThat(service.list("user-source")).containsExactly(new SavedSearchTreeService.Branch(ID,"Tasks"));
        }
    }
    @Test void invalidPersistedFieldsUnknownIdsAndOutOfRangePagesNeverExecuteSearch()throws Exception {
        try(var validators=Validation.buildDefaultValidatorFactory()){
            var service=new SavedSearchTreeService(store,search,validators.getValidator());
            when(store.read("source")).thenReturn(new SavedFiltersStore.Snapshot("source","r",List.of(new SavedFiltersStore.Preset(ID,"Invalid",Map.of("nodeIdStartFilter",List.of("not-a-number"))))));
            assertThatThrownBy(()->service.results("source",ID,0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(()->service.results("source",ID,-1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(()->service.results("source","../../other-user",0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(()->service.results("source","aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",0)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            verifyNoInteractions(search);
            assertThatThrownBy(()->service.form(Map.of("sortBy",List.of("DROP TABLE node")),0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(()->service.form(Map.of("tsCreationStartFilter",List.of("2026-02-31")),0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(()->service.form(Map.of("nodeIdStartFilter",List.of("20"),"nodeIdEndFilter",List.of("1")),0)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

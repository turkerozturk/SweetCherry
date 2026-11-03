package com.turkerozturk.customlists.views;

import com.turkerozturk.node.*;
import com.turkerozturk.children.*;
import com.turkerozturk.node.creation.ChildNodeService;
import com.turkerozturk.node.properties.NodePropertiesService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecordViewServiceTest {
    final NodeService nodes=mock(NodeService.class);final ChildrenRepository children=mock(ChildrenRepository.class);final TemplateService templates=mock(TemplateService.class);final NodePropertiesService properties=mock(NodePropertiesService.class);final ChildNodeService creation=mock(ChildNodeService.class);final RecordViewRenderer renderer=mock(RecordViewRenderer.class);
    final RecordViewService service=new RecordViewService(nodes,children,templates,properties,creation,renderer,new RecordViewSettings());
    static Children child(long id){var c=new Children();c.setNodeId(id);return c;}
    void ready(){when(nodes.findDisplayNode(1)).thenReturn(RecordViewRendererTest.node(1,"template","a"));when(children.findByFatherIdOrderBySequenceAsc(1)).thenReturn(List.of(child(10)));when(nodes.findDisplayNode(10)).thenReturn(RecordViewRendererTest.node(10,"_views",""));when(children.findByFatherIdOrderBySequenceAsc(10)).thenReturn(List.of(child(11)));when(nodes.findDisplayNode(11)).thenReturn(RecordViewRendererTest.node(11,"Layout","{{record.name}}"));}
    @Test void multipleFoldersAreExplicitAndNoFolderIsChosen(){
        ready();when(children.findByFatherIdOrderBySequenceAsc(1)).thenReturn(List.of(child(10),child(12)));when(nodes.findDisplayNode(12)).thenReturn(RecordViewRendererTest.node(12,"_views",""));
        assertThat(service.catalog(1).status()).isEqualTo("multiple");verify(children,never()).findByFatherIdOrderBySequenceAsc(10);assertThatThrownBy(()->service.render(1,11,2,"v",Locale.ENGLISH)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);verifyNoInteractions(templates,renderer);
    }
    @Test void verifiesLayoutOwnershipAndRendersOnlyTheRequestedRecord(){
        ready();assertThatThrownBy(()->service.render(1,999,2,"v",Locale.ENGLISH)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);verifyNoInteractions(templates);
        var record=RecordViewRendererTest.record();when(templates.getRecord(1,2,"v")).thenReturn(record);when(renderer.render(anyString(),any(),anyString(),eq(record),eq("v"),eq(Locale.ENGLISH),any())).thenReturn(new RecordViewRenderer.Output("safe",List.of()));
        assertThat(service.render(1,11,2,"v",Locale.ENGLISH).output().document()).isEqualTo("safe");verify(templates).getRecord(1,2,"v");verify(nodes,never()).getNodesByTags(anyString());
    }
    @Test void readOnlyAndExistingExamplesNeverCreateNodes(){
        assertThatThrownBy(()->service.createExample(1)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoInteractions(creation);
        ready();when(properties.writable()).thenReturn(true);when(nodes.findDisplayNode(11)).thenReturn(RecordViewRendererTest.node(11,"Example layout",""));assertThatThrownBy(()->service.createExample(1)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);verifyNoInteractions(creation);
    }
    @Test void sourceCodeMustBePlainText(){
        ready();var rich=RecordViewRendererTest.node(11,"Rich design","<node/>");rich.setSyntax("custom-colors");when(nodes.findDisplayNode(11)).thenReturn(rich);assertThat(service.catalog(1).views().get(0).plain()).isFalse();assertThatThrownBy(()->service.render(1,11,2,"v",Locale.ENGLISH)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);verifyNoInteractions(templates);
    }
}

package com.turkerozturk.node;
import com.turkerozturk.children.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class TrashDeletionControllerTest {
 @Test void enabledDeletionUsesReviewedTrashRevisionAndDisabledModeKeepsExistingDeletion(){
  var controller=new NodeDeletionController();var nodes=mock(NodeService.class);var deletion=mock(NodeDeletionService.class);var children=mock(ChildrenRepository.class);var trash=mock(TrashService.class);
  ReflectionTestUtils.setField(controller,"nodeService",nodes);ReflectionTestUtils.setField(controller,"nodeDeletionService",deletion);ReflectionTestUtils.setField(controller,"childrenRepository",children);ReflectionTestUtils.setField(controller,"trashService",trash);
  var occurrence=new Children().setNodeId(2);when(children.findByNodeId(2L)).thenReturn(occurrence);var node=new Node();node.setNodeId(2);node.setName("Note");when(nodes.findById(2L)).thenReturn(node);when(deletion.isCurrentTenantWritable()).thenReturn(true);when(deletion.deleteNodeWithSubNodes(2)).thenReturn(1L);
  var model=new ExtendedModelMap();assertThat(controller.confirmDeletion(2,model,"tree")).isEqualTo("node/nodeDelete");assertThat(model.get("trashMode")).isEqualTo(false);assertThat(controller.deleteNode(2,"tree",false,"")).isEqualTo("redirect:/tree?nodeId=1");verify(trash,never()).trash(anyLong(),anyString());clearInvocations(deletion);
  when(trash.enabled()).thenReturn(true);var preview=new TrashSql.Preview("revision","Note",2,1,0);when(trash.preview(2)).thenReturn(preview);when(trash.trash(2,"revision")).thenReturn(1L);
  model=new ExtendedModelMap();controller.confirmDeletion(2,model,"reader");assertThat(model.get("trashPreview")).isSameAs(preview);assertThatThrownBy(()->controller.deleteNode(2,"tree",false,"")).hasMessageContaining("trash.changed");verify(deletion,never()).deleteNodeWithSubNodes(anyLong());
  assertThat(controller.deleteNode(2,"reader",true,"revision")).isEqualTo("redirect:/nodes/1");verify(trash).trash(2,"revision");
 }
}

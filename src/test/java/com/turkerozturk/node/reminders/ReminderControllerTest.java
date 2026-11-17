package com.turkerozturk.node.reminders;

import com.turkerozturk.node.properties.NodePropertiesService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReminderControllerTest {
 @Test void invalidFiltersFallbackAndReadersDoNotGetWriteControls(){
  var service=mock(ReminderService.class);var properties=mock(NodePropertiesService.class);var model=new ExtendedModelMap();
  assertThat(new ReminderController(service,properties).list("","bad","x","1",new MockHttpServletRequest(),new MockHttpServletResponse(),model)).isEqualTo("node/reminders");
  assertThat(model).containsEntry("invalidOptions",true).containsEntry("canEditReminders",false);verify(service).inspect(new ReminderService.Options("","pending",50,1));verifyNoInteractions(properties);
 }
 @Test void invalidDatePreservesTypedValuesAndOriginalRevision(){
  var service=mock(ReminderService.class);var controller=new ReminderController(service,mock(NodePropertiesService.class));
  var editor=new ReminderService.Editor("",11,1,"Node",true,"2026-11-18T10:00","UTC","","pending","fresh");when(service.editor("",11)).thenReturn(editor);
  when(service.save("",11,1,"bad","bad/zone","typed","old")).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidTime"));
  var model=new ExtendedModelMap();var response=new MockHttpServletResponse();
  assertThat(controller.save("",11,1,"bad","bad/zone","typed","old","token",response,model,new RedirectAttributesModelMap())).isEqualTo("node/reminder-edit");
  assertThat(response.getStatus()).isEqualTo(400);var preserved=(ReminderService.Editor)model.get("editor");assertThat(preserved.message()).isEqualTo("typed");assertThat(preserved.revision()).isEqualTo("old");assertThat(preserved.zone()).isEqualTo("bad/zone");
 }
 @Test void successUsesFixedLocalRedirectAndFlashValue(){
  var controller=new ReminderController(mock(ReminderService.class),mock(NodePropertiesService.class));var flash=new RedirectAttributesModelMap();
  var target=controller.save("",1,1,"2026-11-18T10:00","UTC","note","r","token",new MockHttpServletResponse(),new ExtendedModelMap(),flash);
  assertThat(target).startsWith("redirect:/nodes/reminders?").contains("status=all","_tenantView=token");assertThat(flash.getFlashAttributes().get("reminderSaved")).isEqualTo(true);
 }
}

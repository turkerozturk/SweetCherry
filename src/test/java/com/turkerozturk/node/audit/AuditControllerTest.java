package com.turkerozturk.node.audit;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuditControllerTest {
 @Test void previewsAreBoundedButRawAndReadableDownloadsRemainComplete(){
  var service=mock(AuditReadService.class);var controller=new AuditController(service,true);String id=UUID.randomUUID().toString(),old="a".repeat(19999)+"😀"+"b".repeat(20000);
  var text=new AuditSql.TextState(2,"before","plain-text","Name",false,null,null,List.of(10L),List.of(10L),List.of());var event=new AuditSql.Event(1,id,"2026-11-14T12:00:00Z","admin","content.edit",List.of(2L),List.of(),List.of(),List.of(),List.of(text),List.of());when(service.event(id)).thenReturn(event);when(service.source(event,2,"before",false)).thenReturn(old);when(service.source(event,2,"before",true)).thenReturn(old);
  var model=new ExtendedModelMap();assertThat(controller.detail(id,"current",model,new MockHttpServletResponse())).isEqualTo("node/auditEvent");var view=((List<AuditController.SourceView>)model.get("sources")).get(0);assertThat(view.plain()).hasSize(19999);assertThat(view.longPlain()).isTrue();assertThat(controller.download(id,2,"before",true,"current").getBody()).isEqualTo(old.getBytes(java.nio.charset.StandardCharsets.UTF_8));
 }
}

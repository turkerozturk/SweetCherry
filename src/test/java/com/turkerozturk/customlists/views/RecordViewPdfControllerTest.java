package com.turkerozturk.customlists.views;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class RecordViewPdfControllerTest {
 @Test void downloadsSelectedRecordWithUtf8FilenameAndNoCache() {
  var service=mock(RecordViewService.class);var pdf=mock(RecordViewPdf.class);var controller=new RecordViewController(service);ReflectionTestUtils.setField(controller,"pdf",pdf);
  var record=RecordViewRendererTest.record();
  var page=new RecordViewService.Page(null,new RecordViewService.Choice(11,11,"Örnek: tasarım",true),record,new RecordViewRenderer.Output("<p>safe</p>",List.of()));
  var request=new MockHttpServletRequest();request.getSession().setAttribute(com.turkerozturk.multipledatabases.TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
  when(service.render(1,11,2,"current",Locale.ENGLISH)).thenReturn(page);when(pdf.export(page)).thenReturn(new byte[]{37,80,68,70});
  var response=controller.pdf(1,11,2,request,Locale.ENGLISH);
  assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
  assertThat(response.getHeaders().getContentDisposition().getFilename()).endsWith("Örnek_ tasarım.pdf");assertThat(response.getBody()).containsExactly(37,80,68,70);
  verify(service).render(1,11,2,"current",Locale.ENGLISH);
 }
 @Test void invalidLayoutOrMembershipNeverRendersPdf() {
  var service=mock(RecordViewService.class);var pdf=mock(RecordViewPdf.class);var controller=new RecordViewController(service);ReflectionTestUtils.setField(controller,"pdf",pdf);
  when(service.render(anyLong(),anyLong(),anyLong(),any(),any())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
  assertThatThrownBy(()->controller.pdf(1,99,2,new MockHttpServletRequest(),Locale.ENGLISH)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);verifyNoInteractions(pdf);
 }
}

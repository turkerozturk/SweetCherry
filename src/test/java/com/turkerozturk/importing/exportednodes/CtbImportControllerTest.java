package com.turkerozturk.importing.exportednodes;

import com.turkerozturk.multipledatabases.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CtbImportControllerTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void previewShowsCountsAndSingleUseConfirmationImportsOnlyOnce()throws Exception {
        var service=mock(CtbImportService.class);var controller=new CtbImportController(service);
        var preview=new CtbImportService.Preview("a.ctb","digest",Map.of("node",2,"children",3));
        when(service.preview("a.ctb")).thenReturn(preview);when(service.confirm(preview)).thenReturn(3);
        TenantContext.setCurrentTenant("Demo");var session=new MockHttpSession();var model=new ExtendedModelMap();
        assertThat(controller.preview("a.ctb",session,model)).isEqualTo("ctbImportPreview");
        verify(service,never()).confirm(any());assertThat(model.get("preview")).isEqualTo(preview);
        String token=(String)model.get("token");var redirect=new RedirectAttributesModelMap();
        assertThat(controller.confirm(token,session,redirect)).isEqualTo("redirect:/exportedFiles");
        assertThat(redirect.getFlashAttributes().get("importCount")).isEqualTo(3);
        controller.confirm(token,session,new RedirectAttributesModelMap());verify(service,times(1)).confirm(preview);
    }
    @Test void changedTenantExpiredOrIncorrectConfirmationNeverWrites()throws Exception {
        var service=mock(CtbImportService.class);var controller=new CtbImportController(service);var session=new MockHttpSession();
        var preview=new CtbImportService.Preview("a.ctb","hash",Map.of());
        TenantContext.setCurrentTenant("Other");
        session.setAttribute(CtbImportController.SESSION,new CtbImportController.Pending("token","Demo",preview,Long.MAX_VALUE));
        controller.confirm("token",session,new RedirectAttributesModelMap());
        session.setAttribute(CtbImportController.SESSION,new CtbImportController.Pending("token","Other",preview,0));
        controller.confirm("token",session,new RedirectAttributesModelMap());
        controller.confirm("wrong",session,new RedirectAttributesModelMap());verifyNoInteractions(service);
    }
    @Test void importPostsRequireTheCurrentTenantViewToken()throws Exception {
        var controller=new CtbImportController(mock(CtbImportService.class));
        var method=new org.springframework.web.method.HandlerMethod(controller,CtbImportController.class.getMethod("preview",String.class,jakarta.servlet.http.HttpSession.class,org.springframework.ui.Model.class));
        var guard=new TenantRequiredInterceptor();
        var request=new MockHttpServletRequest("POST","/exportedFiles/import/preview");var session=request.getSession();
        session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"view");
        var response=new MockHttpServletResponse();assertThat(guard.preHandle(request,response,method)).isFalse();assertThat(response.getRedirectedUrl()).isEqualTo("/");
        TenantContext.setCurrentTenant("Demo");response=new MockHttpServletResponse();assertThat(guard.preHandle(request,response,method)).isFalse();assertThat(response.getStatus()).isEqualTo(409);
        request.setParameter("_tenantView","view");assertThat(guard.preHandle(request,new MockHttpServletResponse(),method)).isTrue();
    }
}

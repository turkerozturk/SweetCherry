package com.turkerozturk.importing.zip;

import com.turkerozturk.multipledatabases.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ZipImportControllerTest {
    @AfterEach void cleanup(){TenantContext.clear();}
    MockHttpSession session(){var s=new MockHttpSession();s.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"view");TenantContext.setCurrentTenant("Demo");return s;}
    MockMultipartFile file(){return new MockMultipartFile("file","map.zip","application/zip","<map><node TEXT='r'/></map>".getBytes(StandardCharsets.UTF_8));}
    @Test void confirmationIsBoundToTenantViewAndSingleUse()throws Exception {
        var service=mock(ZipImportService.class);when(service.uploadBytes()).thenReturn(1000000L);var controller=new ZipImportController(service);
        var data=mock(ZipImportData.class);when(service.preview(any(),any())).thenReturn(data);when(service.confirm(data)).thenReturn(1);
        var session=session();var model=new ExtendedModelMap();controller.preview(file(),false,true,true,"UTF-8",session,model);var token=(String)model.get("token");
        controller.confirm(token,session,new RedirectAttributesModelMap());controller.confirm(token,session,new RedirectAttributesModelMap());verify(service,times(1)).confirm(data);
        model=new ExtendedModelMap();controller.preview(file(),false,true,true,"UTF-8",session,model);token=(String)model.get("token");
        session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"other-view");
        var redirect=new RedirectAttributesModelMap();controller.confirm(token,session,redirect);
        assertThat(redirect.getFlashAttributes().get("zipImportError")).isEqualTo("zipImport.error.changed");verify(service,times(1)).confirm(data);
    }
    @Test void cancelExpiryAndTenantSwitchCannotWrite()throws Exception {
        var service=mock(ZipImportService.class);when(service.uploadBytes()).thenReturn(1000000L);var controller=new ZipImportController(service);
        var data=mock(ZipImportData.class);when(service.preview(any(),any())).thenReturn(data);var s=session();
        var pending=new ZipImportController.Pending("token","Demo","view",data,0);s.setAttribute(ZipImportController.SESSION,pending);
        controller.confirm("token",s,new RedirectAttributesModelMap());
        s.setAttribute(ZipImportController.SESSION,new ZipImportController.Pending("token","other","view",data,Long.MAX_VALUE));
        controller.confirm("token",s,new RedirectAttributesModelMap());
        controller.preview(file(),false,true,true,"UTF-8",s,new ExtendedModelMap());controller.cancel(s);assertThat(s.getAttribute(ZipImportController.SESSION)).isNull();verify(service,never()).confirm(any());
    }
    @Test void everyImportPostRequiresCurrentViewEvenIfTheParameterIsOmitted()throws Exception {
        var s=session();var interceptor=new TenantRequiredInterceptor();
        var method=new HandlerMethod(new ZipImportController(mock(ZipImportService.class)),ZipImportController.class.getMethod("page",jakarta.servlet.http.HttpSession.class));
        for(String action:java.util.List.of("preview","confirm","cancel")) {
            var request=new MockHttpServletRequest("POST","/zip-import/"+action);request.setSession(s);
            var response=new MockHttpServletResponse();assertThat(interceptor.preHandle(request,response,method)).isFalse();assertThat(response.getStatus()).isEqualTo(409);
            request.setParameter("_tenantView","view");assertThat(interceptor.preHandle(request,new MockHttpServletResponse(),method)).isTrue();
        }
    }
}

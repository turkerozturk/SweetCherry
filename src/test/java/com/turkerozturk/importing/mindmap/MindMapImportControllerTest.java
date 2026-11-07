package com.turkerozturk.importing.mindmap;

import com.turkerozturk.multipledatabases.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MindMapImportControllerTest {
    @AfterEach void cleanup(){TenantContext.clear();}
    MockHttpSession session(){var s=new MockHttpSession();s.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"view");TenantContext.setCurrentTenant("Demo");return s;}
    MockMultipartFile file(){return new MockMultipartFile("file","map.mm","application/xml","<map><node TEXT='r'/></map>".getBytes(StandardCharsets.UTF_8));}
    @Test void confirmationIsBoundToTenantViewAndSingleUse()throws Exception {
        var service=mock(MindMapImportService.class);var controller=new MindMapImportController(service);
        var data=MindMapImportTest.read("<map><node TEXT='r'/></map>");when(service.preview(any())).thenReturn(data);when(service.confirm(data)).thenReturn(1);
        var session=session();var model=new ExtendedModelMap();controller.preview(file(),session,model);var token=(String)model.get("token");
        controller.confirm(token,session,new RedirectAttributesModelMap());controller.confirm(token,session,new RedirectAttributesModelMap());verify(service,times(1)).confirm(data);
        model=new ExtendedModelMap();controller.preview(file(),session,model);token=(String)model.get("token");
        session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"other-view");
        var redirect=new RedirectAttributesModelMap();controller.confirm(token,session,redirect);
        assertThat(redirect.getFlashAttributes().get("mmImportError")).isEqualTo("mmImport.error.changed");verify(service,times(1)).confirm(data);
    }
    @Test void cancelExpiryAndTenantSwitchCannotWrite()throws Exception {
        var service=mock(MindMapImportService.class);var controller=new MindMapImportController(service);
        var data=MindMapImportTest.read("<map><node/></map>");when(service.preview(any())).thenReturn(data);var s=session();
        var pending=new MindMapImportController.Pending("token","Demo","view",data,0);s.setAttribute(MindMapImportController.SESSION,pending);
        controller.confirm("token",s,new RedirectAttributesModelMap());
        s.setAttribute(MindMapImportController.SESSION,new MindMapImportController.Pending("token","other","view",data,Long.MAX_VALUE));
        controller.confirm("token",s,new RedirectAttributesModelMap());
        controller.preview(file(),s,new ExtendedModelMap());controller.cancel(s);assertThat(s.getAttribute(MindMapImportController.SESSION)).isNull();verify(service,never()).confirm(any());
    }
    @Test void everyImportPostRequiresCurrentViewEvenIfTheParameterIsOmitted()throws Exception {
        var s=session();var interceptor=new TenantRequiredInterceptor();
        var method=new HandlerMethod(new MindMapImportController(mock(MindMapImportService.class)),MindMapImportController.class.getMethod("page"));
        for(String action:java.util.List.of("preview","confirm","cancel")) {
            var request=new MockHttpServletRequest("POST","/mindmap-import/"+action);request.setSession(s);
            var response=new MockHttpServletResponse();assertThat(interceptor.preHandle(request,response,method)).isFalse();assertThat(response.getStatus()).isEqualTo(409);
            request.setParameter("_tenantView","view");assertThat(interceptor.preHandle(request,new MockHttpServletResponse(),method)).isTrue();
        }
    }
}

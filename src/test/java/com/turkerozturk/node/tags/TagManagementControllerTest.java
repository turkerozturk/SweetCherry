package com.turkerozturk.node.tags;

import com.turkerozturk.multipledatabases.TenantContext;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TagManagementControllerTest {
    private final TagManagementService service=mock(TagManagementService.class);
    private final NodeTagService tags=mock(NodeTagService.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-11-03T00:00:00Z"),ZoneOffset.UTC);
    private final TagManagementController controller=new TagManagementController(service,tags,clock);
    private final TagManagementService.Operation operation=new TagManagementService.Operation(TagManagementService.Action.RENAME,"old","new");
    @AfterEach void clear(){TenantContext.clear();}
    private String preview(MockHttpSession session){
        TenantContext.setCurrentTenant("Source");when(service.preview(operation)).thenReturn(new TagManagementService.Preview(operation,1,"digest",List.of(new TagManagementService.Row(2,"Name","old","new"))));
        var model=new ExtendedModelMap();assertThat(controller.preview("RENAME","old","new","view",session,model,new RedirectAttributesModelMap(),new MockHttpServletResponse())).isEqualTo("node/tag-management-preview");return model.get("confirmationToken").toString();
    }
    @Test void onlyFrozenOperationCanBeConfirmedOnceAndCancelDoesNotWrite(){
        var session=new MockHttpSession();String token=preview(session);when(service.apply(operation,"digest")).thenReturn(new TagManagementService.Result(operation,1));
        var flash=new RedirectAttributesModelMap();assertThat(controller.confirm(token,"view",session,flash)).isEqualTo("redirect:/nodes/tags");assertThat(flash.getFlashAttributes()).containsKey("tagManagementResult");verify(service).apply(operation,"digest");
        var replay=new RedirectAttributesModelMap();controller.confirm(token,"view",session,replay);assertThat(replay.getFlashAttributes().get("tagManagementError")).isEqualTo("tags.manage.error.expired");verify(service,times(1)).apply(any(),anyString());
        token=preview(session);when(tags.catalogue()).thenReturn(List.of("old"));controller.page(session,new ExtendedModelMap(),new MockHttpServletResponse());var cancel=new RedirectAttributesModelMap();controller.confirm(token,"view",session,cancel);assertThat(cancel.getFlashAttributes().get("tagManagementError")).isEqualTo("tags.manage.error.expired");verify(service,times(1)).apply(any(),anyString());
    }
    @Test void otherSessionsSourcesViewsAndExpiredPlansNeverApply(){
        var session=new MockHttpSession();String token=preview(session);
        for(String view:List.of("different","")){var flash=new RedirectAttributesModelMap();controller.confirm(token,view,session,flash);assertThat(flash.getFlashAttributes().get("tagManagementError")).isEqualTo("tags.manage.error.expired");}
        controller.confirm(token,"view",new MockHttpSession(),new RedirectAttributesModelMap());TenantContext.setCurrentTenant("Other");controller.confirm(token,"view",session,new RedirectAttributesModelMap());TenantContext.setCurrentTenant("Source");
        var expired=new TagManagementController(service,tags,Clock.offset(clock,Duration.ofSeconds(601)));var flash=new RedirectAttributesModelMap();expired.confirm(token,"view",session,flash);assertThat(flash.getFlashAttributes().get("tagManagementError")).isEqualTo("tags.manage.error.expired");verify(service,never()).apply(any(),anyString());
    }
}

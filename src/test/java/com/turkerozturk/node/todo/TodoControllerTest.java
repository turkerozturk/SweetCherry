package com.turkerozturk.node.todo;

import com.turkerozturk.node.properties.NodePropertiesService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoControllerTest {
    @Test void invalidFiltersFallbackAndReadersNeverReceiveWriteControls() {
        var service = mock(TodoService.class); var properties = mock(NodePropertiesService.class);
        var model = new ExtendedModelMap();
        var result = new TodoController(service,properties).show("","bad","50","1",new MockHttpServletRequest(),new MockHttpServletResponse(),model);
        assertThat(result).isEqualTo("node/todo"); assertThat(model).containsEntry("invalidOptions",true).containsEntry("canEditTasks",false);
        verify(service).inspect(new TodoService.Options("","open",50,1)); verifyNoInteractions(properties);
    }
    @Test void writesKeepFiltersInSafeLocalRedirectAndRejectUnknownState() {
        var service = mock(TodoService.class); var controller = new TodoController(service,mock(NodePropertiesService.class));
        var flash = new RedirectAttributesModelMap();
        var redirect = controller.update(1,"done","r","token","a & b","all","25","2",flash);
        assertThat(redirect).startsWith("redirect:/nodes/todo?").contains("q=a%20%26%20b","status=all","page=2","_tenantView=token");
        assertThat(flash.getFlashAttributes().get("todoSaved")).isEqualTo(true); verify(service).update(1,true,"r");
        assertThatThrownBy(() -> controller.update(1,"DELETE","r","token","","open","50","1",flash)).hasMessageContaining("invalid");
        verifyNoMoreInteractions(service);
    }
}

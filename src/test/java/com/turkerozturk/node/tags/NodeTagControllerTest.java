package com.turkerozturk.node.tags;
import com.turkerozturk.node.properties.NodePropertiesService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class NodeTagControllerTest {
 @Test void preservesMultipleTagSelectionsAndSafelyDefaultsInvalidParameters(){
  var service=mock(NodeTagService.class);var properties=mock(NodePropertiesService.class);var controller=new NodeTagController(service,properties);
  when(service.inspect(any())).thenAnswer(call->new NodeTagService.View(java.util.List.of("foo"),java.util.List.of(),0,0,0,call.getArgument(0)));
  var request=new MockHttpServletRequest();var model=new ExtendedModelMap();
  assertThat(controller.view(java.util.List.of("foo","bar"),"all","25","2",request,model)).isEqualTo("node/tags");
  verify(service).inspect(new NodeTagService.Options(java.util.List.of("foo","bar"),"all",25,2));assertThat(model.get("canEditTags")).isEqualTo(false);
  controller.view(java.util.List.of("bad tag"),"any","50","1",request,model);assertThat(model.get("invalidOptions")).isEqualTo(true);
 }
 @Test void httpParametersPreserveCommaAndPlusInsideSingleTags(){
  var service=mock(NodeTagService.class);var controller=new NodeTagController(service,mock(NodePropertiesService.class));
  when(service.inspect(any())).thenAnswer(call->new NodeTagService.View(java.util.List.of(),java.util.List.of(),0,0,0,call.getArgument(0)));
  var request=new MockHttpServletRequest();request.addParameter("tag","one,two","C++","a&b");
  controller.show("any","50","1",request,new ExtendedModelMap());
  verify(service).inspect(new NodeTagService.Options(java.util.List.of("one,two","C++","a&b"),"any",50,1));
 }

}

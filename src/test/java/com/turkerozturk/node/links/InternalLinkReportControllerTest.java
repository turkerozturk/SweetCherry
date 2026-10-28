package com.turkerozturk.node.links;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class InternalLinkReportControllerTest {
 @Test void acceptsFiltersAndSafelyDefaultsInvalidParameters(){
  var service=mock(InternalLinkReportService.class);var controller=new InternalLinkReportController(service);var model=new ExtendedModelMap();
  assertThat(controller.show("name","broken","25","2",model)).isEqualTo("node/internal-links");
  verify(service).inspect(new InternalLinkReportService.Options("name","broken",25,2));
  for(String value:java.util.List.of("0","-1","1001","abc","99999999999999")){
   model=new ExtendedModelMap();controller.show("","all",value,"1",model);assertThat(model.get("invalidOptions")).isEqualTo(true);
  }
  verify(service,times(5)).inspect(new InternalLinkReportService.Options("","all",50,1));
 }
}

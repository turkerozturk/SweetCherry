package com.turkerozturk.node;
import java.util.*;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class TrashControllerTest {
 @Test void paginatesAndBoundsLargeOrInvalidPageRequestsWithoutExtraQueries(){
  var service=mock(TrashService.class);var entries=LongStream.rangeClosed(1,205).mapToObj(id->new TrashSql.Entry(id,id,"Note","Root",0,"2026-11-14T10:00:00Z","admin",0,0,true,"revision")).toList();when(service.listing()).thenReturn(new TrashSql.Listing(999,entries));var controller=new TrashController(service);
  for(var item:Map.of("0",100,"1",100,"999999",5,"-1",100,"invalid",100).entrySet()){
   var model=new ExtendedModelMap();var response=new MockHttpServletResponse();assertThat(controller.list(model,response,item.getKey())).isEqualTo("node/trash");assertThat(((TrashSql.Listing)model.get("trash")).entries()).hasSize(item.getValue());assertThat(model.get("trashTotal")).isEqualTo(205);assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
  }verify(service,times(5)).listing();
 }
}

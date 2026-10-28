package com.turkerozturk.calendar;
import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.node.tags.NodeTagService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class CalendarControllerTest {
 @Test void readsLiteralCommaTagsAndRejectsInvalidDateWithoutSqlErrors()throws Exception {
  var service=mock(CalendarService.class);var properties=mock(NodePropertiesService.class);var tags=mock(NodeTagService.class);
  when(tags.catalogue()).thenReturn(List.of("one,two","C++"));
  var mvc=MockMvcBuilders.standaloneSetup(new CalendarController(service,new CalendarSettings("MONDAY","UTC",10000),properties,tags)).setViewResolvers((name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-calendar-view")).build();
  mvc.perform(get("/nodes/calendar/month").param("date","2026-10-28").param("tag","one,two","C++").locale(Locale.ENGLISH)).andExpect(status().isOk()).andExpect(view().name("calendar/calendar"));
  verify(service).view("month",LocalDate.of(2026,10,28),List.of("one,two","C++"),"any",Locale.ENGLISH);
  mvc.perform(get("/nodes/calendar/week").param("date","bad").locale(Locale.ENGLISH)).andExpect(status().isOk()).andExpect(model().attribute("invalidCalendarDate",true));
 }
 @Test void saveAndContentUseNoStoreJsonAndExposeOnlyErrorCodes()throws Exception {
  var service=mock(CalendarService.class);var mvc=MockMvcBuilders.standaloneSetup(new CalendarController(service,new CalendarSettings("MONDAY","UTC",10000),mock(NodePropertiesService.class),mock(NodeTagService.class))).build();
  mvc.perform(post("/nodes/calendar/11/created").param("expectedCreated","1").param("expectedModified","2").param("local","2030-02-03T04:05").param("_tenantView","v")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.saved").value(true));
  verify(service).changeCreated(11,1,2,"2030-02-03T04:05");
  when(service.content(11,"v")).thenReturn("<pre>safe</pre>");mvc.perform(get("/nodes/calendar/11/content").param("_tenantView","v")).andExpect(jsonPath("$.html").value("<pre>safe</pre>"));
  doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"conflict")).when(service).changeCreated(11,1,2,"2030-02-03T04:05");
  mvc.perform(post("/nodes/calendar/11/created").param("expectedCreated","1").param("expectedModified","2").param("local","2030-02-03T04:05").param("_tenantView","v")).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("conflict"));
 }
}

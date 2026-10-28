package com.turkerozturk.calendar;

import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.node.tags.NodeTagService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller @RequiresTenant
public class CalendarController {
    private final CalendarService service;private final CalendarSettings settings;private final NodePropertiesService properties;private final NodeTagService tags;
    public CalendarController(CalendarService service,CalendarSettings settings,NodePropertiesService properties,NodeTagService tags){this.service=service;this.settings=settings;this.properties=properties;this.tags=tags;}
    @GetMapping("/nodes/calendar/{mode:month|week}")
    public String view(@PathVariable String mode,@RequestParam(required=false)String date,@RequestParam(defaultValue="any")String match,HttpServletRequest request,Model model,Locale locale){
        LocalDate anchor=LocalDate.now(settings.zone());boolean invalid=false;
        if(date!=null)try{anchor=LocalDate.parse(date);if(anchor.getYear()<1||anchor.getYear()>9999)throw new IllegalArgumentException();}catch(RuntimeException error){anchor=LocalDate.now(settings.zone());invalid=true;}
        String[] raw=request.getParameterValues("tag");var selected=raw==null?List.<String>of():Arrays.asList(raw);
        var calendar=service.view(mode,anchor,selected,match,locale);model.addAttribute("calendar",calendar);model.addAttribute("invalidCalendarDate",invalid);
        var choices=new TreeSet<String>(com.turkerozturk.node.tags.NodeTags.ORDER);choices.addAll(tags.catalogue());choices.addAll(selected);model.addAttribute("calendarTags",choices);
        model.addAttribute("calendarEditable",request.isUserInRole("ADMIN")&&properties.writable());return "calendar/calendar";
    }
    @PostMapping("/nodes/calendar/{id}/created") @ResponseBody @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String,Object>> created(@PathVariable long id,@RequestParam long expectedCreated,@RequestParam long expectedModified,@RequestParam String local,@RequestParam String _tenantView){
        service.changeCreated(id,expectedCreated,expectedModified,local);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("saved",true));
    }
    @GetMapping("/nodes/calendar/{id}/content") @ResponseBody
    public ResponseEntity<Map<String,String>> content(@PathVariable long id,@RequestParam String _tenantView){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("html",service.content(id,_tenantView)));}
    @ExceptionHandler(ResponseStatusException.class) @ResponseBody
    public ResponseEntity<Map<String,String>> failure(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("error",Objects.requireNonNullElse(error.getReason(),"failed")));}
}

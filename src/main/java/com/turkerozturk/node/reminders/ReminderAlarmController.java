package com.turkerozturk.node.reminders;

import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.node.properties.NodePropertiesService;
import java.util.Map;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiresTenant
public class ReminderAlarmController {
    private final ReminderService service;private final NodePropertiesService properties;
    public ReminderAlarmController(ReminderService service,NodePropertiesService properties){this.service=service;this.properties=properties;}
    public record Feed(String storeId,List<ReminderService.Alarm> items,long total,boolean canAct) { }
    @GetMapping("/nodes/reminders/alarms")
    public ResponseEntity<Feed> due(@RequestParam String _tenantView,jakarta.servlet.http.HttpServletRequest request){
        var batch=service.alarms();return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Feed(batch.storeId(),batch.items(),batch.total(),request.isUserInRole("ADMIN")&&properties.writable()));
    }
    @PostMapping("/nodes/reminders/alarms/{id:[0-9a-f-]+}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> action(@PathVariable String id,@RequestParam String key,@RequestParam String action,@RequestParam String _tenantView){
        service.alarmAction(id,key,action);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("ok",true));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> problem(ResponseStatusException problem){return ResponseEntity.status(problem.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("error",problem.getReason()==null?"failed":problem.getReason()));}
}

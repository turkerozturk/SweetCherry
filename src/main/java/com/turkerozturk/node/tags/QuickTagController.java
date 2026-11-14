package com.turkerozturk.node.tags;
import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestController @RequiresTenant @PreAuthorize("hasRole('ADMIN')")
public class QuickTagController {
    private final QuickTagService service;
    public QuickTagController(QuickTagService service){this.service=service;}
    @GetMapping("/nodes/tags/quick/{id}") public ResponseEntity<QuickTagService.Snapshot> open(@PathVariable long id,@RequestParam String _tenantView){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.open(id));}
    @PostMapping("/nodes/tags/quick/{id}") public ResponseEntity<QuickTagService.Snapshot> save(@PathVariable long id,@RequestParam String tags,@RequestParam String revision,@RequestParam String _tenantView){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.save(id,tags,revision));}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> problem(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("error",error.getReason()==null?"failed":error.getReason()));}
}

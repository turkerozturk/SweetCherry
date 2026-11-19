package com.turkerozturk.node.title;
import com.turkerozturk.multipledatabases.RequiresTenant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequiresTenant @PreAuthorize("hasRole('ADMIN')")
public class NodeTitleController {
    private final NodeTitleService service;
    public NodeTitleController(NodeTitleService service){this.service=service;}
    @GetMapping("/nodes/properties/{id}/title") public ResponseEntity<NodeTitleService.Snapshot> open(@PathVariable long id,@RequestParam String _tenantView){return response(service.open(id));}
    @PostMapping("/nodes/properties/{id}/title") public ResponseEntity<NodeTitleService.Snapshot> save(@PathVariable long id,@RequestParam String name,@RequestParam String revision,@RequestParam String _tenantView){return response(service.save(id,name,revision));}
    private ResponseEntity<NodeTitleService.Snapshot> response(NodeTitleService.Snapshot value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> problem(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("error",error.getReason()==null?"failed":error.getReason()));}
}

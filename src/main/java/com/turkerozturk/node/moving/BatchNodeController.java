package com.turkerozturk.node.moving;
import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController @RequiresTenant @PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/nodes/batch")
public class BatchNodeController {
 private final BatchNodeService service;
 public BatchNodeController(BatchNodeService service){this.service=service;}
 @PostMapping("/preview") public ResponseEntity<BatchNodeService.Preview> preview(@RequestParam List<Long> ids,@RequestParam String _tenantView){return response(service.preview(ids));}
 @PostMapping("/apply") public ResponseEntity<BatchNodeService.Result> apply(@RequestParam List<Long> ids,@RequestParam String revision,@RequestParam String operation,@RequestParam(defaultValue="") String parentId,@RequestParam(defaultValue="") String tag,@RequestParam String _tenantView){return response(service.apply(ids,revision,operation,parentId,tag));}
 private static <T> ResponseEntity<T> response(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> problem(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("error",Objects.requireNonNullElse(error.getReason(),"failed")));}
}

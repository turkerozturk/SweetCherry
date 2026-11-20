package com.turkerozturk.node.moving;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller @RequiresTenant @PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/nodes/children/{id}/sort")
public class SortChildrenController {
    private final SortChildrenService service;
    public SortChildrenController(SortChildrenService service){this.service=service;}
    @GetMapping @ResponseBody public ResponseEntity<SortChildrenService.State> state(@PathVariable long id,@RequestParam String _tenantView){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.state(id));}
    @PostMapping @ResponseBody public ResponseEntity<Void> sort(@PathVariable long id,@RequestParam String _tenantView,@RequestParam SortChildrenService.Key key,@RequestParam SortChildrenService.Order order,@RequestParam String revision,@RequestParam(defaultValue="false") boolean confirmed){
        if(!confirmed)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        service.sort(id,key,order,revision,true);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Void> failure(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).build();}
}

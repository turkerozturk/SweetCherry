package com.turkerozturk.node.fragments;

import com.turkerozturk.multipledatabases.RequiresTenant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequiresTenant
public class NodeFragmentController {
    private final NodeFragmentService service;
    public NodeFragmentController(NodeFragmentService service){this.service=service;}
    @GetMapping("/nodes/fragments/{id}")
    public ResponseEntity<?> fragment(@PathVariable long id,@RequestParam String _tenantView){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(service.fragment(id,_tenantView));
    }
    @ExceptionHandler(NodeFragmentService.Problem.class)
    public ResponseEntity<?> problem(NodeFragmentService.Problem error){
        return ResponseEntity.status(switch(error.code()){case "missing"->404;case "large"->413;default->422;}).cacheControl(CacheControl.noStore()).body(Map.of("error",error.code()));
    }
}

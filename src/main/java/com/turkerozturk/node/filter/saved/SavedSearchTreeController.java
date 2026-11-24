package com.turkerozturk.node.filter.saved;

import com.turkerozturk.editor.EditorRecoveryContext;
import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequiresTenant
public class SavedSearchTreeController {
    private final SavedSearchTreeService service;
    private final EditorRecoveryContext scopes;
    @Value("${myapp.tree.saved-searches-enabled:true}") private boolean enabled=true;
    public SavedSearchTreeController(SavedSearchTreeService service,EditorRecoveryContext scopes){this.service=service;this.scopes=scopes;}
    @GetMapping("/nodes/saved-filters/tree")
    public ResponseEntity<?> list(@RequestParam String _tenantView,HttpServletRequest request){return run(request,scope->service.list(scope));}
    @GetMapping("/nodes/saved-filters/tree/{id}")
    public ResponseEntity<?> results(@PathVariable String id,@RequestParam(defaultValue="0") int page,
            @RequestParam String _tenantView,HttpServletRequest request){return run(request,scope->service.results(scope,id,page));}
    @FunctionalInterface private interface Operation {Object call(String scope)throws IOException;}
    private ResponseEntity<?> run(HttpServletRequest request,Operation operation){
        if(!enabled)return error(404,"disabled");
        // An unsupported preference namespace is optional-feature unavailability, not a stale CTB.
        String scope=scopes.savedFiltersScope(request);if(scope==null||scope.isEmpty())return error(503,"scope-unavailable");
        try{service.migrate(scope,scopes.scope(request));return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(operation.call(scope));}
        catch(IllegalArgumentException invalid){return error(400,"invalid");}
        catch(ResponseStatusException missing){return error(missing.getStatusCode().value(),"missing");}
        catch(IOException unavailable){return error(503,"storage");}
    }
    private ResponseEntity<?> error(int status,String error){return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("error",error));}
}

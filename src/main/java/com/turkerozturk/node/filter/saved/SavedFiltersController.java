package com.turkerozturk.node.filter.saved;

import com.turkerozturk.editor.EditorRecoveryContext;
import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;

@RestController
@RequiresTenant
public class SavedFiltersController {
    private final SavedFiltersStore store;
    private final EditorRecoveryContext scopes;
    public SavedFiltersController(SavedFiltersStore store,EditorRecoveryContext scopes){this.store=store;this.scopes=scopes;}
    public record Save(String revision,String id,String name,Map<String,List<String>> fields) { }
    public record Delete(String revision,String id) { }
    @GetMapping("/nodes/saved-filters")
    public ResponseEntity<?> list(@RequestParam String _tenantView,HttpServletRequest request){
        return run(()->store.read(scope(request)));
    }
    @PostMapping("/nodes/saved-filters")
    public ResponseEntity<?> save(@RequestParam String _tenantView,@RequestBody Save value,HttpServletRequest request){
        return run(()->store.save(scope(request),value.revision(),value.id(),value.name(),value.fields()));
    }
    @PostMapping("/nodes/saved-filters/delete")
    public ResponseEntity<?> delete(@RequestParam String _tenantView,@RequestBody Delete value,HttpServletRequest request){
        return run(()->store.delete(scope(request),value.revision(),value.id()));
    }
    private String scope(HttpServletRequest request){String value=scopes.scope(request);if(value.isEmpty())throw new SavedFiltersStore.Conflict();return value;}
    @FunctionalInterface private interface Operation{Object call()throws IOException;}
    private ResponseEntity<?> run(Operation operation){
        try{return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(operation.call());}
        catch(SavedFiltersStore.Conflict stale){return error(409,"stale");}
        catch(IllegalArgumentException invalid){return error(400,"invalid");}
        catch(IOException failure){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Saved filter storage unavailable: {}",failure.getClass().getSimpleName());return error(503,"storage");}
    }
    private ResponseEntity<?> error(int status,String code){return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("error",code));}
}

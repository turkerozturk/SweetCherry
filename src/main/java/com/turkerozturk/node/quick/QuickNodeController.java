package com.turkerozturk.node.quick;

import com.turkerozturk.editor.EditorRecoveryContext;
import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiresTenant
public class QuickNodeController {
    private final QuickNodeService service;
    private final EditorRecoveryContext scope;
    public QuickNodeController(QuickNodeService service, EditorRecoveryContext scope) { this.service=service; this.scope=scope; }

    @GetMapping("/nodes/quick/search")
    public ResponseEntity<QuickNodeService.Search> search(@RequestParam(defaultValue="") String q,
                                                         @RequestParam String _tenantView) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.search(q));
    }
    @GetMapping("/nodes/quick/options")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String,Object>> options(@RequestParam String _tenantView, HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("writable",service.writable(),"scope",scope.scope(request)));
    }
    @PostMapping("/nodes/quick/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String,String>> create(@RequestParam String parentId, @RequestParam String title,
            @RequestParam(defaultValue="") String text, @RequestParam(defaultValue="plain-text") String syntax,
            @RequestParam String _tenantView) {
        final long parent;
        try {
            if (!parentId.matches("0|[1-9][0-9]{0,18}")) throw new NumberFormatException();
            parent = Long.parseLong(parentId);
        } catch (NumberFormatException invalid) { return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).build(); }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("id",Long.toString(service.create(parent,title,text,syntax))));
    }
}

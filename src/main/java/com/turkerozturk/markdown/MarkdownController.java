package com.turkerozturk.markdown;

import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.node.contentediting.NodeContentEditingService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Preview is read-only; the existing content editor performs every actual write. */
@Controller @RequiresTenant @PreAuthorize("hasRole('ADMIN')")
public class MarkdownController {
    private final NodeContentEditingService service;
    public MarkdownController(NodeContentEditingService service){this.service=service;}
    @PostMapping("/nodes/markdown/{id}/preview") @ResponseBody
    public ResponseEntity<Map<String,String>> preview(@PathVariable long id,@RequestParam String text,@RequestParam String _tenantView){
        var node=service.editableNode(id);
        if(!"markdown".equals(node.getSyntax()))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("html",NodeMarkup.render("markdown",text,_tenantView)));
    }
}

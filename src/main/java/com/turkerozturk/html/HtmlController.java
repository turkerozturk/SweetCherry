package com.turkerozturk.html;

import com.turkerozturk.markdown.NodeMarkup;
import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.node.contentediting.NodeContentEditingService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller @RequiresTenant @PreAuthorize("hasRole('ADMIN')")
public class HtmlController {
    private final NodeContentEditingService service;
    public HtmlController(NodeContentEditingService service){this.service=service;}
    @PostMapping("/nodes/html/{id}/preview") @ResponseBody
    public ResponseEntity<Map<String,String>> preview(@PathVariable long id,@RequestParam String text,@RequestParam String _tenantView){
        var node=service.editableNode(id);
        if(!"html".equals(node.getSyntax()))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST);
        String fragment=NodeMarkup.render("html",text,_tenantView);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("document",HtmlNodeRenderer.document(fragment)));
    }
}

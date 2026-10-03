package com.turkerozturk.node.duplication;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
public class NodeDuplicationController {
    private final NodeDuplicationService service;
    public NodeDuplicationController(NodeDuplicationService service) {this.service=service;}
    @GetMapping("/nodes/duplicate/{id}/state")
    @ResponseBody
    public NodeDuplicationService.State state(@PathVariable long id) {return service.state(id);}

    /** Copies one occurrence or a complete branch and opens the newly allocated root in the same reader. */
    @PostMapping("/nodes/duplicate/{id}")
    public String duplicate(@PathVariable long id,@RequestParam(defaultValue="false") boolean withSubnodes,
            @RequestParam String revision,@RequestParam(defaultValue="reader") String view) {
        long result=service.duplicate(id,withSubnodes,revision);
        return "tree".equals(view)?"redirect:/tree?nodeId="+result:"redirect:/nodes/"+result;
    }
}

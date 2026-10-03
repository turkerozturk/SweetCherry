package com.turkerozturk.node.moving;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
public class NodeMoveController {
    private final NodeMoveService service;
    public NodeMoveController(NodeMoveService service) { this.service = service; }
    @GetMapping("/nodes/move/{id}/state")
    @ResponseBody
    public NodeMoveService.State state(@PathVariable long id) { return service.state(id); }

    /** Accepts only structural directions and redirects to the same node in the requested new view. */
    @PostMapping("/nodes/move/{id}")
    public String move(@PathVariable long id, @RequestParam String direction, @RequestParam String revision,
            @RequestParam(defaultValue="reader") String view) {
        NodeMoveService.Direction selected;
        try { selected = NodeMoveService.Direction.valueOf(direction); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid direction"); }
        service.move(id,selected,revision);
        return "tree".equals(view) ? "redirect:/tree?nodeId="+id : "redirect:/nodes/"+id;
    }
}

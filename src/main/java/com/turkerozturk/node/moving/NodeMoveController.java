package com.turkerozturk.node.moving;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Controller
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
public class NodeMoveController {
    private final NodeMoveService service;
    public NodeMoveController(NodeMoveService service) { this.service = service; }
    @GetMapping("/nodes/move/{id}/state")
    @ResponseBody
    public NodeMoveService.State state(@PathVariable long id) { return service.state(id); }

    @PostMapping("/nodes/move/{id}/to")
    @ResponseBody
    public ResponseEntity<Void> moveTo(@PathVariable long id, @RequestParam String parentId, @RequestParam String revision) {
        long parent;
        try {
            if (!parentId.matches("[0-9]{1,19}")) throw new NumberFormatException();
            parent = Long.parseLong(parentId);
        } catch (NumberFormatException error) {
            return ResponseEntity.badRequest().build();
        }
        try {
            service.moveTo(id,parent,revision);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException error) {
            // Expected validation failures stay in the dialog, without a servlet /error dispatch.
            return ResponseEntity.status(error.getStatusCode()).build();
        }
    }

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

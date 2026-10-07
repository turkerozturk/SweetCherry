package com.turkerozturk.node.creation;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
public class ChildNodeController {
    private final ChildNodeService service;

    public ChildNodeController(ChildNodeService service) {
        this.service = service;
    }

    @PostMapping("/nodes/children/{parentId}")
    public String create(@PathVariable long parentId) {
        return "redirect:/nodes/" + service.create(parentId);
    }

    @PostMapping("/nodes/siblings/{nodeId}")
    public String createSibling(@PathVariable long nodeId) {
        return "redirect:/nodes/" + service.createSibling(nodeId);
    }

    @PostMapping("/nodes/top-level")
    public String createTopLevel() {
        return "redirect:/nodes/" + service.createTopLevel();
    }
}

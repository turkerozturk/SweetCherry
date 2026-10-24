package com.turkerozturk.node.contentediting;

import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.node.Node;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
public class NodeContentEditingController {
    private final NodeContentEditingService service;

    public NodeContentEditingController(NodeContentEditingService service) {
        this.service = service;
    }

    @GetMapping("/nodes/content/{nodeId}")
    public String edit(@PathVariable long nodeId, Model model) {
        Node node = service.editableNode(nodeId);
        model.addAttribute("node", node);
        return "node/nodeContentEdit";
    }

    public String save(long nodeId,String text) { return save(nodeId,text,false); }

    @PostMapping("/nodes/content/{nodeId}")
    public String save(@PathVariable long nodeId, @RequestParam("text") String text,
                       @RequestParam(defaultValue="false") boolean continueEditing) {
        service.update(nodeId, text);
        return continueEditing ? "redirect:/nodes/content/"+nodeId : "redirect:/nodes/" + nodeId;
    }
}

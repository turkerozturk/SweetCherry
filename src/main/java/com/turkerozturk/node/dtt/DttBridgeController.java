package com.turkerozturk.node.dtt;

import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.node.NodeService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiresTenant
public class DttBridgeController {
    private final DttBridge bridge;
    private final NodeService nodes;

    public DttBridgeController(DttBridge bridge, NodeService nodes) {
        this.bridge = bridge;
        this.nodes = nodes;
    }

    @GetMapping("/nodes/dtt/{id}/entry")
    public String read(@PathVariable long id, @RequestParam String _tenantView,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        bridge.read(nodes.findDisplayNode(id), date, model);
        model.addAttribute("requestedNodeId", id);
        return "node/dttBridge :: panel";
    }

    @PostMapping("/nodes/dtt/{id}/today")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(@PathVariable long id, @RequestParam String _tenantView,
                         @RequestParam String binding, @RequestParam String revision,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam(defaultValue = "false") boolean dated,
                         @RequestParam(defaultValue = "save") String operation,
                         @RequestParam(required = false) Integer status,
                         @RequestParam(defaultValue = "") String note,
                         Model model, HttpServletResponse response) {
        if (!("save".equals(operation) || "delete".equals(operation))
                || !revision.matches("new|[a-f0-9]{64}")
                || !binding.matches("[a-f0-9]{64}")
                || ("save".equals(operation) && (status == null || status < 0 || status > 2 || note.length() > 65536))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        response.setHeader("Cache-Control", "no-store");
        bridge.update(nodes.findDisplayNode(id), binding, new DttBridge.Update(date, revision, status, note),
                "delete".equals(operation), dated, model);
        model.addAttribute("requestedNodeId", id);
        model.addAttribute("dttSaved", true);
        return "node/dttBridge :: panel";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public void error(ResponseStatusException error, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setStatus(error.getStatusCode().value());
    }
}

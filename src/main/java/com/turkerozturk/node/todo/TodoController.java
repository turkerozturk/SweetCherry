package com.turkerozturk.node.todo;

import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.servlet.http.*;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiresTenant
public class TodoController {
    private final TodoService service;
    private final NodePropertiesService properties;
    public TodoController(TodoService service, NodePropertiesService properties) { this.service = service; this.properties = properties; }
    private TodoService.Options options(String q, String status, String limit, String page) {
        return new TodoService.Options(q, status, Integer.parseInt(limit), Integer.parseInt(page));
    }
    @GetMapping("/nodes/todo")
    public String show(@RequestParam(defaultValue="") String q, @RequestParam(defaultValue="open") String status,
            @RequestParam(defaultValue="50") String limit, @RequestParam(defaultValue="1") String page,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        TodoService.Options options;
        try { options = options(q,status,limit,page); }
        catch (IllegalArgumentException invalid) { options = new TodoService.Options("","open",50,1); model.addAttribute("invalidOptions",true); }
        response.setHeader("Cache-Control","no-store");
        model.addAttribute("tasks",service.inspect(options));
        model.addAttribute("canEditTasks",request.isUserInRole("ADMIN") && properties.writable());
        return "node/todo";
    }
    @PostMapping("/nodes/todo/{id}/state")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(@PathVariable long id, @RequestParam String state, @RequestParam String revision,
            @RequestParam String _tenantView, @RequestParam(defaultValue="") String q,
            @RequestParam(defaultValue="open") String status, @RequestParam(defaultValue="50") String limit,
            @RequestParam(defaultValue="1") String page, RedirectAttributes flash) {
        if (!Set.of("open","done").contains(state)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalid");
        TodoService.Options options;
        try { options = options(q,status,limit,page); }
        catch (IllegalArgumentException invalid) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalid"); }
        service.update(id,state.equals("done"),revision);
        flash.addFlashAttribute("todoSaved",true);
        return "redirect:" + UriComponentsBuilder.fromPath("/nodes/todo").queryParam("q",options.q()).queryParam("status",options.status())
                .queryParam("limit",options.limit()).queryParam("page",options.page()).queryParam("_tenantView",_tenantView).build().encode().toUriString();
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView problem(ResponseStatusException error, HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        String reason = error.getReason();
        if (reason == null || !Set.of("changed","readonly","invalid","missing").contains(reason)) reason = "changed";
        var view = new ModelAndView("node/todo-error"); view.setStatus(error.getStatusCode()); view.addObject("todoError",reason); return view;
    }
}

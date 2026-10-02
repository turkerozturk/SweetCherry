package com.turkerozturk.richtext.editing;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.turkerozturk.multipledatabases.RequiresTenant;

@Controller
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
public class RichTextEditingController {
    private final RichTextEditingService service;
    public RichTextEditingController(RichTextEditingService service) { this.service = service; }

    /** Opens a separate editor; no conversion or database write happens on GET. */
    @GetMapping("/nodes/richtext/edit/{id}")
    public String open(@PathVariable long id, Model model) {
        model.addAttribute("editor", service.open(id));
        return "node/richtext-edit";
    }

    @PostMapping("/nodes/richtext/edit/{id}")
    public String save(@PathVariable long id, @RequestParam String xml, @RequestParam String revision,
            @RequestParam(defaultValue = "{}") String images, @RequestParam(defaultValue = "{}") String files, @RequestParam(defaultValue = "{}") String tables) {
        service.save(id, xml, revision, images, files, tables);
        return "redirect:/nodes/" + id;
    }
}

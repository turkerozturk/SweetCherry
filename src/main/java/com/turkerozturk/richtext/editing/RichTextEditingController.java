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

    public String save(long id,String xml,String revision,String images,String files,String tables,String deleted,boolean allowObjectDeletion) {
        return save(id,xml,revision,images,files,tables,deleted,allowObjectDeletion,false);
    }

    @PostMapping("/nodes/richtext/edit/{id}")
    public String save(@PathVariable long id, @RequestParam String xml, @RequestParam String revision,
            @RequestParam(defaultValue = "{}") String images, @RequestParam(defaultValue = "{}") String files, @RequestParam(defaultValue = "{}") String tables,
            @RequestParam(defaultValue = "[]") String deleted, @RequestParam(defaultValue = "false") boolean allowObjectDeletion,
            @RequestParam(defaultValue="false") boolean continueEditing) {
        service.save(id, xml, revision, images, files, tables, deleted, allowObjectDeletion);
        return continueEditing ? "redirect:/nodes/richtext/edit/"+id : "redirect:/nodes/" + id;
    }
}

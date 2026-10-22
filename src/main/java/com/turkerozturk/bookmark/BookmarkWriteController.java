package com.turkerozturk.bookmark;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
public class BookmarkWriteController {
    private final BookmarkWriteService service;
    public BookmarkWriteController(BookmarkWriteService service){this.service=service;}
    @GetMapping("/bookmarks/{id}/state")
    @ResponseBody
    public BookmarkWriteService.State state(@PathVariable long id){return service.state(id);}

    /** Returns to the same occurrence in the new desktop tree or mobile reader after adding it. */
    @PostMapping("/bookmarks/{id}/add")
    public String add(@PathVariable long id,@RequestParam(defaultValue="reader") String view){
        service.add(id);return "tree".equals(view)?"redirect:/tree?nodeId="+id:"redirect:/nodes/"+id;
    }
    @PostMapping("/bookmarks/order")
    public String reorderAll(@RequestParam java.util.List<Long> order,@RequestParam String revision, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try { service.reorderAll(order,revision); }
        catch (org.springframework.web.server.ResponseStatusException ex) {
            if (ex.getStatusCode().value() != 409) throw ex;
            flash.addFlashAttribute("bookmarkOrderConflict", true);
        }
        return "redirect:/bookmarks";
    }
    @PostMapping("/bookmarks/{id}/sequence")
    public String reorder(@PathVariable long id, @RequestParam long sequence) {
        service.reorder(id,sequence); return "redirect:/bookmarks";
    }
    @PostMapping("/bookmarks/{id}/remove")
    public String remove(@PathVariable long id){service.remove(id);return "redirect:/bookmarks";}
}

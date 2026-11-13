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
        model.addAttribute("codeboxLanguages", RichTextCodeboxes.languages());
        return "node/richtext-edit";
    }

    public String save(long id,String xml,String revision,String images,String files,String tables,String deleted,boolean allowObjectDeletion) {
        return save(id,xml,revision,images,files,tables,deleted,allowObjectDeletion,false);
    }

    public String save(long id,String xml,String revision,String images,String files,String tables,String deleted,boolean allowObjectDeletion,boolean continueEditing) {
        return save(id,xml,revision,images,files,tables,deleted,allowObjectDeletion,continueEditing,"{}");
    }

    public String save(long id,String xml,String revision,String images,String files,String tables,String deleted,boolean allowObjectDeletion,boolean continueEditing,String codeboxes){
        return save(id,xml,revision,images,files,tables,deleted,allowObjectDeletion,continueEditing,codeboxes,"{}",false);
    }
    @PostMapping("/nodes/richtext/edit/{id}")
    public String save(@PathVariable long id, @RequestParam String xml, @RequestParam String revision,
            @RequestParam(defaultValue = "{}") String images, @RequestParam(defaultValue = "{}") String files, @RequestParam(defaultValue = "{}") String tables,
            @RequestParam(defaultValue = "[]") String deleted, @RequestParam(defaultValue = "false") boolean allowObjectDeletion,
            @RequestParam(defaultValue="false") boolean continueEditing, @RequestParam(defaultValue="{}") String codeboxes,
            @RequestParam(defaultValue="{}") String wikiChoices,@RequestParam(defaultValue="false") boolean keepUnresolvedWikiLinks) {
        if("{}".equals(wikiChoices)&&!keepUnresolvedWikiLinks)service.save(id, xml, revision, images, files, tables, deleted, allowObjectDeletion, codeboxes);
        else service.save(id,xml,revision,images,files,tables,deleted,allowObjectDeletion,codeboxes,wikiChoices,keepUnresolvedWikiLinks);
        return continueEditing ? "redirect:/nodes/richtext/edit/"+id : "redirect:/nodes/" + id;
    }
    @PostMapping("/nodes/richtext/edit/{id}/wiki-links") @ResponseBody
    public org.springframework.http.ResponseEntity<RichTextWikiLinks.Preview> wikiLinks(@PathVariable long id,@RequestParam String xml){
        return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(service.wikiPreview(id,xml));
    }
    /** Explicit receipt only after the existing transactional save returns successfully. */
    @org.springframework.web.bind.annotation.PostMapping(value="/nodes/richtext/edit/{id}", headers="X-SweetCherry-Editor-Save=1")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String,Object>> saveWithReceipt(@PathVariable long id, @RequestParam String xml, @RequestParam String revision, @RequestParam(defaultValue="{}") String images, @RequestParam(defaultValue="{}") String files, @RequestParam(defaultValue="{}") String tables, @RequestParam(defaultValue="[]") String deleted, @RequestParam(defaultValue="false") boolean allowObjectDeletion, @RequestParam(defaultValue="false") boolean continueEditing, @RequestParam(defaultValue="{}") String codeboxes,@RequestParam(defaultValue="{}") String wikiChoices,@RequestParam(defaultValue="false") boolean keepUnresolvedWikiLinks, jakarta.servlet.http.HttpServletRequest request) {
        String redirect=save(id,xml,revision,images,files,tables,deleted,allowObjectDeletion,continueEditing,codeboxes,wikiChoices,keepUnresolvedWikiLinks);
        return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(java.util.Map.of("saved",true,"redirect",request.getContextPath()+redirect.substring("redirect:".length())));
    }
}

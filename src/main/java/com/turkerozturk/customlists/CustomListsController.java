package com.turkerozturk.customlists;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequiresTenant
public class CustomListsController {
    private final CustomListsService service;
    public CustomListsController(CustomListsService service){this.service=service;}
    @GetMapping("/cherrytemplatetasks")
    public String page(Model model,jakarta.servlet.http.HttpServletRequest request){model.addAttribute("setup",service.inspect());model.addAttribute("customListsAdmin",request.isUserInRole("ADMIN"));return "customlists/cherrytemplatetasks";}
    @PostMapping("/cherrytemplatetasks/create") @PreAuthorize("hasRole('ADMIN')")
    public String create(@RequestParam(defaultValue="")String key,@RequestParam(defaultValue="false")boolean main,RedirectAttributes flash){
        long id=main?service.createMain():service.createTemplate(key);flash.addFlashAttribute("customListsCreated",id);return "redirect:/cherrytemplatetasks";
    }
}

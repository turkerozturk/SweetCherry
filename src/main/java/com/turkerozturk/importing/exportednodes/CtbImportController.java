package com.turkerozturk.importing.exportednodes;

import com.turkerozturk.multipledatabases.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/exportedFiles/import")
public class CtbImportController {
    private final CtbImportService service;
    static final String SESSION="ctbImportPreview";
    record Pending(String token,String tenant,CtbImportService.Preview preview,long expires) { }
    public CtbImportController(CtbImportService service){this.service=service;}
    @PostMapping("/preview")
    public String preview(@RequestParam String filename,HttpSession session,Model model) {
        model.addAttribute("importMaxBytes",service.maxBytes());
        model.addAttribute("importMaxRecords",service.maxRecords());
        session.removeAttribute(SESSION);
        try {
            var preview=service.preview(filename);
            var pending=new Pending(UUID.randomUUID().toString(),TenantContext.getCurrentTenant(),preview,System.currentTimeMillis()+600000);
            session.setAttribute(SESSION,pending);
            model.addAttribute("preview",preview); model.addAttribute("token",pending.token());
            model.addAttribute("tenant",pending.tenant());
        } catch(Exception failure){ model.addAttribute("importError",errorKey(failure)); }
        return "ctbImportPreview";
    }
    @PostMapping("/confirm")
    public String confirm(@RequestParam String token,HttpSession session,RedirectAttributes redirect) {
        Pending pending;
        synchronized(session) {
            pending=(Pending)session.getAttribute(SESSION);
            if(pending!=null && pending.token().equals(token)) session.removeAttribute(SESSION);
            else pending=null;
        }
        try {
            if(pending==null || pending.expires()<System.currentTimeMillis()
                    || !Objects.equals(pending.tenant(),TenantContext.getCurrentTenant())) throw new IllegalArgumentException("changed");
            int count=service.confirm(pending.preview());
            redirect.addFlashAttribute("importCount",count);
        } catch(Exception failure){redirect.addFlashAttribute("importError",errorKey(failure));}
        return "redirect:/exportedFiles";
    }
    private static String errorKey(Exception failure) {
        if(failure instanceof org.springframework.security.access.AccessDeniedException) return "ctbImport.readonly";
        if(failure instanceof IllegalArgumentException && Set.of("file","schema","hierarchy","limit","changed","links").contains(failure.getMessage()))
            return "ctbImport.error."+failure.getMessage();
        return "ctbImport.error.failed";
    }
}

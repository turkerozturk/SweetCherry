package com.turkerozturk.importing.mindmap;

import com.turkerozturk.multipledatabases.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller @RequiresTenant @PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/mindmap-import")
public class MindMapImportController {
    static final String SESSION="mindmapImportPreview";
    record Pending(String token,String tenant,String view,MindMapImportData data,long expires) { }
    private final MindMapImportService service;
    public MindMapImportController(MindMapImportService service){this.service=service;}
    @GetMapping public String page(){return "mindmap-import";}
    @PostMapping("/preview")
    public String preview(@RequestParam MultipartFile file,HttpSession session,Model model) {
        session.removeAttribute(SESSION);
        try {
            String filename=file.getOriginalFilename();
            if(filename==null || !filename.toLowerCase(Locale.ROOT).endsWith(".mm"))throw new IllegalArgumentException("file");
            if(file.getSize()>MindMapImportData.MAX_BYTES)throw new IllegalArgumentException("limit");
            MindMapImportData data;
            try(var stream=file.getInputStream()){data=service.preview(stream);}
            var pending=new Pending(UUID.randomUUID().toString(),TenantContext.getCurrentTenant(),
                    (String)session.getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN),data,System.currentTimeMillis()+600000);
            session.setAttribute(SESSION,pending);model.addAttribute("preview",data);model.addAttribute("token",pending.token());
        }catch(Exception e){model.addAttribute("mmImportError",error(e));}
        return "mindmap-import";
    }
    @PostMapping("/confirm")
    public String confirm(@RequestParam String token,HttpSession session,RedirectAttributes redirect) {
        Pending pending;
        synchronized(session){
            pending=(Pending)session.getAttribute(SESSION);
            if(pending!=null && pending.token().equals(token))session.removeAttribute(SESSION);
            else pending=null;
        }
        try {
            if(pending==null || !pending.token().equals(token) || pending.expires()<System.currentTimeMillis()
                    || !Objects.equals(pending.tenant(),TenantContext.getCurrentTenant())
                    || !Objects.equals(pending.view(),session.getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN)))throw new IllegalArgumentException("changed");
            redirect.addFlashAttribute("mmImportCount",service.confirm(pending.data()));
        }catch(Exception e){redirect.addFlashAttribute("mmImportError",error(e));}
        return "redirect:/mindmap-import";
    }
    @PostMapping("/cancel")
    public String cancel(HttpSession session){session.removeAttribute(SESSION);return "redirect:/mindmap-import";}
    private static String error(Exception e){
        if(e instanceof org.springframework.security.access.AccessDeniedException)return "mmImport.error.readonly";
        if(e instanceof IllegalArgumentException && Set.of("file","xml","limit","changed").contains(e.getMessage()))return "mmImport.error."+e.getMessage();
        return "mmImport.error.failed";
    }
}

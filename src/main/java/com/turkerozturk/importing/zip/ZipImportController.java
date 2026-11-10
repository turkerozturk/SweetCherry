package com.turkerozturk.importing.zip;

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
@RequestMapping("/zip-import")
public class ZipImportController {
    static final String SESSION="zipImportPreview";
    record Pending(String token,String tenant,String view,ZipImportData data,long expires) { }
    private final ZipImportService service;
    public ZipImportController(ZipImportService service){this.service=service;}
    @ModelAttribute public void limits(Model model){model.addAttribute("zipUploadBytes",service.uploadBytes());model.addAttribute("zipAttachmentBytes",service.attachmentBytes());}
    @GetMapping public String page(HttpSession session){session.removeAttribute(SESSION);return "zip-import";}
    @PostMapping("/preview")
    public String preview(@RequestParam MultipartFile file,
            @RequestParam(defaultValue="false") boolean removeExtension,
            @RequestParam(defaultValue="false") boolean attachments,
            @RequestParam(defaultValue="false") boolean metadata,
            @RequestParam(defaultValue="UTF-8") String charset,HttpSession session,Model model) {
        session.removeAttribute(SESSION);
        try {
            String filename=file.getOriginalFilename();
            if(filename==null || !filename.toLowerCase(Locale.ROOT).endsWith(".zip"))throw new IllegalArgumentException("file");
            if(file.getSize()>service.uploadBytes())throw new IllegalArgumentException("upload");
            ZipImportData data;
            try(var stream=file.getInputStream()){data=service.preview(stream,new ZipImportData.Options(removeExtension,attachments,metadata,charset));}
            var pending=new Pending(UUID.randomUUID().toString(),TenantContext.getCurrentTenant(),
                    (String)session.getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN),data,System.currentTimeMillis()+600000);
            session.setAttribute(SESSION,pending);model.addAttribute("preview",data);model.addAttribute("token",pending.token());
        }catch(Exception e){model.addAttribute("zipImportError",error(e));}
        return "zip-import";
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
            redirect.addFlashAttribute("zipImportCount",service.confirm(pending.data()));
        }catch(Exception e){redirect.addFlashAttribute("zipImportError",error(e));}
        return "redirect:/zip-import";
    }
    @PostMapping("/cancel")
    public String cancel(HttpSession session){session.removeAttribute(SESSION);return "redirect:/zip-import";}
    private static String error(Exception e){
        if(e instanceof org.springframework.security.access.AccessDeniedException)return "zipImport.error.readonly";
        if(e instanceof IllegalArgumentException && Set.of("file","zip","limit","changed","path","empty","encoding","upload").contains(e.getMessage()))return "zipImport.error."+e.getMessage();
        return "zipImport.error.failed";
    }
}

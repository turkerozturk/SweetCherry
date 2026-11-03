package com.turkerozturk.customlists.views;

import com.turkerozturk.multipledatabases.*;
import jakarta.servlet.http.*;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiresTenant
public class RecordViewController {
    private final RecordViewService service;
    public RecordViewController(RecordViewService service){this.service=service;}
    @GetMapping("/cherrytemplatenode/{templateId}/views")
    public String catalog(@PathVariable long templateId,@RequestParam(required=false)Long recordId,Model model,HttpServletRequest request){
        model.addAttribute("layouts",service.catalog(templateId));model.addAttribute("selectedRecord",recordId);model.addAttribute("layoutAdmin",request.isUserInRole("ADMIN"));return "customlists/record-views";
    }
    @GetMapping("/cherrytemplatenode/{templateId}/view/{viewId}")
    public String render(@PathVariable long templateId,@PathVariable long viewId,@RequestParam long recordId,Model model,HttpServletRequest request,Locale locale){
        String token=(String)request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN);
        model.addAttribute("recordView",service.render(templateId,viewId,recordId,token,locale));model.addAttribute("layoutAdmin",request.isUserInRole("ADMIN"));return "customlists/record-view";
    }
    @org.springframework.beans.factory.annotation.Autowired private RecordViewPdf pdf;
    @GetMapping("/cherrytemplatenode/{templateId}/view/{viewId}/pdf")
    public org.springframework.http.ResponseEntity<byte[]> pdf(@PathVariable long templateId,@PathVariable long viewId,@RequestParam long recordId,HttpServletRequest request,Locale locale){
        String token=(String)request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN);
        var page=service.render(templateId,viewId,recordId,token,locale);
        byte[] bytes=pdf.export(page);
        String filename=com.turkerozturk.pdf.PdfFilename.create(page.record().getName()+" - "+page.view().name(),recordId);
        return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_PDF)
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,org.springframework.http.ContentDisposition.attachment().filename(filename,java.nio.charset.StandardCharsets.UTF_8).build().toString())
            .header(org.springframework.http.HttpHeaders.CACHE_CONTROL,"no-store").contentLength(bytes.length).body(bytes);
    }
    @PostMapping("/cherrytemplatenode/{templateId}/views/example")
    @PreAuthorize("hasRole('ADMIN')")
    public String example(@PathVariable long templateId,RedirectAttributes flash){flash.addFlashAttribute("layoutCreated",service.createExample(templateId));return "redirect:/cherrytemplatenode/"+templateId+"/views";}
}

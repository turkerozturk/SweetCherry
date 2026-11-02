package com.turkerozturk.export.texttree;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiresTenant
@PreAuthorize("isAuthenticated()")
public class TextTreeExportController {
    private final TextTreeExportService service;
    private final TextTreeExportLimits limits;
    public TextTreeExportController(TextTreeExportService service,TextTreeExportLimits limits){this.service=service;this.limits=limits;}
    @GetMapping("/nodes/export/text/{nodeId}")
    public String page(@PathVariable long nodeId,Model model){
        model.addAttribute("nodeId",nodeId);model.addAttribute("rootName",service.title(nodeId));model.addAttribute("limits",limits);
        if(!model.containsAttribute("includeDescendants"))model.addAttribute("includeDescendants",true);
        return "node/text-tree-export";
    }
    @PostMapping("/nodes/export/text/{nodeId}")
    public String download(@PathVariable long nodeId,@RequestParam(defaultValue="false") boolean includeDescendants,
            @RequestParam("_tenantView") String token,HttpServletRequest request,HttpServletResponse response,RedirectAttributes flash)throws IOException {
        TextTreeExportService.Archive archive;
        try{archive=service.export(nodeId,includeDescendants);}
        catch(RuntimeException failure){
            String key="textTree.error.failed";
            if(failure instanceof ResponseStatusException status)key=switch(status.getStatusCode().value()){
                case 413 -> "textTree.error.limit";case 429 -> "textTree.error.busy";case 409,404,422 -> "textTree.error.tree";default -> key;
            };
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Text tree ZIP export failed for occurrence {}: {}",nodeId,key);
            flash.addFlashAttribute("textTreeError",key);flash.addFlashAttribute("includeDescendants",includeDescendants);
            flash.addAttribute("_tenantView",token);return "redirect:/nodes/export/text/"+nodeId;
        }
        try(archive){
            response.setContentType("application/zip");response.setHeader("Cache-Control","no-store");response.setHeader("X-Content-Type-Options","nosniff");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(archive.filename(),StandardCharsets.UTF_8).build().toString());
            response.setContentLengthLong(Files.size(archive.path()));Files.copy(archive.path(),response.getOutputStream());response.flushBuffer();
        }
        return null;
    }
}

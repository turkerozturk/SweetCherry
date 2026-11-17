package com.turkerozturk.image.attachments.preview;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller @RequiresTenant
public class FilePreviewController {
    private final FilePreviewService service;
    public FilePreviewController(FilePreviewService service){this.service=service;}
    @GetMapping("/attachments/{nodeId}/{offset}/preview")
    public String preview(@PathVariable long nodeId,@PathVariable int offset,@RequestParam String _tenantView,
            @RequestParam(defaultValue="auto") String mode,@RequestParam(defaultValue="UTF-8") String encoding,@RequestParam(defaultValue="1") String page,
            @CookieValue(value="viewMode",defaultValue="tree") String viewMode,Model model,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");response.setHeader("X-Content-Type-Options","nosniff");
        model.addAttribute("nodeId",nodeId);model.addAttribute("offset",offset);model.addAttribute("encodings",FilePreviewService.ENCODINGS);
        try{
            FilePreviewService.Options options;
            try{options=new FilePreviewService.Options(mode,encoding,Long.parseLong(page));}catch(IllegalArgumentException bad){throw new FilePreviewService.Problem("request");}
            model.addAttribute("file",service.preview(nodeId,offset,options));
        }catch(FilePreviewService.Problem error){
            response.setStatus(switch(error.code()){case "missing"->404;case "request"->400;case "changed"->409;default->422;});model.addAttribute("fileError",error.code());
        }
        return "mobile".equals(viewMode)?"attachment/fileMobile":"attachment/file";
    }
}

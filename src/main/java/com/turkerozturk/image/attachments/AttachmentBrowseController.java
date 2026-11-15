package com.turkerozturk.image.attachments;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller @RequiresTenant @RequestMapping("/attachments")
public class AttachmentBrowseController {
    private final AttachmentBrowseService service;
    public AttachmentBrowseController(AttachmentBrowseService service){this.service=service;}
    @GetMapping public String list(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="")String q,
            @RequestParam(defaultValue="newest")String sort,@CookieValue(value="viewMode",defaultValue="tree")String viewMode,
            Model model,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");model.addAttribute("attachments",service.list(new AttachmentBrowseService.Options(page,q,sort)));
        model.addAttribute("viewMode",viewMode);return "mobile".equals(viewMode)?"attachmentsMobile":"attachments";
    }
    @GetMapping("/{nodeId}/{offset}/download")
    public void download(@PathVariable long nodeId,@PathVariable int offset,@RequestParam String _tenantView,HttpServletResponse response)throws IOException{
        response.setHeader("Cache-Control","no-store");
        service.stream(nodeId,offset,meta->{
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);response.setContentLengthLong(meta.bytes());
            response.setHeader("X-Content-Type-Options","nosniff");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(meta.filename(),StandardCharsets.UTF_8).build().toString());
        },response.getOutputStream());
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Void> failure(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).build();}
}

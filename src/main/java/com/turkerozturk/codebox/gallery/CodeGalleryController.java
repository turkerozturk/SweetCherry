package com.turkerozturk.codebox.gallery;
import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;

@Controller @RequiresTenant @RequestMapping("/codebox-gallery")
public class CodeGalleryController {
    private final CodeGalleryService service;private final CodeGallerySettings settings;
    public CodeGalleryController(CodeGalleryService service,CodeGallerySettings settings){this.service=service;this.settings=settings;}
    @GetMapping public String gallery(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String syntax,@RequestParam(defaultValue="newest")String sort,Model model,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");model.addAttribute("gallery",service.list(new CodeGalleryService.Options(page,q,syntax,sort)));model.addAttribute("codeSettings",settings);return "gallery/codeboxes";
    }
    @GetMapping("/{nodeId}/{offset}/data") @ResponseBody public ResponseEntity<CodeGalleryService.Data> data(@PathVariable long nodeId,@PathVariable int offset,@RequestParam String _tenantView){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.data(nodeId,offset));}
    @GetMapping("/{nodeId}/{offset}/source") @ResponseBody public ResponseEntity<byte[]> source(@PathVariable long nodeId,@PathVariable int offset,@RequestParam String _tenantView){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_OCTET_STREAM).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"codebox-"+nodeId+"-"+offset+".txt\"").body(service.source(nodeId,offset).text().getBytes(StandardCharsets.UTF_8));}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Void> failure(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).build();}
}

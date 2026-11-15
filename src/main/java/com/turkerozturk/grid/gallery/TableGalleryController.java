package com.turkerozturk.grid.gallery;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

@Controller @RequiresTenant @RequestMapping("/table-gallery")
public class TableGalleryController {
    private final TableGalleryService service;
    private final TableGallerySettings settings;
    private final Semaphore parsers=new Semaphore(2);
    public TableGalleryController(TableGalleryService service,TableGallerySettings settings){this.service=service;this.settings=settings;}
    @GetMapping public String gallery(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="")String q,
            @RequestParam(defaultValue="newest")String sort,Model model,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");model.addAttribute("gallery",service.list(new TableGalleryService.Options(page,q,sort)));
        model.addAttribute("tableSettings",settings);return "gallery/tables";
    }
    @GetMapping("/{nodeId}/{offset}/data") @ResponseBody
    public ResponseEntity<TableGalleryService.Data> data(@PathVariable long nodeId,@PathVariable int offset,
            @RequestParam String _tenantView,@RequestParam(defaultValue="false")boolean full){
        if(!parsers.tryAcquire())return ResponseEntity.status(429).cacheControl(CacheControl.noStore()).header("Retry-After","1").build();
        try{return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.data(nodeId,offset,full));}
        finally{parsers.release();}
    }
    @GetMapping("/{nodeId}/{offset}/source") @ResponseBody
    public ResponseEntity<byte[]> source(@PathVariable long nodeId,@PathVariable int offset,@RequestParam String _tenantView){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Content-Type-Options","nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"table-"+nodeId+"-"+offset+".xml\"")
                .body(service.source(nodeId,offset).getBytes(StandardCharsets.UTF_8));
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Void> failure(ResponseStatusException error){
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).build();
    }
}

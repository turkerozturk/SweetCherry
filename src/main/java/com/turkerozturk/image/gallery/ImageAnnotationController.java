package com.turkerozturk.image.gallery;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller @RequiresTenant @PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/image-gallery/{nodeId}/{offset}")
public class ImageAnnotationController {
    private final ImageAnnotationService service;
    private final java.util.concurrent.Semaphore encoders=new java.util.concurrent.Semaphore(2);
    public ImageAnnotationController(ImageAnnotationService service){this.service=service;}
    @GetMapping("/annotate") public String page(@PathVariable long nodeId,@PathVariable int offset,Model model,HttpServletResponse response){
        if(nodeId<=0||offset<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        response.setHeader("Cache-Control","no-store");model.addAttribute("annotationNode",nodeId);model.addAttribute("annotationOffset",offset);return "gallery/annotate";
    }
    @GetMapping("/annotation") @ResponseBody
    public ResponseEntity<ImageAnnotationService.Snapshot> open(@PathVariable long nodeId,@PathVariable int offset,@RequestParam String _tenantView){
        if(!encoders.tryAcquire())return ResponseEntity.status(429).cacheControl(CacheControl.noStore()).build();
        try{return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.open(nodeId,offset));}
        finally{encoders.release();}
    }
    @PostMapping(value="/annotation",consumes=MediaType.IMAGE_PNG_VALUE) @ResponseBody
    public ResponseEntity<Void> save(@PathVariable long nodeId,@PathVariable int offset,@RequestParam String _tenantView,
            @RequestParam String revision,@RequestParam(defaultValue="false") boolean confirmed,HttpServletRequest request)throws IOException{
        if(!confirmed)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if(!encoders.tryAcquire())return ResponseEntity.status(429).cacheControl(CacheControl.noStore()).build();
        try{
        if(request.getContentLengthLong()>ImageAnnotationService.MAX_BYTES)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        byte[] png=request.getInputStream().readNBytes(ImageAnnotationService.MAX_BYTES+1);
        if(png.length>ImageAnnotationService.MAX_BYTES)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        service.save(nodeId,offset,revision,png,true);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        }finally{encoders.release();}
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Void> failure(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).build();}
}

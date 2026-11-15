package com.turkerozturk.image.gallery;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.concurrent.Semaphore;

@Controller
@RequiresTenant
@RequestMapping("/image-gallery")
public class GalleryController {
    private final GalleryService service;
    private final Semaphore decoders = new Semaphore(2);
    public GalleryController(GalleryService service) { this.service = service; }

    @GetMapping
    public String gallery(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "") String q,
                          @RequestParam(defaultValue = "newest") String sort,
                          Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("gallery", service.list(new GalleryService.Options(page, q, sort)));
        return "gallery/gallery";
    }

    @GetMapping("/{nodeId}/{offset}/preview")
    @ResponseBody
    public ResponseEntity<byte[]> preview(@PathVariable long nodeId, @PathVariable int offset,
                                         @RequestParam String _tenantView,
                                         @RequestParam(defaultValue = "false") boolean large) {
        if (!decoders.tryAcquire()) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .cacheControl(CacheControl.noStore()).header("Retry-After", "1").build();
        try {
            var payload = service.payload(nodeId, offset);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(GalleryPreview.png(payload.bytes(), large ? 2560 : 320));
        } finally { decoders.release(); }
    }

    @GetMapping("/{nodeId}/{offset}/original")
    @ResponseBody
    public ResponseEntity<byte[]> original(@PathVariable long nodeId, @PathVariable int offset,
                                          @RequestParam String _tenantView) {
        var payload = service.payload(nodeId, offset);
        // Fixed safe download name; untrusted stored filenames are displayed only as text.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"image-" + nodeId + "-" + offset + "." + GalleryPreview.extension(payload.bytes()) + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(payload.bytes());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Void> failure(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).build();
    }
}

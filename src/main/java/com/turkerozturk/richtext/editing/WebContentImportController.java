package com.turkerozturk.richtext.editing;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
public class WebContentImportController {
    private final RichTextEditingService editing;
    private final WebContentImportService importer;
    @Value("${myapp.web-import-enabled:true}") private boolean enabled=true;
    public WebContentImportController(RichTextEditingService editing,WebContentImportService importer){this.editing=editing;this.importer=importer;}
    @PostMapping("/nodes/richtext/import/{id}")
    public WebContentImportService.Result load(@PathVariable long id,@RequestParam String url) {
        if(!enabled)throw new PublicWebFetcher.Problem("disabled");
        editing.open(id); // ADMIN + selected tenant + writable CTB/node, same checks as the editor.
        return importer.load(url);
    }
    @ExceptionHandler(PublicWebFetcher.Problem.class)
    ResponseEntity<java.util.Map<String,String>> failed(PublicWebFetcher.Problem error) {
        return ResponseEntity.status(error.code.equals("busy")?429:422).body(java.util.Map.of("error",error.code));
    }
}

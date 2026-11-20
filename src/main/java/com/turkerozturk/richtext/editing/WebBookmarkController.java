package com.turkerozturk.richtext.editing;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController @RequiresTenant
public class WebBookmarkController {
    private final WebBookmarkService service;
    @Value("${myapp.web-bookmarks-enabled:true}") private boolean enabled=true;
    public WebBookmarkController(WebBookmarkService service){this.service=service;}
    @GetMapping("/nodes/web-bookmarks/{id}")
    public ResponseEntity<?> preview(@PathVariable long id,@RequestParam String _tenantView){
        if(!enabled)return error(404,"disabled");
        try{return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.load(id));}
        catch(PublicWebFetcher.Problem problem){return error(problem.code.equals("busy")?429:422,problem.code);}
        catch(ResponseStatusException unavailable){return error(unavailable.getStatusCode().value(),"missing");}
    }
    private ResponseEntity<?> error(int status,String code){return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("error",code));}
}

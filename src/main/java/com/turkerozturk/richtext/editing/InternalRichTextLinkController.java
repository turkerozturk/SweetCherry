package com.turkerozturk.richtext.editing;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/nodes/richtext/links")
public class InternalRichTextLinkController {
    private final InternalRichTextTargets targets;
    public InternalRichTextLinkController(InternalRichTextTargets targets){this.targets=targets;}
    @GetMapping("/target")
    public ResponseEntity<InternalRichTextTargets.Target> target(@RequestParam(defaultValue="") String nodeId,@RequestParam String _tenantView) {
        final long id;
        try {if(!nodeId.matches("[1-9][0-9]{0,18}"))throw new NumberFormatException();id=Long.parseLong(nodeId);}
        catch(NumberFormatException invalid){return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).build();}
        var target=targets.target(id);
        return target==null ? ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build()
                : ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(target);
    }
    @GetMapping("/matches")
    public ResponseEntity<InternalRichTextTargets.Matches> matches(@RequestParam(defaultValue="") String name,@RequestParam String _tenantView) {
        if(name.isBlank() || name.length()>1000 || name.codePoints().anyMatch(Character::isISOControl))
            return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(targets.matches(name));
    }
}

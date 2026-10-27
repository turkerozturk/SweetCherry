package com.turkerozturk.node.navigation;

import com.turkerozturk.bookmark.BookmarkShortcuts;
import com.turkerozturk.global.ViewController;
import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/** Navigation is available to authenticated readers, including read-only data sources. */
@RestController
@RequiresTenant
public class NodeNavigationController {
    private final NodeNavigationService service;
    public NodeNavigationController(NodeNavigationService service) { this.service = service; }
    public record Destination(String url) { }

    @GetMapping("/nodes/go-to")
    public ResponseEntity<Destination> destination(@RequestParam(defaultValue="") String nodeId,
            @RequestParam String _tenantView,
            @CookieValue(value="viewMode", defaultValue="reader") String mode,
            HttpServletRequest request) {
        final long id;
        try {
            if (!nodeId.matches("[1-9][0-9]{0,18}")) throw new NumberFormatException();
            id = Long.parseLong(nodeId);
        } catch (NumberFormatException failure) {
            return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).build();
        }
        if (!service.exists(id)) return ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build();
        var uri = UriComponentsBuilder.fromPath(request.getContextPath());
        if ("tree".equals(ViewController.normalizeMode(mode))) uri.path("/tree").queryParam("nodeId", id);
        else uri.path("/nodes/" + id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Destination(uri.queryParam("_tenantView", _tenantView).build().encode().toUriString()));
    }

    @GetMapping("/nodes/go-to/bookmarks")
    public ResponseEntity<List<BookmarkShortcuts.Shortcut>> bookmarks() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.bookmarks());
    }
}

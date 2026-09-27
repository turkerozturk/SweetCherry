package com.turkerozturk.multipledatabases;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SessionViewController {
    @GetMapping("/session-view")
    public ResponseEntity<Map<String, String>> currentView(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String token = session == null ? null : (String) session.getAttribute(
                TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("view", token == null ? "" : token));
    }
}

package com.turkerozturk.richtext.experimental;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;
import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.multipledatabases.TenantContext;

@Controller
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
public class RichTextPreviewController {
    private final RichTextPreviewService service;
    public RichTextPreviewController(RichTextPreviewService service) { this.service = service; }

    /** Pins direct visits to the current tenant-view token before any database content is loaded. */
    @GetMapping("/nodes/richtext-preview/{nodeId}")
    public String preview(@PathVariable long nodeId, HttpServletRequest request, Model model) {
        var session = request.getSession(false);
        String token = session == null ? null : (String) session.getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN);
        if (token == null) return "redirect:/";
        if (request.getParameter("_tenantView") == null) {
            return "redirect:/nodes/richtext-preview/" + nodeId + "?_tenantView="
                    + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        }
        model.addAttribute("preview", service.compare(nodeId, token));
        model.addAttribute("tenantViewToken", token);
        return "node/richtext-preview";
    }
}

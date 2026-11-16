package com.turkerozturk.image.attachments.word;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller @RequiresTenant
public class WordPreviewController {
    private final WordPreviewService service;
    public WordPreviewController(WordPreviewService service) { this.service = service; }

    @GetMapping("/attachments/{nodeId}/{offset}/word")
    public String preview(@PathVariable long nodeId, @PathVariable int offset,
            @RequestParam String _tenantView,
            @CookieValue(value = "viewMode", defaultValue = "tree") String viewMode,
            Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        model.addAttribute("nodeId", nodeId); model.addAttribute("offset", offset);
        try { model.addAttribute("word", service.preview(nodeId, offset)); }
        catch (WordPreviewService.Problem error) {
            response.setStatus(switch (error.code()) { case "missing" -> 404; case "request" -> 400; default -> 422; });
            model.addAttribute("wordError", error.code());
        }
        return "mobile".equals(viewMode) ? "attachment/wordMobile" : "attachment/word";
    }
}

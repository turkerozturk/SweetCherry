package com.turkerozturk.image.attachments.excel;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Locale;

@Controller @RequiresTenant
public class ExcelPreviewController {
    private final ExcelPreviewService service;
    public ExcelPreviewController(ExcelPreviewService service) { this.service = service; }

    @GetMapping("/attachments/{nodeId}/{offset}/excel")
    public String preview(@PathVariable long nodeId, @PathVariable int offset,
            @RequestParam String _tenantView, @RequestParam(defaultValue = "0") int sheet,
            @RequestParam(defaultValue = "1") int page,
            @CookieValue(value = "viewMode", defaultValue = "tree") String viewMode,
            Locale locale, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        model.addAttribute("nodeId", nodeId); model.addAttribute("offset", offset);
        try { model.addAttribute("excel", service.preview(nodeId, offset, sheet, page, locale)); }
        catch (ExcelPreviewService.Problem error) {
            response.setStatus(switch (error.code()) { case "missing" -> 404; case "request" -> 400; default -> 422; });
            model.addAttribute("excelError", error.code());
        }
        return "mobile".equals(viewMode) ? "attachment/excelMobile" : "attachment/excel";
    }
}

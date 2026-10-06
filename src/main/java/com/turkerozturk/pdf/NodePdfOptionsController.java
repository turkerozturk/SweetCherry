package com.turkerozturk.pdf;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.turkerozturk.multipledatabases.RequiresTenant;

@Controller
@RequiresTenant
public class NodePdfOptionsController {
    static final String PREFERENCES = "NODE_PDF_OPTIONS";
    private final NodePdfExportService service;
    private final PdfSourceMetadata sources;
    public NodePdfOptionsController(NodePdfExportService service, PdfSourceMetadata sources) {
        this.service = service; this.sources = sources;
    }
    @GetMapping("/nodes/pdf/{nodeId}")
    public String page(@PathVariable long nodeId, HttpSession session, Model model) {
        model.addAttribute("nodeId", nodeId);
        model.addAttribute("nodeName", service.title(nodeId));
        model.addAttribute("options", remembered(session));
        return "node/pdfExportOptions";
    }
    @PostMapping("/nodes/pdf/{nodeId}")
    public ResponseEntity<byte[]> download(@PathVariable long nodeId, HttpSession session,
            @RequestParam(defaultValue="A4") String paper, @RequestParam(defaultValue="portrait") String orientation,
            @RequestParam(defaultValue="false") boolean colors, @RequestParam(defaultValue="false") boolean nodeTitle,
            @RequestParam(defaultValue="false") boolean outline, @RequestParam(defaultValue="false") boolean filenameHeader,
            @RequestParam(defaultValue="false") boolean pageNumbers, @RequestParam(defaultValue="false") boolean metadata,
            @RequestParam(defaultValue="false") boolean sourceName, @RequestParam(defaultValue="false") boolean sourceAddress) {
        var options = new PdfExportOptions(paper, orientation, colors, nodeTitle, outline,
                filenameHeader, pageNumbers, metadata, sourceName, sourceAddress);
        var exported = service.export(nodeId, options, sources.current(options));
        session.setAttribute(PREFERENCES, options);
        return PdfFromHtmlController.downloadResponse(exported, nodeId);
    }
    static PdfExportOptions remembered(HttpSession session) {
        var value = session.getAttribute(PREFERENCES);
        return value instanceof PdfExportOptions options ? options : PdfExportOptions.defaults();
    }
}

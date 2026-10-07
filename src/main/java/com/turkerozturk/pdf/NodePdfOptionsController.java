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
        model.addAttribute("objects", rememberedObjects(session));
        return "node/pdfExportOptions";
    }
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

    @GetMapping("/nodes/pdf/{nodeId}/subtree")
    public String subtreePage(@PathVariable long nodeId, HttpSession session, Model model) {
        page(nodeId, session, model);
        model.addAttribute("includeDescendants", true);
        model.addAttribute("contents", Boolean.TRUE.equals(session.getAttribute("NODE_PDF_CONTENTS")));
        return "node/pdfExportOptions";
    }

    public ResponseEntity<byte[]> subtreeDownload(@PathVariable long nodeId, HttpSession session,
            @RequestParam(defaultValue="A4") String paper, @RequestParam(defaultValue="portrait") String orientation,
            @RequestParam(defaultValue="false") boolean colors, @RequestParam(defaultValue="false") boolean nodeTitle,
            @RequestParam(defaultValue="false") boolean outline, @RequestParam(defaultValue="false") boolean filenameHeader,
            @RequestParam(defaultValue="false") boolean pageNumbers, @RequestParam(defaultValue="false") boolean metadata,
            @RequestParam(defaultValue="false") boolean sourceName, @RequestParam(defaultValue="false") boolean sourceAddress,
            @RequestParam(defaultValue="false") boolean contents, java.util.Locale locale) {
        var options = new PdfExportOptions(paper, orientation, colors, nodeTitle, outline,
                filenameHeader, pageNumbers, metadata, sourceName, sourceAddress);
        String contentsTitle = java.util.ResourceBundle.getBundle("messages", locale).getString("pdf.options.contents");
        var exported = service.exportSubtree(nodeId, options, sources.current(options), contents, contentsTitle);
        session.setAttribute(PREFERENCES, options);
        session.setAttribute("NODE_PDF_CONTENTS", contents);
        return PdfFromHtmlController.downloadResponse(exported, nodeId);
    }

    @PostMapping("/nodes/pdf/{nodeId}")
    public ResponseEntity<byte[]> downloadObjects(@PathVariable long nodeId, HttpSession session,
            @RequestParam(defaultValue="A4") String paper, @RequestParam(defaultValue="portrait") String orientation,
            @RequestParam(defaultValue="false") boolean colors, @RequestParam(defaultValue="false") boolean nodeTitle,
            @RequestParam(defaultValue="false") boolean outline, @RequestParam(defaultValue="false") boolean filenameHeader,
            @RequestParam(defaultValue="false") boolean pageNumbers, @RequestParam(defaultValue="false") boolean metadata,
            @RequestParam(defaultValue="false") boolean sourceName, @RequestParam(defaultValue="false") boolean sourceAddress,
            @RequestParam(defaultValue="false") boolean images, @RequestParam(defaultValue="false") boolean codeboxes,
            @RequestParam(defaultValue="false") boolean tables, @RequestParam(defaultValue="false") boolean latex) {
        var objects = new com.turkerozturk.richtext.experimental.PdfObjectSelection(images, codeboxes, tables, latex);
        var options = new PdfExportOptions(paper, orientation, colors, nodeTitle, outline,
                filenameHeader, pageNumbers, metadata, sourceName, sourceAddress);
        var exported = service.export(nodeId, options, sources.current(options), objects);
        remember(session, options, objects);
        return PdfFromHtmlController.downloadResponse(exported, nodeId);
    }

    @PostMapping("/nodes/pdf/{nodeId}/subtree")
    public ResponseEntity<byte[]> subtreeDownloadObjects(@PathVariable long nodeId, HttpSession session,
            @RequestParam(defaultValue="A4") String paper, @RequestParam(defaultValue="portrait") String orientation,
            @RequestParam(defaultValue="false") boolean colors, @RequestParam(defaultValue="false") boolean nodeTitle,
            @RequestParam(defaultValue="false") boolean outline, @RequestParam(defaultValue="false") boolean filenameHeader,
            @RequestParam(defaultValue="false") boolean pageNumbers, @RequestParam(defaultValue="false") boolean metadata,
            @RequestParam(defaultValue="false") boolean sourceName, @RequestParam(defaultValue="false") boolean sourceAddress,
            @RequestParam(defaultValue="false") boolean contents,
            @RequestParam(defaultValue="false") boolean images, @RequestParam(defaultValue="false") boolean codeboxes,
            @RequestParam(defaultValue="false") boolean tables, @RequestParam(defaultValue="false") boolean latex,
            java.util.Locale locale) {
        var objects = new com.turkerozturk.richtext.experimental.PdfObjectSelection(images, codeboxes, tables, latex);
        var options = new PdfExportOptions(paper, orientation, colors, nodeTitle, outline,
                filenameHeader, pageNumbers, metadata, sourceName, sourceAddress);
        var exported = service.exportSubtree(nodeId, options, sources.current(options), contents,
                java.util.ResourceBundle.getBundle("messages", locale).getString("pdf.options.contents"), objects);
        remember(session, options, objects);
        session.setAttribute("NODE_PDF_CONTENTS", contents);
        return PdfFromHtmlController.downloadResponse(exported, nodeId);
    }

    private void remember(HttpSession session, PdfExportOptions options,
            com.turkerozturk.richtext.experimental.PdfObjectSelection objects) {
        session.setAttribute(PREFERENCES, options);
        session.setAttribute("NODE_PDF_OBJECTS", objects);
    }
    static com.turkerozturk.richtext.experimental.PdfObjectSelection rememberedObjects(HttpSession session) {
        var value = session.getAttribute("NODE_PDF_OBJECTS");
        return value instanceof com.turkerozturk.richtext.experimental.PdfObjectSelection objects
                ? objects : com.turkerozturk.richtext.experimental.PdfObjectSelection.all();
    }
    static PdfExportOptions remembered(HttpSession session) {
        var value = session.getAttribute(PREFERENCES);
        return value instanceof PdfExportOptions options ? options : PdfExportOptions.defaults();
    }
}

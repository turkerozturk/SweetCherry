package com.turkerozturk.multipledatabases.info;

import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.node.links.InternalLinkReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiresTenant
public class DatabaseInfoController {
    private final DatabaseInfoService infoService;
    private final InternalLinkReportService links;

    public DatabaseInfoController(DatabaseInfoService infoService, InternalLinkReportService links) {
        this.infoService = infoService;
        this.links = links;
    }

    @GetMapping("/database-info")
    public String show(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "broken") String status,
            @RequestParam(defaultValue = "50") String limit,
            @RequestParam(defaultValue = "1") String page, Model model) {
        InternalLinkReportService.Options options;
        try {
            options = new InternalLinkReportService.Options(q, status, Integer.parseInt(limit), Integer.parseInt(page));
        } catch (IllegalArgumentException invalid) {
            options = new InternalLinkReportService.Options("", "broken", 50, 1);
            model.addAttribute("invalidOptions", true);
        }
        model.addAttribute("databaseInfo", infoService.inspect());
        model.addAttribute("report", links.inspect(options));
        model.addAttribute("reportPath", "/database-info");
        return "database-info";
    }
}

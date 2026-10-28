package com.turkerozturk.node.links;

import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiresTenant
public class InternalLinkReportController {
    private final InternalLinkReportService service;
    public InternalLinkReportController(InternalLinkReportService service){this.service=service;}
    @GetMapping("/nodes/internal-links")
    public String show(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="all")String status,
            @RequestParam(defaultValue="50")String limit,@RequestParam(defaultValue="1")String page,Model model) {
        InternalLinkReportService.Options options;
        try{options=new InternalLinkReportService.Options(q,status,Integer.parseInt(limit),Integer.parseInt(page));}
        catch(IllegalArgumentException invalid){options=new InternalLinkReportService.Options("","all",50,1);model.addAttribute("invalidOptions",true);}
        model.addAttribute("report",service.inspect(options));
        return "node/internal-links";
    }
}

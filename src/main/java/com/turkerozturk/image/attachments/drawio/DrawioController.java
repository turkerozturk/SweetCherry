package com.turkerozturk.image.attachments.drawio;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller @RequiresTenant
public class DrawioController {
    private final DrawioLibraries libraries;private final DrawioService service;private final java.util.concurrent.Semaphore readers=new java.util.concurrent.Semaphore(2);
    public DrawioController(DrawioService service,DrawioLibraries libraries){this.service=service;this.libraries=libraries;}
    @GetMapping("/attachments/{nodeId}/{offset}/drawio")
    public String page(@PathVariable long nodeId,@PathVariable int offset,@RequestParam String _tenantView,@CookieValue(value="viewMode",defaultValue="tree") String view,Model model,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");model.addAttribute("nodeId",nodeId);model.addAttribute("offset",offset);return "mobile".equals(view)?"attachment/drawioMobile":"attachment/drawio";
    }
    @GetMapping("/attachments/{nodeId}/{offset}/drawio/data") @ResponseBody
    public ResponseEntity<DrawioService.Preview> data(@PathVariable long nodeId,@PathVariable int offset,@RequestParam String _tenantView){
        if(!readers.tryAcquire())return ResponseEntity.status(429).cacheControl(CacheControl.noStore()).build();
        try{return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.preview(nodeId,offset));}finally{readers.release();}
    }
    @GetMapping("/attachments/drawio/frame")
    public String frame(@RequestParam String _tenantView,Model model,HttpServletResponse response){
        model.addAttribute("drawioLibraries",String.join(",",libraries.enabled()));model.addAttribute("drawioIconScripts",libraries.iconScripts());
        String nonce=java.util.UUID.randomUUID().toString();model.addAttribute("drawioNonce",nonce);
        response.setHeader("Cache-Control","no-store");response.setHeader("X-Content-Type-Options","nosniff");
        response.setHeader("Content-Security-Policy","default-src 'none'; script-src 'nonce-"+nonce+"'; style-src 'unsafe-inline'; img-src data:; font-src 'none'; connect-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'self'; sandbox allow-scripts");
        return "attachment/drawioFrame";
    }
    @ExceptionHandler(DrawioService.Problem.class) public ResponseEntity<java.util.Map<String,Object>> failure(DrawioService.Problem problem){return ResponseEntity.status(switch(problem.code()){case "missing"->404;case "request"->400;default->422;}).cacheControl(CacheControl.noStore()).body(java.util.Map.of("error",problem.code(),"limit",problem.limit(),"reason",problem.reason(),"page",problem.page(),"line",problem.line(),"column",problem.column()));}
}

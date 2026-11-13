package com.turkerozturk.node.links;
import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Controller @RequiresTenant
public class LinkGraphController {
    private final LinkGraphService service;
    public LinkGraphController(LinkGraphService service){this.service=service;}
    @GetMapping("/nodes/link-graph") public String page(@RequestParam(defaultValue="0") long nodeId,Model model,HttpServletResponse response){
        if(nodeId<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        response.setHeader("Cache-Control","no-store");model.addAttribute("graphNodeId",Long.toString(nodeId));return "node/link-graph";
    }
    @GetMapping("/nodes/link-graph/data") @ResponseBody
    public ResponseEntity<?> data(@RequestParam(defaultValue="0") long nodeId,@RequestParam String _tenantView,
            @RequestParam(defaultValue="false") boolean hierarchy,@RequestParam(defaultValue="40") int limit,@RequestParam(defaultValue="false") boolean refresh){
        try{return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.graph(nodeId,_tenantView,hierarchy,limit,refresh));}
        catch(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("error","unavailable"));}
        catch(RuntimeException error){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Link graph unavailable: {}",error.getClass().getSimpleName());return ResponseEntity.status(503).cacheControl(CacheControl.noStore()).body(Map.of("error","unavailable"));}
    }
}

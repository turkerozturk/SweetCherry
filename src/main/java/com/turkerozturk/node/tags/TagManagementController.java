package com.turkerozturk.node.tags;

import com.turkerozturk.multipledatabases.*;
import jakarta.servlet.http.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequiresTenant @PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/nodes/tags/manage")
public class TagManagementController {
    static final String PLAN="CTB_TAG_MANAGEMENT_PLAN";
    private record Plan(String token,String source,String view,Instant expires,TagManagementService.Operation operation,String revision) {}
    private final TagManagementService service;private final NodeTagService catalogue;private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public TagManagementController(TagManagementService service,NodeTagService catalogue){this(service,catalogue,Clock.systemUTC());}
    TagManagementController(TagManagementService service,NodeTagService catalogue,Clock clock){this.service=service;this.catalogue=catalogue;this.clock=clock;}
    @GetMapping
    public String page(HttpSession session,Model model,HttpServletResponse response){
        service.requireWritable();session.removeAttribute(PLAN);response.setHeader("Cache-Control","no-store");
        model.addAttribute("tagChoices",catalogue.catalogue());return "node/tag-management";
    }
    @PostMapping("/preview")
    public String preview(@RequestParam String action,@RequestParam String from,@RequestParam(defaultValue="")String to,
            @RequestParam("_tenantView")String view,HttpSession session,Model model,RedirectAttributes flash,HttpServletResponse response){
        service.requireWritable();response.setHeader("Cache-Control","no-store");session.removeAttribute(PLAN);
        try{
            var operation=new TagManagementService.Operation(TagManagementService.Action.valueOf(action),from,to);
            var preview=service.preview(operation);String token=UUID.randomUUID().toString();
            synchronized(session){session.setAttribute(PLAN,new Plan(token,TenantContext.getCurrentTenant(),view,clock.instant().plusSeconds(600),operation,preview.revision()));}
            model.addAttribute("tagChange",preview);model.addAttribute("confirmationToken",token);return "node/tag-management-preview";
        }catch(TagManagementProblem problem){flash.addFlashAttribute("tagManagementError",problem.key());}
        catch(IllegalArgumentException invalid){flash.addFlashAttribute("tagManagementError","tags.manage.error.invalid");}
        return "redirect:/nodes/tags/manage";
    }
    @PostMapping("/confirm")
    public String confirm(@RequestParam(defaultValue="")String token,@RequestParam("_tenantView")String view,HttpSession session,RedirectAttributes flash){
        service.requireWritable();
        try{
            Plan plan;
            synchronized(session){
                Object stored=session.getAttribute(PLAN);
                if(!(stored instanceof Plan p)||!p.token().equals(token)||!p.view().equals(view)||!Objects.equals(p.source(),TenantContext.getCurrentTenant())||!p.expires().isAfter(clock.instant()))throw new TagManagementProblem("expired");
                plan=p;session.removeAttribute(PLAN);
            }
            flash.addFlashAttribute("tagManagementResult",service.apply(plan.operation(),plan.revision()));return "redirect:/nodes/tags";
        }catch(TagManagementProblem problem){flash.addFlashAttribute("tagManagementError",problem.key());}
        catch(RuntimeException failure){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Tag management transaction failed; no success result was issued.",failure);flash.addFlashAttribute("tagManagementError","tags.manage.error.failed");}
        return "redirect:/nodes/tags/manage";
    }
}

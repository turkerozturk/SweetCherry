package com.turkerozturk.node.reminders;

import com.turkerozturk.multipledatabases.RequiresTenant;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.servlet.http.*;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiresTenant
public class ReminderController {
    private final ReminderService service;private final NodePropertiesService properties;
    public ReminderController(ReminderService service,NodePropertiesService properties){this.service=service;this.properties=properties;}
    @GetMapping("/nodes/reminders")
    public String list(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="pending")String status,
            @RequestParam(defaultValue="50")String limit,@RequestParam(defaultValue="1")String page,HttpServletRequest request,HttpServletResponse response,Model model){
        ReminderService.Options options;
        try{options=options(q,status,limit,page);}catch(IllegalArgumentException invalid){options=new ReminderService.Options("","pending",50,1);model.addAttribute("invalidOptions",true);}
        response.setHeader("Cache-Control","no-store");model.addAttribute("reminders",service.inspect(options));
        model.addAttribute("canEditReminders",request.isUserInRole("ADMIN")&&properties.writable());return "node/reminders";
    }
    @GetMapping("/nodes/reminders/new/{nodeId}") @PreAuthorize("hasRole('ADMIN')")
    public String create(@PathVariable long nodeId,HttpServletResponse response,Model model){response.setHeader("Cache-Control","no-store");model.addAttribute("editor",service.editor("",nodeId));return "node/reminder-edit";}
    @GetMapping("/nodes/reminders/edit/{id}") @PreAuthorize("hasRole('ADMIN')")
    public String edit(@PathVariable String id,HttpServletResponse response,Model model){response.setHeader("Cache-Control","no-store");model.addAttribute("editor",service.editor(id,0));return "node/reminder-edit";}
    @PostMapping("/nodes/reminders/save") @PreAuthorize("hasRole('ADMIN')")
    public String save(@RequestParam(defaultValue="")String id,@RequestParam long nodeId,@RequestParam long contentId,
            @RequestParam String localTime,@RequestParam String zone,@RequestParam(defaultValue="")String message,
            @RequestParam(defaultValue="none")String repeat,@RequestParam(defaultValue="feb28")String leapDay,
            @RequestParam String revision,@RequestParam(defaultValue="")String occurrence,@RequestParam String _tenantView,HttpServletResponse response,Model model,RedirectAttributes flash){
        response.setHeader("Cache-Control","no-store");
        try{service.save(id,nodeId,contentId,localTime,zone,message,repeat,leapDay,revision,occurrence);}catch(ResponseStatusException invalid){
            if(invalid.getStatusCode().value()!=400||invalid.getReason()==null||!Set.of("invalidTime","invalidInput").contains(invalid.getReason()))throw invalid;
            var editor=service.editor(id,nodeId);if(editor.contentId()!=contentId)throw ReminderStore.problem("targetMissing");model.addAttribute("editor",editor.input(localTime,zone,message,revision,repeat,leapDay,occurrence));model.addAttribute("reminderError",invalid.getReason());response.setStatus(400);return "node/reminder-edit";
        }
        flash.addFlashAttribute("reminderSaved",true);return redirect(_tenantView,new ReminderService.Options("","all",50,1));
    }
    @PostMapping("/nodes/reminders/{id}/state") @PreAuthorize("hasRole('ADMIN')")
    public String state(@PathVariable String id,@RequestParam String state,@RequestParam String revision,@RequestParam String _tenantView,
            @RequestParam(defaultValue="")String q,@RequestParam(defaultValue="pending")String status,@RequestParam(defaultValue="50")String limit,@RequestParam(defaultValue="1")String page,@RequestParam(defaultValue="")String occurrence,RedirectAttributes flash){
        ReminderService.Options options;try{options=options(q,status,limit,page);}catch(IllegalArgumentException invalid){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalidInput");}
        service.changeState(id,state,revision,occurrence);flash.addFlashAttribute("reminderSaved",true);return redirect(_tenantView,options);
    }
    private ReminderService.Options options(String q,String status,String limit,String page){return new ReminderService.Options(q,status,Integer.parseInt(limit),Integer.parseInt(page));}
    private String redirect(String token,ReminderService.Options options){return "redirect:"+UriComponentsBuilder.fromPath("/nodes/reminders").queryParam("q",options.q()).queryParam("status",options.status()).queryParam("limit",options.limit()).queryParam("page",options.page()).queryParam("_tenantView",token).build().encode().toUriString();}
    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView problem(ResponseStatusException error,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");String reason=error.getReason();
        if(reason==null||!Set.of("storeInvalid","changed","readonly","missing","targetMissing","invalidInput","invalidTime","limit").contains(reason))reason="changed";
        var result=new ModelAndView("node/reminder-error");result.setStatus(error.getStatusCode());result.addObject("reminderError",reason);return result;
    }
}

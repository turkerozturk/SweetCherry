package com.turkerozturk.settings.editing;

import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ApplicationSettingsController {
    private final ApplicationSettingsService service;
    public ApplicationSettingsController(ApplicationSettingsService service){this.service=service;}
    @GetMapping("/system/settings")
    public String form(Model model,HttpServletResponse response){return display(model,response);}
    @PostMapping("/system/settings")
    public String save(@RequestParam Map<String,String> fields,Model model,HttpServletResponse response){
        try {Map<String,String> values=new LinkedHashMap<>();for(var setting:ApplicationSettingsService.SETTINGS)values.put(setting.key(),fields.get(setting.key()));
            // Additional fields cannot become arbitrary YAML keys.
            service.save(fields.get("revision"),values);model.addAttribute("notice","appsettings.saved");
        }catch(IllegalArgumentException error){model.addAttribute("notice","appsettings.invalid");}
        catch(Exception error){model.addAttribute("notice","appsettings.unavailable");}
        return display(model,response);
    }
    private String display(Model model,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");model.addAttribute("settings",ApplicationSettingsService.SETTINGS);
        try{var snapshot=service.read();model.addAttribute("snapshot",snapshot);}
        catch(Exception error){model.addAttribute("notice","appsettings.unavailable");}
        return "applicationSettings";
    }
}

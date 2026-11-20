package com.turkerozturk.backup;
import com.turkerozturk.multipledatabases.DatabaseSwitchController;
import com.turkerozturk.settings.editing.ApplicationSettingsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
@ControllerAdvice(assignableTypes={DatabaseSwitchController.class,CtbBackupController.class})
public class AutomaticBackupTenantAdvice {
    private final CtbBackupSettings settings;private final ApplicationSettingsService configuration;
    public AutomaticBackupTenantAdvice(CtbBackupSettings settings,ApplicationSettingsService configuration){this.settings=settings;this.configuration=configuration;}
    @ModelAttribute public void status(Model model){
        model.addAttribute("automaticBackupTenants",settings.getAutomatic().getTenants());
        try{model.addAttribute("automaticBackupRevision",configuration.read().revision());}
        catch(Exception unavailable){model.addAttribute("automaticBackupRevision",null);}
    }
}

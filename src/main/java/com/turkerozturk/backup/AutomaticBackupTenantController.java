package com.turkerozturk.backup;
import com.turkerozturk.settings.editing.ApplicationSettingsService;
import com.turkerozturk.multipledatabases.TenantService;
import java.io.*;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @PreAuthorize("hasRole('ADMIN')")
public class AutomaticBackupTenantController {
    private final ApplicationSettingsService configuration;
    private final CtbBackupScheduler scheduler;
    private final TenantService tenants;
    public AutomaticBackupTenantController(ApplicationSettingsService configuration,CtbBackupScheduler scheduler,TenantService tenants){this.configuration=configuration;this.scheduler=scheduler;this.tenants=tenants;}
    @PostMapping("/tenants/backups/automatic/remove")
    public String remove(@RequestParam String tenant,@RequestParam String revision,RedirectAttributes flash){
        try{
            if(!tenants.getAllTenants().containsKey(tenant))throw new IllegalArgumentException("invalid");
            scheduler.removeTenant(tenant,()->{try{configuration.edit(revision,yaml->AutomaticBackupTenantList.remove(yaml,tenant));}catch(IOException error){throw new UncheckedIOException(error);}});
            flash.addFlashAttribute("automaticBackupNotice","backupTenant.removed");
        }catch(IllegalArgumentException conflict){flash.addFlashAttribute("automaticBackupNotice","backupTenant.changed");}
        catch(UncheckedIOException unavailable){flash.addFlashAttribute("automaticBackupNotice","backupTenant.unavailable");}
        return "redirect:/tenants";
    }
}

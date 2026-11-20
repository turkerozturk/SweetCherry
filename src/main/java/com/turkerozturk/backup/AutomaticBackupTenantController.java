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
    @PostMapping("/system/backups/automatic/enable")
    @com.turkerozturk.multipledatabases.RequiresTenant
    public String enable(@RequestParam String revision,RedirectAttributes flash){
        try{
            String tenant=com.turkerozturk.multipledatabases.TenantContext.getCurrentTenant();
            if(tenant==null||!tenants.getAllTenants().containsKey(tenant))throw new IllegalArgumentException("invalid");
            scheduler.enableTenant(tenant,()->{try{configuration.edit(revision,yaml->AutomaticBackupTenantList.enable(yaml,tenant));}catch(IOException error){throw new UncheckedIOException(error);}});
            flash.addFlashAttribute("automaticBackupNotice","backupTenant.enabled");
        }catch(CtbBackupProblem unavailable){flash.addFlashAttribute("automaticBackupNotice",unavailable.key());}
        catch(IllegalArgumentException conflict){flash.addFlashAttribute("automaticBackupNotice","backupTenant.changed");}
        catch(UncheckedIOException unavailable){flash.addFlashAttribute("automaticBackupNotice","backupTenant.unavailable");}
        return "redirect:/system/backups";
    }
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

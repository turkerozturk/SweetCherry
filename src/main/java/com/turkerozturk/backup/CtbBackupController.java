package com.turkerozturk.backup;

import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.multipledatabases.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/system/backups")
@PreAuthorize("hasRole('ADMIN')")
public class CtbBackupController {
    private final CtbBackupSettings settings;
    private final CtbBackupService service;
    private final CtbBackupStore store;
    private final CtbBackupEngine engine;
    private final TenantService tenants;
    private final CtbBackupScheduler automatic;
    public CtbBackupController(CtbBackupSettings settings,CtbBackupService service,CtbBackupStore store,CtbBackupEngine engine,TenantService tenants) {
        this(settings,service,store,engine,tenants,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public CtbBackupController(CtbBackupSettings settings,CtbBackupService service,CtbBackupStore store,CtbBackupEngine engine,TenantService tenants,CtbBackupScheduler automatic) {
        this.settings=settings;this.service=service;this.store=store;this.engine=engine;this.tenants=tenants;this.automatic=automatic;
    }
    @GetMapping public String page(Model model,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        model.addAttribute("backupEnabled",settings.isEnabled());model.addAttribute("backupCurrent",service.current());
        model.addAttribute("backupDirectory",store.directory());
        if(automatic!=null)model.addAttribute("backupAutomatic",automatic.view());
        model.addAttribute("backups",List.of());model.addAttribute("backupInvalid",0);
        try {var catalog=store.catalog();model.addAttribute("backups",catalog.backups());model.addAttribute("backupInvalid",catalog.invalid());}
        catch(IOException|RuntimeException failure){model.addAttribute("backupError","ctbBackup.error.storage");}
        model.addAttribute("backupSelected",TenantContext.getCurrentTenant());
        return "backup/ctbBackups";
    }
    @PostMapping("/start") @RequiresTenant
    public String start(RedirectAttributes flash) {
        try {String tenant=TenantContext.getCurrentTenant();service.start(tenants.getAllTenants().get(tenant),tenant);}
        catch(CtbBackupProblem problem){flash.addFlashAttribute("backupError",problem.key());}
        return "redirect:/system/backups";
    }
    @GetMapping("/status/{id}") @ResponseBody
    public ResponseEntity<CtbBackupService.Status> status(@PathVariable String id) {
        try{return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.status(id));}
        catch(CtbBackupProblem missing){return ResponseEntity.notFound().build();}
    }
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id,RedirectAttributes flash) throws IOException {
        try {
            var backup=store.checked(id);
            var stream=Files.newInputStream(store.file(id),StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(backup.fileName()).build().toString())
                    .header("X-Content-Type-Options","nosniff").header("X-Backup-SHA256",backup.sha256())
                    .contentLength(backup.bytes()).body(new InputStreamResource(stream));
        } catch(CtbBackupProblem|IOException problem) {
            flash.addFlashAttribute("backupError",problem instanceof CtbBackupProblem p?p.key():"ctbBackup.error.storage");
            return ResponseEntity.status(HttpStatus.SEE_OTHER).location(java.net.URI.create("/system/backups")).build();
        }
    }
    @PostMapping("/{id}/open")
    public String open(@PathVariable String id,Model model,RedirectAttributes flash,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        try {
            var backup=store.get(id);Path copy=store.workingCopy(id,engine);
            var wizard=new TenantWizardForm();wizard.path=copy.toString();
            wizard.name="Backup "+DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss").withZone(ZoneId.systemDefault()).format(Instant.parse(backup.created()))+" "+id.substring(id.length()-8);
            wizard.writable=false;wizard.allowLegacySchemaUpgrade=false;
            model.addAttribute("wizard",wizard);model.addAttribute("backupWorkingCopy",true);
            model.addAttribute("databaseIconOptions",Arrays.stream(NodeIcon.values()).filter(icon->!icon.getIconName().equals("zero"))
                    .sorted(Comparator.comparing(NodeIcon::getIconNameForHuman,String.CASE_INSENSITIVE_ORDER)).toList());
            return "tenantWizard";
        } catch(Exception problem) {
            flash.addFlashAttribute("backupError",problem instanceof CtbBackupProblem p?p.key():"ctbBackup.error.storage");
            return "redirect:/system/backups";
        }
    }
}

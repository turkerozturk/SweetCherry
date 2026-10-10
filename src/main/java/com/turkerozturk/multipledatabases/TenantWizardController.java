package com.turkerozturk.multipledatabases;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class TenantWizardController {
    private final TenantWizardService service;
    private final DatabaseSwitchController selection;
    public TenantWizardController(TenantWizardService service, DatabaseSwitchController selection){this.service=service;this.selection=selection;}
    @InitBinder("wizard")
    public void bind(org.springframework.web.bind.WebDataBinder binder){binder.setAllowedFields("tenant","revision","name","type","path","url","username","password","clearPassword","role","writable","allowLegacySchemaUpgrade","newNodeName","newNodeTags","maxEmbeddedFileSizeMB");}
    @GetMapping("/tenants/wizard")
    public String form(@RequestParam(required=false) String tenant, Model model,HttpServletResponse response)throws IOException {
        response.setHeader("Cache-Control","no-store");
        try { model.addAttribute("wizard",tenant==null?new TenantWizardForm():service.edit(tenant)); }
        catch(IllegalArgumentException error) {model.addAttribute("wizard",new TenantWizardForm());model.addAttribute("wizardError","wizard.unsupportedDriver");}
        return "tenantWizard";
    }
    @PostMapping("/tenants/wizard")
    public String save(@ModelAttribute("wizard") TenantWizardForm form,org.springframework.validation.BindingResult binding,Model model,HttpServletRequest request,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        try {
            if(binding.hasErrors())throw new IllegalArgumentException("wizard.invalidConfig");
            String name=service.save(form);
            return selection.setTenant(name,request);
        } catch(IllegalArgumentException error) {
            String key=error.getMessage(); model.addAttribute("wizardError",key!=null&&key.startsWith("wizard.")?key:"wizard.invalidConfig");
        } catch(IllegalStateException error) {model.addAttribute("wizardError","wizard.busy");}
        catch(IOException error){model.addAttribute("wizardError","wizard.ioError");}
        form.password=""; return "tenantWizard";
    }
    public record Entry(String name,String path,boolean directory){}
    public record Listing(String path,String parent,List<Entry> entries,boolean truncated){}
    /** Lists server files only; no upload and no arbitrary file-content/download endpoint. */
    @GetMapping("/tenants/wizard/browse") @ResponseBody
    public ResponseEntity<Listing> browse(@RequestParam(defaultValue="") String path) {
        try { return listing(path); }
        catch(IOException | InvalidPathException error) {return ResponseEntity.badRequest().cacheControl(org.springframework.http.CacheControl.noStore()).build();}
    }
    private ResponseEntity<Listing> listing(String path)throws IOException {
        if(path.isBlank()) {
            Set<Path> roots=new LinkedHashSet<>();for(File root:File.listRoots())roots.add(root.toPath());
            roots.add(Path.of(System.getProperty("user.home")));roots.add(Path.of(".").toAbsolutePath().normalize());
            return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(new Listing("","",roots.stream().map(p->new Entry(p.toString(),p.toString(),true)).toList(),false));
        }
        Path folder=Path.of(path).toRealPath();if(!Files.isDirectory(folder))throw new IOException("Not a directory");
        List<Entry> entries=new ArrayList<>();boolean truncated=false;
        try(var files=Files.newDirectoryStream(folder)) {
            for(Path item:files) {
                boolean directory=Files.isDirectory(item);
                if(directory||Files.isRegularFile(item)&&item.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ctb")) {
                    if(entries.size()>=500){truncated=true;break;}
                    entries.add(new Entry(item.getFileName().toString(),item.toString(),directory));
                }
            }
        }
        entries.sort(Comparator.comparing(Entry::directory).reversed().thenComparing(Entry::name,String.CASE_INSENSITIVE_ORDER));
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(new Listing(folder.toString(),folder.getParent()==null?"":folder.getParent().toString(),entries,truncated));
    }
}

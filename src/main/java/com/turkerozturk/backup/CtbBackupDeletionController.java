package com.turkerozturk.backup;

import jakarta.servlet.http.*;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/system/backups/delete")
@PreAuthorize("hasRole('ADMIN')")
public class CtbBackupDeletionController {
    private final CtbBackupDeletionService service;
    public CtbBackupDeletionController(CtbBackupDeletionService service){this.service=service;}
    @PostMapping("/preview")
    public String preview(@RequestParam(required=false) List<String> ids,HttpSession session,Model model,RedirectAttributes flash,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        try {model.addAttribute("deletion",service.preview(ids,session));return "backup/ctbBackupDelete";}
        catch(Exception failure){flash.addFlashAttribute("backupError",failure instanceof CtbBackupProblem p?p.key():"ctbBackup.error.storage");return "redirect:/system/backups";}
    }
    @PostMapping("/confirm")
    public String confirm(@RequestParam(defaultValue="") String token,HttpSession session,RedirectAttributes flash) {
        try {flash.addFlashAttribute("backupDeletion",service.confirm(token,session));}
        catch(CtbBackupProblem failure){flash.addFlashAttribute("backupError",failure.key());}
        return "redirect:/system/backups";
    }
}

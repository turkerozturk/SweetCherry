package com.turkerozturk.importing.dtt;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiresTenant
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/dtt-topic-import")
public class DttTopicImportController {
    private static final String VIEW = "dtt-topic-import";
    private final DttTopicImportService service;
    private final MessageSource messages;

    public DttTopicImportController(DttTopicImportService service, MessageSource messages) {
        this.service = service;
        this.messages = messages;
    }

    @GetMapping
    public String page(Model model, Locale locale, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        if (model.containsAttribute("dttImported")) return VIEW;
        model.addAttribute("rootName", "DailyTopicTracker");
        load(model, locale);
        return VIEW;
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam String rootName, @RequestParam String revision,
                          Model model, Locale locale, RedirectAttributes redirect, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        String root = rootName.strip();
        model.addAttribute("rootName", root.length() <= 510 ? root : "");
        var preview = load(model, locale);
        if (preview == null) return VIEW;
        if (root.isEmpty() || root.codePointCount(0, root.length()) > 255
                || root.codePoints().anyMatch(Character::isISOControl)) {
            model.addAttribute("dttImportError", "dttImport.invalidRoot");
        } else if (!preview.revision().equals(revision)) {
            model.addAttribute("dttImportError", "dttImport.changed");
        } else {
            try {
                var result = service.write(root, preview);
                if (result != null) {
                    redirect.addFlashAttribute("dttImported", result);
                    return "redirect:/dtt-topic-import";
                }
                model.addAttribute("dttImportNotice", "dttImport.duplicate");
            } catch (ResponseStatusException error) {
                model.addAttribute("dttImportError", error.getReason());
            } catch (org.springframework.dao.DataAccessException | org.hibernate.HibernateException
                     | org.springframework.transaction.TransactionException | ArithmeticException error) {
                model.addAttribute("dttImportError", "dttImport.failed");
            }
        }
        return VIEW;
    }

    private DttTopicImportService.Preview load(Model model, Locale locale) {
        try {
            var preview = service.preview(messages.getMessage("dttImport.ungrouped", null, locale),
                    messages.getMessage("dttImport.uncategorized", null, locale));
            model.addAttribute("catalogPreview", preview);
            return preview;
        } catch (ResponseStatusException error) {
            model.addAttribute("dttImportError", error.getReason());
            return null;
        }
    }
}

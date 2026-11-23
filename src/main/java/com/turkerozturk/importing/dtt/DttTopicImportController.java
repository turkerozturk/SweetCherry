package com.turkerozturk.importing.dtt;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import java.util.UUID;
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
    private static final String CONFIRMATION = DttTopicImportController.class.getName() + ".preview";
    private final DttTopicImportService service;
    private final MessageSource messages;

    public DttTopicImportController(DttTopicImportService service, MessageSource messages) {
        this.service = service;
        this.messages = messages;
    }

    @GetMapping
    public String page(@RequestParam(defaultValue = "true") boolean missingOnly,
                       Model model, Locale locale, HttpSession session, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        if (!model.containsAttribute("dttImported")) load(model, locale, session, missingOnly);
        return VIEW;
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam String confirmation, @RequestParam String revision,
                          @RequestParam boolean missingOnly, Model model, Locale locale, HttpSession session,
                          RedirectAttributes redirect, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        boolean current;
        synchronized (session) {
            current = !confirmation.isEmpty() && confirmation.equals(session.getAttribute(CONFIRMATION));
            if (current) session.removeAttribute(CONFIRMATION);
        }
        var plan = load(model, locale, session, missingOnly);
        if (plan == null) return VIEW;
        if (!current || !plan.selected().revision().equals(revision)) {
            model.addAttribute("dttImportError", "dttImport.changed");
            return VIEW;
        }
        try {
            var result = service.write(plan);
            if (result != null) {
                redirect.addFlashAttribute("dttImported", result);
                return "redirect:/dtt-topic-import";
            }
        } catch (ResponseStatusException error) {
            model.addAttribute("dttImportError", error.getReason());
        } catch (org.springframework.dao.DataAccessException | org.hibernate.HibernateException
                 | org.springframework.transaction.TransactionException | ArithmeticException error) {
            model.addAttribute("dttImportError", "dttImport.failed");
        }
        return VIEW;
    }

    private DttTopicImportService.Plan load(Model model, Locale locale, HttpSession session, boolean missingOnly) {
        model.addAttribute("missingOnly", missingOnly);
        try {
            var plan = service.preview(messages.getMessage("dttImport.ungrouped", null, locale),
                    messages.getMessage("dttImport.uncategorized", null, locale), missingOnly);
            String token = UUID.randomUUID().toString();
            session.setAttribute(CONFIRMATION, token);
            model.addAttribute("confirmation", token);
            model.addAttribute("catalogPreview", plan.selected());
            return plan;
        } catch (ResponseStatusException error) {
            model.addAttribute("dttImportError", error.getReason());
            return null;
        }
    }
}

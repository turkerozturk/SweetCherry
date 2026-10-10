package com.turkerozturk.shutdown;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletResponse;
@Controller
public class RestartController {
    @PostMapping("/restartContext") @PreAuthorize("hasRole('ADMIN')")
    public String restart(Model model,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");
        model.addAttribute("started",com.turkerozturk.SweetCherry.requestRestart());return "restartApplication";
    }
}

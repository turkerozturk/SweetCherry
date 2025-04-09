/*
 * This file is part of the SweetCherry project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/SweetCherry
 *
 * Copyright (c) 2024 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.login;

import com.turkerozturk.multipledatabases.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;

/**
  https://www.youtube.com/watch?v=aMd3P_5bB6s
 Spring Security 6: Personalize Your Login Experience
 */
@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    /**
     * https://www.youtube.com/watch?v=aMd3P_5bB6s
     *  Spring Security 6: Personalize Your Login Experience
     * @return
     */
    @GetMapping("/login")
    public String login(HttpServletRequest request) {

        /*
        HttpSession httpSession = request.getSession();
        logger.info("Login page. Current Data source: " + TenantContext.getCurrentTenant());

        //HttpSession session = request.getSession();

        Collections.list(httpSession.getAttributeNames())
                .forEach(name -> logger.info(name + " : " + httpSession.getAttribute(name)));



        TenantContext.setCurrentTenant(null);
        */

        return "login/login";
    }

    /**
     * https://stackoverflow.com/questions/43762260/custom-logout-with-method
     * Do you mean any inside any method, you just want to logout the user.
     * In SpringSecurity Logout just means clearing the Securitycontext.
     * Just Call SecurityContextHolder.clearContext(); would do it.
     * Also you may want to invalidate HttpSession.invalidate();
     * You may also have to consider clearing cookies and stuffs depending on your use case.
     * Or HttpServletRequest.logout() this would also be a cleaner way to do the same.
     *
     * VEYA SecurityConfig.java sinifimda filter chain içerisine asagidaki satirlari eklemek gerekecerkti denemedim;
     * .logout()
     *     .logoutUrl("/logoutUrl")
     *     .logoutSuccessUrl("/index.html?logout=true")
     *     .logoutSuccessHandler(logoutHandler)
     *     .invalidateHttpSession(true)
     *
     *
     * @param request
     * @return
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         @RequestParam(value = "lang", required = false) String lang) {
        HttpSession httpSession = request.getSession(); // chatgpt onerdi.



        TenantContext.setCurrentTenant(null);



        //databaseSessionManager.unbindSession(); // bunu kullanmadim, bulmusken kaybetmemek icin comment ettim. gerek yok.
        TenantContext.setCurrentTenant(null);
        httpSession.setAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT, null); // Bu sayede
        logger.info(String.format("Data Source closed. Session variable %s is cleared.", TenantContext.SESSION_VARIABLE__CURRENT_TENANT));

        SecurityContextHolder.clearContext();

        Collections.list(httpSession.getAttributeNames())
                .forEach(name -> logger.info(name + " : " + httpSession.getAttribute(name)));



        httpSession.invalidate();
        // VEYA HttpServletRequest.logout(); demis linkte, denemedim

/*

        if (lang != null) {
            return "redirect:/login/logout?lang=" + lang;
        }

        return "redirect:/login/logout";

        */

        return "/login/logout";

    }



}

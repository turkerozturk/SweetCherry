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
package com.turkerozturk.multipledatabases;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@Controller
public class DatabaseSwitchController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSwitchController.class);

    @Autowired
    private TenantService tenantService;

    /**
     * bilgi: ONEMLI metod. Secilen tenant ismini oturum degiskenine atayarak veritabaninin degismesini sagliyor.
     *
     * @param tenant
     * @param request
     * @return
     */
    @PostMapping("/setTenant")
    public String setTenant(@RequestParam("tenant") String tenant, HttpServletRequest request) {
        HttpSession session = request.getSession(); // chatgpt onerdi.
        session.setAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT, tenant); // Bu sayede

        //databaseSessionManager.unbindSession(); // bunu kullanmadim, bulmusken kaybetmemek icin comment ettim. gerek yok.
        TenantContext.setCurrentTenant(tenant); // bu sadece bu metod icinde ise yaradi o yuzden yukaridaki gibi session degiskenine atama yaptik ki butun programda ise yarasin, veritabaninin degismisini kullnabilelim diye.
        //databaseSessionManager.bindSession();


        //https://stackoverflow.com/questions/55973220/spring-boot-hibernate-multi-tenancy-transactional-not-working


        // tenantService.printDataSourceDetails();
        //TenantContext.setCurrentTenant(tenant);
        //System.out.println("TENANT DEGISTI: " + tenant);
        return "redirect:/";  // Ana sayfaya yonlendir
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tenants")
    public String listTenants(Model model, HttpServletRequest request) {

        List<TenantForm> tenantForms = tenantService.getTenantForms();

        model.addAttribute("tenants", tenantForms);

        logger.info("Current Tenant Name: \"" + TenantContext.getCurrentTenant() + "\"");
/*
        HttpSession session = request.getSession(); // chatgpt onerdi.
        String currentTenant = (String) session.getAttribute("CURRENT_TENANT");


        model.addAttribute("currentTenantName", currentTenant);
*/

        return "tenants";
    }

}

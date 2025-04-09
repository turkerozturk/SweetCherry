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

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class DataSourceCloseController {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceCloseController.class);


    @Autowired
    private MultitenantConfiguration multitenantConfiguration;

    @PostMapping("/close-datasources")
    public String closeDataSources(ServletRequest request) {

        HttpServletRequest req = (HttpServletRequest) request;

        // String tenantName = req.getHeader("X-TenantID"); // security ve jwt gerektiren ornek. Denemedim.
        // TenantContext.setCurrentTenant(tenantName);
        HttpSession session = req.getSession(); // chatgpt onerdi, session degiskeninde tutmayi. Yoksa setCurrentTenant diger metodlarca anlasilmiyor ve database degismiyor.
        String tenantName = (String) session.getAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT);
        TenantContext.setCurrentTenant(null);
        session.setAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT, null);
        logger.info(String.format("Data Source closed. Session variable %s is cleared.", TenantContext.SESSION_VARIABLE__CURRENT_TENANT));

        return "redirect:/";  // Ana sayfaya yonlendir

    }
}

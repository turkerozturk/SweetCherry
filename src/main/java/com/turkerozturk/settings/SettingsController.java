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
package com.turkerozturk.settings;

import com.turkerozturk.sunandmoon.AstronomyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SettingsController {
    @Value("${myapp.openWebBrowserOnStartup:true}") private boolean openWebBrowserOnStartup;
    @Value("${myapp.debug:false}") private boolean debug;
    @Value("${myapp.syntax-highlighting.enabled:true}") private boolean syntaxHighlightingEnabled;
    @Value("${server.port:8080}") private int serverPort;
    @Value("${server.ssl.enabled:false}") private boolean sslEnabled;
    private final AstronomyService astronomyService;

    public SettingsController(AstronomyService astronomyService) { this.astronomyService = astronomyService; }

    /** Exposes selected non-secret runtime information and configuration guidance for administrators. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/settings")
    public String getSettings(Model model) {
        model.addAttribute("astronomyProperties", astronomyService.astronomyProperties);
        model.addAttribute("openWebBrowserOnStartup", openWebBrowserOnStartup);
        model.addAttribute("debug", debug);
        model.addAttribute("syntaxHighlightingEnabled", syntaxHighlightingEnabled);
        model.addAttribute("serverPort", serverPort);
        model.addAttribute("sslEnabled", sslEnabled);
        return "settings/settings";
    }
}

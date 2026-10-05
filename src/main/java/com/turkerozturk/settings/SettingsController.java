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
    @Value("${server.http.port:8080}") private int httpPort;
    @Value("${server.address:127.0.0.1}") private String serverAddress;
    @Value("${server.forward-headers-strategy:none}") private String forwardHeadersStrategy;
    @Value("${server.servlet.session.timeout:30m}") private String sessionTimeout;
    @Value("${myapp.security.proxy-https-enabled:false}") private boolean proxyHttpsEnabled;
    @Value("${myapp.security.proxy-https-port:443}") private int proxyHttpsPort;
    @Value("${myapp.security.hsts-enabled:false}") private boolean hstsEnabled;
    @Value("${myapp.login.trusted-proxy-address:}") private String trustedProxyAddress;
    @Value("${myapp.exportingFolderName:exportedFiles}") private String exportingFolderName;
    private final AstronomyService astronomyService;

    public SettingsController(AstronomyService astronomyService) { this.astronomyService = astronomyService; }

    /** Exposes selected non-secret runtime information and configuration guidance for administrators. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/settings")
    public String getSettings(Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("astronomyProperties", astronomyService.astronomyProperties);
        model.addAttribute("openWebBrowserOnStartup", openWebBrowserOnStartup);
        model.addAttribute("debug", debug);
        model.addAttribute("syntaxHighlightingEnabled", syntaxHighlightingEnabled);
        model.addAttribute("serverPort", serverPort);
        model.addAttribute("sslEnabled", sslEnabled);
        var values = new java.util.LinkedHashMap<String, Object>();
        values.put("server.address", serverAddress);
        values.put("server.port", serverPort);
        values.put("server.http.port", httpPort);
        values.put("server.ssl.enabled", sslEnabled);
        values.put("server.forward-headers-strategy", forwardHeadersStrategy);
        values.put("server.servlet.session.timeout", sessionTimeout);
        values.put("myapp.security.proxy-https-enabled", proxyHttpsEnabled);
        values.put("myapp.security.proxy-https-port", proxyHttpsPort);
        values.put("myapp.security.hsts-enabled", hstsEnabled);
        values.put("myapp.login.trusted-proxy-address", trustedProxyAddress);
        values.put("myapp.openWebBrowserOnStartup", openWebBrowserOnStartup);
        values.put("myapp.debug", debug);
        values.put("myapp.syntax-highlighting.enabled", syntaxHighlightingEnabled);
        values.put("myapp.exportingFolderName", exportingFolderName);
        values.put("astronomy.enabled", astronomyService.astronomyProperties.isEnabled());
        values.put("astronomy.latitude", astronomyService.astronomyProperties.getLatitude());
        values.put("astronomy.longitude", astronomyService.astronomyProperties.getLongitude());
        values.put("astronomy.timezone", astronomyService.astronomyProperties.getTimezone());
        model.addAttribute("configurationValues", values);
        model.addAttribute("requestAddress", request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort());
        return "settings/settings";
    }
}

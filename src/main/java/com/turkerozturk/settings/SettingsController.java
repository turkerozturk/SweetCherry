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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * https://www.baeldung.com/spring-reloading-properties
 */
@Controller
public class SettingsController {

    @Value("${myapp.openWebBrowserOnStartup:}")
    Boolean openWebBrowserOnStartup;

    @Value("${myapp.debug:}")
    Boolean debug;

    @Autowired
    AstronomyService astronomyService;

    @Value("${server.http.port:}")
    private int httpPort;

    @Value("${server.port:}")
    private int httpsPort;


    @Value("${server.ssl.key-store:}")
    private String keyStore;

    @Value("${server.ssl.key-store-password:}")
    private String keyStorePassword;

    @Value("${server.ssl.key-password:}")
    private String keyPassword;

    @Value("${server.ssl.key-alias:}")
    private String keyAlias;

    @Value("${server.ssl.key-store-type:}")
    private String keyStoreType;

    @Value("${server.ssl.trust-store:}")
    private String trustStore;

    @Value("${server.ssl.trust-store-password:}")
    private String trustStorePassword;

    @Value("${server.ssl.trust-store-type:}")
    private String trustStoreType;

    @Value("${server.ssl.certificate:}")
    private String certificate;

    @Value("${server.ssl.certificate-private-key:}")
    private String certificatePrivateKey;

    public SettingsController(AstronomyService astronomyService) {
        this.astronomyService = astronomyService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/settings")
    public String getSettings(Model model) {

        model.addAttribute("astronomyProperties", astronomyService.astronomyProperties);


        model.addAttribute("openWebBrowserOnStartup", openWebBrowserOnStartup);
        model.addAttribute("debug", debug);

        model.addAttribute("httpPort", httpPort);
        model.addAttribute("httpsPort", httpsPort);


        Map<String, String> sslConfig = new HashMap<>();
        sslConfig.put("key-store", keyStore);
        sslConfig.put("key-store-password", keyStorePassword);
        sslConfig.put("key-password", keyPassword);
        sslConfig.put("key-alias", keyAlias);
        sslConfig.put("key-store-type", keyStoreType);
        sslConfig.put("trust-store", trustStore);
        sslConfig.put("trust-store-password", trustStorePassword);
        sslConfig.put("trust-store-type", trustStoreType);

        model.addAttribute("sslConfig", sslConfig);

        model.addAttribute("certificate", certificate);

        model.addAttribute("certificatePrivateKey", certificatePrivateKey);



        return ("settings/settings");

    }

}

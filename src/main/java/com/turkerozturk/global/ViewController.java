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
package com.turkerozturk.global;

import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class ViewController {

    private static final Logger logger = LoggerFactory.getLogger(ViewController.class);


    @RequestMapping("/changeView")
    public String changeView(@RequestParam String mode,
                             HttpServletResponse response,
                             HttpServletRequest request) {

        mode = normalizeMode(mode);
        Cookie cookie = new Cookie("viewMode", mode);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 1 week
        response.addCookie(cookie);

        String target = SafeRefererRedirect.target(request);
        if ("tree".equals(mode)) {
            target = treeTarget(target);
        } else if (target.equals("/tree") || target.startsWith("/tree?")) {
            target = readerTarget(target);
        }
        return "redirect:" + target;

    }
    /** Migrates historical cookies/options to the two maintained node views. */
    public static String normalizeMode(String mode) {
        return "tree".equals(mode) || "desktop".equals(mode) ? "tree" : "reader";
    }

    /** Preserves a selected real/shared node and its CTB token when entering the workspace. */
    private String treeTarget(String target) {
        if (target.equals("/tree") || target.startsWith("/tree?")) return target;
        var uri = UriComponentsBuilder.fromUriString(target).build();
        String path = uri.getPath();
        String id = path != null && path.matches("/nodes/[0-9]+") ? path.substring(7) : "0";
        try { Long.parseLong(id); } catch (NumberFormatException exception) { id = "0"; }
        var destination = UriComponentsBuilder.fromPath("/tree").queryParam("nodeId", id);
        String token = uri.getQueryParams().getFirst("_tenantView");
        if (token != null) destination.queryParam("_tenantView", token);
        return destination.build().encode().toUriString();
    }

    /** Preserve the selected tree node and its CTB token when entering the mobile reader. */
    private String readerTarget(String target) {
        var query = UriComponentsBuilder.fromUriString(target).build().getQueryParams();
        String id = query.getFirst("nodeId");
        if (id == null) return "/";
        try {
            if (!id.matches("[0-9]+") || Long.parseLong(id) < 0) return "/";
        } catch (NumberFormatException exception) { return "/"; }
        var destination = UriComponentsBuilder.fromPath("/nodes/" + Long.parseLong(id));
        String token = query.getFirst("_tenantView");
        if (token != null) destination.queryParam("_tenantView", token);
        return destination.build().encode().toUriString();
    }
}

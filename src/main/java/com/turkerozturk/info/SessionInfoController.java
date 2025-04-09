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
package com.turkerozturk.info;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Controller
public class SessionInfoController {


    @GetMapping("/sessionInfo")
    public String getNodeAsHtml(Model model, HttpServletRequest request,
                                @CookieValue(value = "viewMode", defaultValue = "mobile") String viewMode) {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpSession session = req.getSession();

        int sessionMaxInactiveInterval = session.getMaxInactiveInterval();
        long sessionCreationTime = session.getCreationTime();
        long sessionLastAccessedTime = session.getLastAccessedTime();
        String sessionId = session.getId();
        Map<String, Object> sessionAttributes = new HashMap<>();
        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);
            sessionAttributes.put(attributeName, attributeValue);
        }

        model.addAttribute("sessionMaxInactiveInterval", sessionMaxInactiveInterval);
        model.addAttribute("sessionCreationTime", sessionCreationTime);
        model.addAttribute("sessionLastAccessedTime", sessionLastAccessedTime);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("sessionAttributes", sessionAttributes);


        // Add servlet context parameters
        ServletContext servletContext = session.getServletContext();



        Map<String, Object> servletContextParams = new HashMap<>();
        servletContextParams.put("Context Path", servletContext.getContextPath());
        servletContextParams.put("Server Info", servletContext.getServerInfo());
        servletContextParams.put("Servlet Context Name", servletContext.getServletContextName());
        servletContextParams.put("Major Version", servletContext.getMajorVersion());
        servletContextParams.put("Minor Version", servletContext.getMinorVersion());
        servletContextParams.put("Effective Major Version", servletContext.getEffectiveMajorVersion());
        servletContextParams.put("Effective Minor Version", servletContext.getEffectiveMinorVersion());

        model.addAttribute("servletContextParams", servletContextParams);


        Map<String, Object> servletContextAttributes = new HashMap<>();
        Enumeration<String> contextAttributeNames = servletContext.getAttributeNames();
        while (contextAttributeNames.hasMoreElements()) {
            String attributeName = contextAttributeNames.nextElement();
            Object attributeValue = servletContext.getAttribute(attributeName);
            servletContextAttributes.put(attributeName, attributeValue);
        }

        model.addAttribute("servletContextAttributes", servletContextAttributes);

        // Get cookies
        Map<String, String> cookies = new HashMap<>();
        Cookie[] cookieArray = request.getCookies();
        if (cookieArray != null) {
            for (Cookie cookie : cookieArray) {
                cookies.put(cookie.getName(), cookie.getValue());
            }
        }

        model.addAttribute("cookies", cookies);


        return "sessionInfo";
    }


        /*
        java.lang.IllegalArgumentException: The 'request','session','servletContext' and 'response' expression utility objects are no longer available by default for template expressions and their use is not recommended. In cases where they are really needed, they should be manually added as context variables.
         */



}

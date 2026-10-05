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
package com.turkerozturk.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);


    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    /** Logs missing pages without a stack trace and exposes only a status and correlation ID. */
    @RequestMapping("/error")
    public String handleError(WebRequest webRequest, Map<String, Object> model) {
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(
                webRequest, ErrorAttributeOptions.defaults());
        Object status = attributes.get("status");
        int statusCode = status instanceof Integer code && code >= 400 && code <= 599 ? code : 500;
        String errorId = java.util.UUID.randomUUID().toString();
        Throwable failure = errorAttributes.getError(webRequest);
        if (statusCode == 404) {
            // Missing resources are normal request outcomes; never log their path or query string.
            logger.info("Page not found: errorId={}, status=404", errorId);
        } else {
            logger.warn("Request failed: errorId={}, status={}", errorId, statusCode, failure);
        }

        org.springframework.http.HttpStatus httpStatus = org.springframework.http.HttpStatus.resolve(statusCode);
        model.put("status", statusCode);
        model.put("error", httpStatus == null ? "Request failed" : httpStatus.getReasonPhrase());
        model.put("errorId", errorId);
        return statusCode == 404 ? "errors/404" : "error";
    }
}

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

    @RequestMapping("/error")
    public String handleError(WebRequest webRequest, Map<String, Object> model) {

        // Get error attributes
        Map<String, Object> errorAttributes = this.errorAttributes.getErrorAttributes(webRequest,
                ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE,
                        ErrorAttributeOptions.Include.MESSAGE,
                        ErrorAttributeOptions.Include.EXCEPTION,
                        ErrorAttributeOptions.Include.BINDING_ERRORS)
        );

        // Add all error attributes to the model
        model.put("timestamp", errorAttributes.get("timestamp"));
        model.put("status", errorAttributes.get("status"));
        model.put("error", errorAttributes.get("error"));
        model.put("error", errorAttributes.get("exception"));
        model.put("error", errorAttributes.get("trace"));
        model.put("message", errorAttributes.get("message"));
        model.put("path", errorAttributes.get("path"));

        /*
        for (String item : errorAttributes.keySet()) {
            System.out.println(item);
        }
        */

        // Check the status code and return appropriate error page
        Integer statusCode = (Integer) errorAttributes.get("status");
        if (statusCode != null && statusCode == 500) {
            return "errors/500"; // Custom 404 error page
        }

        if (statusCode != null && statusCode == 404) {
            logger.info((String) errorAttributes.get("error"));
            logger.info((String) errorAttributes.get("trace"));
            logger.info((String) errorAttributes.get("message"));
            logger.info((String) errorAttributes.get("exception"));
            logger.info((String) errorAttributes.get("path"));

            return "errors/404"; // Custom 404 error page
        }


        //the name of custom error page template
        return "error";
    }

}

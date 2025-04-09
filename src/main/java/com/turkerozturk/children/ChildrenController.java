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
package com.turkerozturk.children;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ChildrenController {
    @Autowired
    private ChildrenService childrenService;

    @GetMapping("/children")
    public String getAllChildrenAsHtml(Model model) {
        model.addAttribute("children", childrenService.getChildren());
        return "children"; // Bu, bir Thymeleaf şablonu olmalıdır: src/main/resources/templates/children.html
    }

    @GetMapping("/father")
    public String getNodesByFatherId(@RequestParam(required = false) Long fatherId, Model model) {
        if (fatherId == null) {
            fatherId = 0L; // Varsayılan olarak 0'a eşitlenir.
        }
        List<NaviNode> nodes = childrenService.getNaviNodesByFatherId(fatherId);
        model.addAttribute("nodes", nodes);
        return "father";
    }


    @GetMapping("/findchildrenbyfatherid")
    public String showSearchForm(Model model) {
        model.addAttribute("fatherId", "");
        return "findchildrenbyfatherid";
    }

}

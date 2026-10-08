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
package com.turkerozturk.export;

import com.turkerozturk.multipledatabases.RequiresTenant;
import java.io.IOException;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiresTenant
public class ExportManipulatedRecursiveController {
    private static final Logger logger = LoggerFactory.getLogger(ExportManipulatedRecursiveController.class);
    private final CtbExportService service;

    public ExportManipulatedRecursiveController(CtbExportService service) { this.service = service; }

    @GetMapping("/expo/{node_id}")
    public String exportPage(@PathVariable("node_id") Integer nodeId) {
        return "redirect:/nodes/" + nodeId;
    }

    @PostMapping("/expo/{node_id}")
    public String export1(@PathVariable("node_id") Integer nodeId,
            @RequestParam(defaultValue = "true") boolean includeDescendants,
            RedirectAttributes redirectAttributes) {
        try {
            var result = service.export(nodeId, includeDescendants, true);
            redirectAttributes.addFlashAttribute("contentText", "Export successful. Exported node count: "
                    + result.count() + ". File: " + result.filename());
        } catch (IOException | SQLException failure) {
            logger.error("CTB export failed", failure);
            redirectAttributes.addFlashAttribute("contentText", "Export failed.");
        }
        return "redirect:/export-result";
    }

    @GetMapping("/export-result")
    public String exportResult(Model model) {
        if (!model.containsAttribute("contentText")) model.addAttribute("contentText", "No export operation was performed.");
        return "expo";
    }

}

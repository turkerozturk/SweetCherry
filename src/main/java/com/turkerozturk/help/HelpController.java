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
package com.turkerozturk.help;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@PreAuthorize("hasRole('ADMIN')")
@Controller
public class HelpController {

    @GetMapping("/help/advanced-search")
    public String getAdvancedSearch() {
        return "help/advanced-search";
    }

    @GetMapping("/help/bookmarks")
    public String getBookmarks() {
        return "help/bookmarks";
    }

    @GetMapping("/help/attachments")
    public String getAtachments() {
        return "help/attachments";
    }

    @GetMapping("/help/custom-lists")
    public String getCustomLists() {
        return "help/custom-lists";
    }

    @GetMapping("/help/information-panel")
    public String getInformationPanel() {
        return "help/information-panel";
    }

    @GetMapping("/help/index")
    public String getIndex() {
        return "help/index";
    }

    @GetMapping("/help/icons")
    public String getIcons() {
        return "help/icons";
    }

    @GetMapping("/help/settings")
    public String getSettings() {
        return "help/settings";
    }

    @GetMapping("/help/about")
    public String getAbout() {
        return "help/about";
    }


    // bilgi onceki helpler TODO duzenle

    @GetMapping("/help/dashboard")
    public String helpDashboard() {
        return "/help/dashboard";
    }

    @GetMapping("/help/cherrytemplatenode")
    public String help() {
        return "/help/cherrytemplatenode";
    }

    @GetMapping("/help/cherrytemplatenode--pdf")
    public String helpPdf() {
        return "/help/cherrytemplatenode--pdf";
    }

    @GetMapping("/help/cherrytemplatetasks")
    public String helpTasks() {
        return "/help/cherrytemplatetasks";
    }


}

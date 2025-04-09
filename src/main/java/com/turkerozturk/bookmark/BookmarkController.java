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
package com.turkerozturk.bookmark;

import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenService;
import com.turkerozturk.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class BookmarkController {
    @Autowired
    private BookmarkService bookmarkService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private ChildrenService childrenService;

    @GetMapping("/bookmarks")
    public String getAllChildrenAsHtml(Model model,
                                       @CookieValue(value = "viewMode", defaultValue = "mobile") String viewMode) {
        List<Bookmark> bookmarks = bookmarkService.getBookmarks();

        if (!bookmarks.isEmpty()) {
            // BASLA shared node correction
            for (Bookmark bookmark : bookmarks) {

                if (bookmark.getNode() != null) {
                    bookmark.getNode().setBreadcrumbs(nodeService.addBreadcrumbs(bookmark.getNode().getNodeId()));
                } else {
                    Children child = childrenService.findById(bookmark.getNodeId());
                    bookmark.setNode(nodeService.findById(child.getMasterId()));
                    bookmark.getNode().setBreadcrumbs(nodeService.addBreadcrumbs(child.getMasterId()));
                }
            }
            // BASLA shared node correction

            model.addAttribute("bookmarks", bookmarks);
        }

        model.addAttribute("viewMode", viewMode);
        if ("mobile".equals(viewMode)) {
            return "bookmarksMobile";
        } else {
            return "bookmarks";
        }

    }

}

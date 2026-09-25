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
import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.ArrayList;

@Controller
public class BookmarkController {
    @Autowired
    private BookmarkService bookmarkService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private ChildrenService childrenService;

    @GetMapping("/bookmarks")
    @RequiresTenant
    public String getAllChildrenAsHtml(Model model,
                                       @CookieValue(value = "viewMode", defaultValue = "mobile") String viewMode) {
        List<Bookmark> bookmarks = bookmarkService.getBookmarks();

        List<Bookmark> available = new ArrayList<>();
        List<Long> missing = new ArrayList<>();
        for (Bookmark bookmark : bookmarks) {
            Children child = childrenService.findById(bookmark.getNodeId());
            if (child == null) {
                missing.add(bookmark.getNodeId());
                continue;
            }
            long realId = child.getMasterId() != null && child.getMasterId() != 0
                    ? child.getMasterId() : child.getNodeId();
            var node = nodeService.findById(realId);
            if (node == null) {
                missing.add(bookmark.getNodeId());
                continue;
            }
            node.setBreadcrumbs(nodeService.addBreadcrumbs(realId));
            bookmark.setNode(node);
            available.add(bookmark);
        }
        model.addAttribute("bookmarks", available);
        model.addAttribute("missingBookmarkIds", missing);

        model.addAttribute("viewMode", viewMode);
        if ("mobile".equals(viewMode)) {
            return "bookmarksMobile";
        } else {
            return "bookmarks";
        }

    }

}

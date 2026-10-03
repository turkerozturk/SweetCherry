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

    @Autowired
    private BookmarkWriteService bookmarkWriteService;

    @GetMapping("/bookmarks")
    @RequiresTenant
    public String getAllChildrenAsHtml(Model model,
                                       @CookieValue(value = "viewMode", defaultValue = "mobile") String viewMode) {
        List<Bookmark> bookmarks = bookmarkService.getBookmarks();

        List<Bookmark> available = new ArrayList<>();
        List<Long> missing = new ArrayList<>();
        var paths = new java.util.LinkedHashMap<Long, java.util.LinkedHashMap<Long, String>>();
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
            paths.put(bookmark.getNodeId(), occurrencePath(child));
            bookmark.setNode(node);
            available.add(bookmark);
        }
        model.addAttribute("bookmarks", available);
        model.addAttribute("bookmarkPaths", paths);
        model.addAttribute("missingBookmarkIds", missing);

        model.addAttribute("canWriteBookmarks", bookmarkWriteService.writable());
        model.addAttribute("viewMode", viewMode);
        if ("mobile".equals(viewMode)) {
            return "bookmarksMobile";
        } else {
            return "bookmarks";
        }

    }

    /** Builds each bookmark path from its own parent chain without mutating a shared master Node. */
    private java.util.LinkedHashMap<Long,String> occurrencePath(Children selected) {
        var trail = new ArrayList<Children>();
        var seen = new java.util.HashSet<Long>();
        Children current = selected;
        while (current != null && seen.add(current.getNodeId())) {
            trail.add(current);
            if (current.getFatherId() == 0) break;
            current = childrenService.findById(current.getFatherId());
        }
        java.util.Collections.reverse(trail);
        var path = new java.util.LinkedHashMap<Long,String>();
        for (Children item : trail) {
            long contentId = item.getMasterId() != null && item.getMasterId() != 0 ? item.getMasterId() : item.getNodeId();
            var display = nodeService.findById(contentId);
            if (display != null) path.put(item.getNodeId(),display.getName());
        }
        return path;
    }
}

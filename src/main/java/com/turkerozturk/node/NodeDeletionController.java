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
package com.turkerozturk.node;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.turkerozturk.multipledatabases.RequiresTenant;
import org.springframework.security.access.AccessDeniedException;




@Controller
public class NodeDeletionController {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeDeletionService nodeDeletionService;



    @PreAuthorize("hasRole('ADMIN')")
    @RequiresTenant
    @GetMapping("/nodes/delete/{nodeId}")
    public String confirmDeletion(@PathVariable long nodeId, Model model) {
        if (!nodeDeletionService.isCurrentTenantWritable()) {
            throw new AccessDeniedException("The selected CTB is read-only.");
        }
        Node node = nodeService.findById(nodeId);
        if (node == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("node", node);
        return "node/nodeDelete";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequiresTenant
    @PostMapping("/nodes/delete/{nodeId}")
    public String deleteNode(@PathVariable long nodeId) {
        if (!nodeDeletionService.isCurrentTenantWritable()) {
            throw new AccessDeniedException("The selected CTB is read-only.");
        }
        if (nodeService.findById(nodeId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        nodeDeletionService.deleteNodeWithSubNodes(nodeId);
        return "redirect:/";
    }


}

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



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;




@Controller
public class NodeDeletionController {

    private static final Logger logger = LoggerFactory.getLogger(NodeDeletionController.class);

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeDeletionService nodeDeletionService;



    @GetMapping("/nodes/delete/{nodeId}")
    public String getNodeAsHtml(@PathVariable long nodeId, Model model,
                                @CookieValue(value = "viewMode", defaultValue = "mobile") String viewMode) {

     //   Node node = nodeService.findById(nodeId);
    //    model.addAttribute("deletedNodeId", node.getNodeId());
    //    model.addAttribute("deletedNodeName", node.getName());

      //  Node fatherNode = node.getFatherNode();


        //String message =
        nodeDeletionService.deleteNodeWithSubNodes(nodeId);


      //  model.addAttribute("fatherNodeId", fatherNode.getNodeId());
      //  model.addAttribute("fatherNodeName", fatherNode.getName());





        return "node/nodeDelete";
    }


}

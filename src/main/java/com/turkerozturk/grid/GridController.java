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
package com.turkerozturk.grid;

import com.turkerozturk.helpers.XmlHelper;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
public class GridController {
    @Autowired
    private GridService gridService;

    @Autowired
    private NodeService nodeService;

    @GetMapping("/grids")
    public String getAllAsHtml(Model model) {
        model.addAttribute("grids", gridService.getGrids());
        return "grids"; // Bu, bir Thymeleaf şablonu olmalıdır: src/main/resources/templates/grids.html
    }

    @GetMapping("/grids/{node_id}")
    public String getGridsByNodeId(@PathVariable("node_id") Long nodeId, Model model) throws IOException {
        Node node = nodeService.getById(nodeId);
        Set<Grid> grids = node.getGrids();
        model.addAttribute("grids", grids);
        model.addAttribute("xmlHelper", new XmlHelper());
        return "grid";
    }

    @GetMapping("/grids/{node_id}/{offset}")
    public String getGridByNodeIdAndOffset(@PathVariable("node_id") Long nodeId, @PathVariable("offset") Integer offset, Model model) {
        Node node = nodeService.getById(nodeId);
        Set<Grid> grids = node.getGrids();
        for(Grid grid : grids) {
            if(offset.equals(grid.getId().getOffset())) {
                List<Grid> tmpGrids = new ArrayList<>();
                tmpGrids.add(grid);
                model.addAttribute("grids", grids);
                model.addAttribute("xmlHelper", new XmlHelper());
                break;
            }
        }


        return "grid";
    }

}

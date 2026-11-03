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
package com.turkerozturk.dashboard;

import com.turkerozturk.multipledatabases.RequiresTenant;

//import com.turkerozturk.sunandmoon.MoonTime4j;
//import com.turkerozturk.helpers.CommonsSunCalc;

import com.turkerozturk.node.*;
import com.turkerozturk.xlsx.XlsxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



@Controller
public class DashBoardController {

    private static final Logger logger = LoggerFactory.getLogger(DashBoardController.class);

    /*
        @Autowired
        CommonsSunCalc commonsSunCalc;
        bilgi: Artik com.turkerozturk.global GlobalControllerAdvice sinifinda. ChatGPT sagolsun.
        @Autowired
        MoonTime4j moonTime4j;
    */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboardold")
    public String showDsahboard(Model model) {
/*
        model.addAttribute("astronomyWidget", commonsSunCalc.getAstronomy());
        model.addAttribute("moonTime4j", moonTime4j);
*/
        return "dashboard/dashboard";
    }

    @Autowired
    private NodeService nodeService;

    @Autowired
    private TemplateService templateService;

    /*
    bilgi burasi sablondaki tum nodelerden tablo olusturan can alici yer.
     */
    @RequiresTenant
    @GetMapping("/cherrytemplatenode/{templateNodeId}")
    public String checkTemplateNodesByNodeIds(@PathVariable long templateNodeId, Model model, jakarta.servlet.http.HttpSession session) {
        model.addAttribute("cherryTemplateNode",templateService.getTemplateNode(templateNodeId,(String)session.getAttribute(com.turkerozturk.multipledatabases.TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN)));
        model.addAttribute("reportGrid",false);return "cherrytemplatenode";
    }

    @RequiresTenant
    @GetMapping("/cherrytemplate-type-1/{templateNodeId}")
    public String cherrytemplateTtype1(@PathVariable long templateNodeId, Model model, jakarta.servlet.http.HttpSession session) {
        model.addAttribute("cherryTemplateNode",templateService.getTemplateNode(templateNodeId,(String)session.getAttribute(com.turkerozturk.multipledatabases.TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN)));
        model.addAttribute("reportGrid",true);return "cherrytemplate-type-1";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        List<Object[]> syntaxCounts = nodeService.getNodeSyntaxCounts();
        List<String> syntaxes = syntaxCounts.stream().map(array -> (String) array[0]).collect(Collectors.toList());
        List<Long> counts = syntaxCounts.stream().map(array -> (Long) array[1]).collect(Collectors.toList());

        model.addAttribute("syntaxes", syntaxes);
        model.addAttribute("counts", counts);

        return "dashboard/dashboard";
    }

    /*
    @GetMapping("/check")
    public String checkNode(Model model) {
        String message = nodeService.checkNodeWithNameCherryTemplateTasks();
        model.addAttribute("message", message);
        return "check";
    }
    */

    @GetMapping("/check/{tags}")
    public String checkNodesByTags(@PathVariable String tags, Model model) {
        List<Node> nodes = nodeService.getNodesByTags(tags);
        model.addAttribute("nodes", nodes);
        return "nodes";
    }



    @GetMapping("/check/{tags}/{fatherId}")
    public String checkNodesByTagsAndFatherId(@PathVariable String tags, @PathVariable long fatherId, Model model) {
        List<Node> nodes = nodeService.getNodesByTagsAndFatherId(tags, fatherId);
        model.addAttribute("nodes", nodes);
        return "nodes";
    }

    /**
     bilgi The main purpose of this software starts here.
     **/
    private Map<String, String> getNameAndDescriptions(Node node) {

        Map<String, String> nameAndDescription = new HashMap<>();
        String[] tmp = node.getTxt().split("\\r?\\n");

        for(String t: tmp) {
            String templateName;
            String templateDescription;
            String arr[] = t.split("\\t");
            templateName = arr[0]; // bilgi BOS satir bile olsa MAPe ekliyoruz. Kontrolunu baska metoda birakmak icin.
            if(arr.length > 1) {
                templateDescription = arr[1];
                nameAndDescription.put(templateName, templateDescription);
            } else {
                nameAndDescription.put(templateName, templateName);
            }
            logger.info("ANA DUGUMDEKI SABLON ISMI: " + templateName);
        }
        return nameAndDescription;
    }




    @GetMapping("/cherryprojectnodes/{tags}")
    public String checkProjectNodesByTagsColumn(@PathVariable String tags, Model model) {
        List<Node> nodes = nodeService.getNodesByTags(tags);
        model.addAttribute("nodes", nodes);
        return "cherryprojectnodes";
    }

    // bilgi data node buraya
    /*
    @GetMapping("/cherryprojectnode/{projectNodeId}")
    public String showProjectNode(@PathVariable long projectNodeId, Model model) {
        ProjectNode projectNode = nodeService.getProjectNodeById(projectNodeId);


        model.addAttribute("cherryProjectNode", projectNode);
        return "cherryprojectnode";
    }
    */

}

/*
        Node node = nodeService.findById(templateNodeId);

        if (node.getTxt() == null || node.getTxt().isEmpty()) {
            model.addAttribute("message", "Listede variable yok");
        } else {

            model.addAttribute("message", "Bulundu");

            List<Node> dataNodes = nodeService.getNodesWithNameTemplateNames(node.getTxt().split("\\r?\\n"));

            // ADIM sablon dugum hakkinda bilgi gostermek.
            model.addAttribute("cherryTemplateNode", new TemplateNode(node, dataNodes));

        }
        model.addAttribute("cherryTemplateNode", new TemplateNode(node, dataNodes));
        */

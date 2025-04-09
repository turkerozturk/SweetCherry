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

//import com.turkerozturk.sunandmoon.MoonTime4j;
//import com.example.sqlitedemo.helpers.CommonsSunCalc;

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
    @GetMapping("/cherrytemplatenode/{templateNodeId}")
    public String checkTemplateNodesByNodeIds(@PathVariable long templateNodeId, Model model) {

        TemplateNode templateNode = templateService.getTemplateNode(templateNodeId);

        model.addAttribute("cherryTemplateNode", templateNode);

        if(!templateNode.getProjectNodes().isEmpty()) {
            model.addAttribute("hasProjectNodes", true);

        }



        // see thymeleaf newline character topic:
        model.addAttribute("newlinechar", '\n');

        return "cherrytemplatenode";
    }

    @GetMapping("/cherrytemplate-type-1/{templateNodeId}")
    public String cherrytemplateTtype1(@PathVariable long templateNodeId, Model model) {

        TemplateNode templateNode = templateService.getTemplateNode(templateNodeId);

        model.addAttribute("cherryTemplateNode", templateNode);

        if(!templateNode.getProjectNodes().isEmpty()) {
            model.addAttribute("hasProjectNodes", true);

        }



        // see thymeleaf newline character topic:
        model.addAttribute("newlinechar", '\n');




        // bilgi start


        // bilgi: gonderilecek tum veri. Icteki liste bir satira ait hucreler, distaki liste satirlar, ilk satir sutun basliklarini icerir.
        List<List<String>> dataRowsAsStringArray = new ArrayList<>();





        // bilgi: ilk satir yani sutun basliklarini iceren hucrelerin eldesi ve satirin ana tabloya eklenmesi.
        List<String> rowOfDataLabels = new ArrayList<>();

        // bilgi: etiket ismi messages.properties dosyasinda var.
        rowOfDataLabels.add("node.nodeId");

        // bilgi: etiket ismi messages.properties dosyasinda var.
        rowOfDataLabels.add("node.name");


        for (String key : templateNode.getDataLabels().keySet()) {
            rowOfDataLabels.add(key);
        }
        dataRowsAsStringArray.add(rowOfDataLabels);

        // bilgi: ikinci ve sonraki satirlarin eldesi.
        List<ProjectNode> projectNodes = templateNode.getProjectNodes();
        for (ProjectNode projectNode : projectNodes) {

            // bilgi: projectNode nin veri hucrelerini string listesine donusturecegiz.
            Map<String, DataNode> dataNodesMap = projectNode.getDataNodes();


            List<String> dataCellsInOneRow = new ArrayList<>();

            // bilgi: node id sutunu olsun.
            dataCellsInOneRow.add(String.valueOf(projectNode.getNodeId()));

            // bilgi: node name sutunu olsun.
            dataCellsInOneRow.add(projectNode.getName());


            for (String key : templateNode.getDataLabels().keySet()) {



                DataNode dataNode = dataNodesMap.get(key);
                String value;
                if (dataNode != null) {
                    if (!dataNode.getSyntax().equals("custom-colors")) {
                        value = dataNode.getData();
                    } else {
                        try {
                            String xmlContent = dataNode.getData();
                            ByteArrayInputStream input = new ByteArrayInputStream(xmlContent.getBytes("UTF-8"));
                            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                            Document doc = null;
                            doc = dBuilder.parse(input);
                            doc.getDocumentElement().normalize();
                            value = XlsxController.getTextContent(doc.getDocumentElement());

                        } catch (SAXException | IOException e) {
                            throw new RuntimeException(e);
                        } catch (ParserConfigurationException e) {
                            throw new RuntimeException(e);
                        }


                    }
                } else {
                    value = null;
                }
                dataCellsInOneRow.add(value);

            }

            dataRowsAsStringArray.add(dataCellsInOneRow);
        }


        model.addAttribute("dataRowsAsStringArray", dataRowsAsStringArray);


        // bilgi stop

        return "cherrytemplate-type-1";
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
    @GetMapping("/cherrytemplatetasks")
    public String checkNodeWithNameCherryTemplateTasks(Model model) {
        CustomListsNodeStatus customListsNodeStatus = null;

        List<Node> nodes = nodeService.getNodesWithNameCherryTemplateTasks();

        if (nodes.isEmpty()) {
            customListsNodeStatus = CustomListsNodeStatus.MAIN_NODE_IS_ABSENT; // bilgi ASAMA 1
        } else {

            Node node = nodes.get(0);

            if (node.isRichTextBit()) {
            //node.getTxt() == null || node.getTxt().isEmpty()) {
                customListsNodeStatus = CustomListsNodeStatus.EXIST_BUT_ITS_CONTENT_IS_RICH_TEXT; // bilgi ASAMA 1.5

            } else {
                TextContentToListHelper textContentToListHelper = new TextContentToListHelper(node.getTxt());
                Map<String, String> customListNamesAndDescriptionsRaw = textContentToListHelper.getKeyNameAndDescription();

                Map<String, String> uselessLinesMap = textContentToListHelper.getInvalidDataMap();

                if(!customListNamesAndDescriptionsRaw.isEmpty()) {
                    model.addAttribute("customListNamesAndDescriptionsRaw", customListNamesAndDescriptionsRaw);
                }

                if(!uselessLinesMap.isEmpty()) {
                    model.addAttribute("uselessLinesMap", uselessLinesMap);
                }

                List<Node> templateNodes = nodeService.getNodesWithNameTemplateNames(customListNamesAndDescriptionsRaw.keySet().toArray(new String[0]));

                // TODO asagida valid name olmasina ragmen
                if(!templateNodes.isEmpty()) {
                    if (nodes.size() == 1) {

                        customListsNodeStatus = CustomListsNodeStatus.EXIST;


                        // ADIM ana dugum hakkinda bilgi gostermek.
                        model.addAttribute("godNode", new UberNode(node, templateNodes));
                        model.addAttribute("descriptions", customListNamesAndDescriptionsRaw.values());


                    } else {
                        customListsNodeStatus = CustomListsNodeStatus.EXIST_MULTIPLE;
                        long smallestNodeId = nodes.stream().mapToLong(Node::getNodeId).min().orElse(Long.MAX_VALUE);
                        List<Long> otherNodeIds = nodes.stream().map(Node::getNodeId).collect(Collectors.toList());
                        otherNodeIds.remove(smallestNodeId);
                        model.addAttribute("smallestNodeId", nodes);
                        model.addAttribute("nodes", nodes);

                    }
                } else {
                    customListsNodeStatus = CustomListsNodeStatus.EXIST_BUT_HAS_NO_VALID_LIST_NAMES;
                    model.addAttribute("nodes", nodes);

                }


            }

        }
        model.addAttribute("customListsNodeStatus", customListsNodeStatus);

        return "customlists/cherrytemplatetasks";
    }

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

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

import com.turkerozturk.children.Children;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.helpers.DateTimeHelper;
import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.helpers.TimeEnum;
import com.turkerozturk.node.filter.FormSearch;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import static com.turkerozturk.yenixmlparser.YeniXMLTransformer.RICH_TEXT_TAG;

@Service
public class NodeService {

    @Autowired
    private ChildrenRepository childrenRepository;
    @Autowired
    private NodeRepository nodeRepository;

    public List<Node> getNodes() {
        List<Node> nodes = nodeRepository.findAll();
        return nodes;
    }

    public List<Node> getNodesByTagsContaining(String filter) {
        return nodeRepository.findByTagsContaining(filter);
    }

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> getNodeSyntaxCounts() {
        String sql = "SELECT syntax, COUNT(syntax) AS sumcount FROM Node GROUP BY syntax";
        Query query = entityManager.createQuery(sql);
        return query.getResultList();

    }

    public List<Node> getNodesByTags(String tags) {
        return nodeRepository.findByTags(tags);
    }

    public List<Node> getNodesWithNameCherryTemplateTasks() {
        // return nodeRepository.findByName("cherrytemplatetasks");
        return nodeRepository.findByNameOrderByNodeIdAsc("cherrytemplatetasks");

    }

    public List<Node> getNodesWithNameTemplateNames(String[] templateNodeNames) {
        List<Node> templateNodes = new ArrayList<>();
        for(int i = 0; i < templateNodeNames.length; i++) {
            List<Node> temporaryNodes = nodeRepository.findByName("cherrytemplate-" + templateNodeNames[i]);
            if(temporaryNodes.size() == 1) {
                Node templateNode = temporaryNodes.get(0);
                templateNodes.add(templateNode);
            }
        }
        return templateNodes;
    }

    public List<Node> findByName(String name) {
        return nodeRepository.findByName(name);
    }

    public Node findById(long nodeId) {
        return nodeRepository.findById(nodeId);
    }

    public Node getById(long nodeId) {
        return nodeRepository.getById(nodeId);
    }

    public List<Node> getNodesByParentNodeId(String tags, long nodeId) {
        List<Children> childrenList = childrenRepository.findByFatherId(nodeId);
        List<Node> nodeList = new ArrayList<>();
        for (Children child : childrenList) {
            Node node = nodeRepository.findById(child.getNodeId());
            if (node != null) {
                nodeList.add(node);
            }
        }
        return nodeList;
    }

    public List<Node> getNodesByTagsAndFatherId(String tags, long fatherId) {
        List<Children> childrenList = childrenRepository.findByFatherId(fatherId);
        System.out.println("turker: " + childrenList.size());
        List<Node> nodeList = new ArrayList<>();
        nodeList.add(nodeRepository.findById(fatherId));
        for (Children child : childrenList) {
            Node node = nodeRepository.findById(child.getNodeId());
            if (node != null && node.getTags().equals("variable")) {
                nodeList.add(node);
            }
        }
        return nodeList;
    }

    /**
     *
     * @param nodeId
     * @return
     */
    public LinkedHashMap<Long, String> addBreadcrumbs(Long nodeId) {

        List<Long> fatherNodeIds = findAllNodeIdsUntilFatherId(nodeId);
        Collections.reverse(fatherNodeIds);
        LinkedHashMap<Long, String> breadcrumbs = new LinkedHashMap<>();
        for (long fathetNodeId : fatherNodeIds) {
            Node fatherNode = findById(fathetNodeId);
            breadcrumbs.put(fathetNodeId, fatherNode.getName());
        }

        return breadcrumbs;
    }

    public String doBreadcrumbsToString(LinkedHashMap<Long, String> breadcrumbs) {
        StringBuilder sb = new StringBuilder();
        for(Long fatherNodeId : breadcrumbs.keySet()) {
            String fatherNodeName = breadcrumbs.get(fatherNodeId);
            sb.append(" > ");
            sb.append(fatherNodeName);
        }

        return sb.toString();
    }

    public List<Long> findAllNodeIdsUntilFatherId(long startNodeId) {
        List<Long> nodeIds = new ArrayList<>();
        long currentNodeId = startNodeId;

        while (currentNodeId != 0) {
            nodeIds.add(currentNodeId);
            Children node = childrenRepository.findById(currentNodeId).orElse(null);
            if (node == null) {
                break;
            }
            currentNodeId = node.getFatherId();
        }

        return nodeIds;
    }

    public void prepareFatherNode(Node node) {




        long fatherId = node.getNodeIdInChildrenTable().getFatherId();


            if (fatherId != 0) {

                Optional<Node> fatherNode = Optional.ofNullable(nodeRepository.findById(fatherId));
                node.setFatherNode(fatherNode.get());
                //System.out.println("FATHER: " + fatherNode.get().getName());
               // node.setFatherNode(fatherNode);
            }




        /*

        if (node.getFatherNode() != null) {
            Optional<Node> fatherNode = nodeRepository.findById(node.getFatherNode().getId());
            fatherNode.ifPresent(node::setFatherNode);
        }
        return nodeRepository.save(node);
        */
    }


    public List<Node> findRecentlyCreatedNodes() {
        Pageable pageable = PageRequest.of(0, 50);
        return nodeRepository.findRecentlyCreatedNodes(pageable);
    }

    public List<Node> findRecentlyModifiedNodes() {
        Pageable pageable = PageRequest.of(0, 50);
        return nodeRepository.findRecentlyModifiedNodes(pageable);
    }

    /*
    public List<Node> getNodesWithCustomQuery(String name, String txt, String tags) {
        return nodeRepository.findByCustomQuery(name, txt, tags);
    }

    public Page<Node> getNodesWithCustomQuery(String name, String txt, String tags, Pageable pageable) {
        return nodeRepository.findByCustomQuery(name, txt, tags, pageable);
    }
    */

    public Page<Node> getNodesWithCustomQuery(String name, String txt, String tags,
                                              Long tsCreationStart, Long tsCreationEnd,
                                              Long tsModificationStart, Long tsModificationEnd,
                                              Pageable pageable) {
        return nodeRepository.findByCustomQuery(name, txt, tags,
                tsCreationStart, tsCreationEnd,
                tsModificationStart, tsModificationEnd,
                pageable);
    }

    public Page<Node> getNodesWithCustomQuery(Long nodeIcons, String name, String txt, String tags,
                                              Long tsCreationStart, Long tsCreationEnd,
                                              Long tsModificationStart, Long tsModificationEnd,
                                              Pageable pageable) {
        return nodeRepository.findByCustomQuery(nodeIcons, name, txt, tags,
                tsCreationStart, tsCreationEnd,
                tsModificationStart, tsModificationEnd,
                pageable);
    }

/*
    public Page<Node> getNodesWithCustomQuery(String name, String txt, String tags,
                                              Long tsCreationStart, Long tsCreationEnd,
                                              Long tsModificationStart, Long tsModificationEnd,
                                              Long nodeIdStart, Long nodeIdEnd,
                                              Pageable pageable) {
        return nodeRepository.findByCustomQuery(name, txt, tags,
                tsCreationStart, tsCreationEnd,
                tsModificationStart, tsModificationEnd,
                nodeIdStart, nodeIdEnd,
                pageable);
    }

    public Page<Node> getNodesWithCustomQuery(Long nodeIcons, String name, String txt, String tags,
                                              Long tsCreationStart, Long tsCreationEnd,
                                              Long tsModificationStart, Long tsModificationEnd,
                                              Long nodeIdStart, Long nodeIdEnd,
                                              Pageable pageable) {
        return nodeRepository.findByCustomQuery(nodeIcons, name, txt, tags,
                tsCreationStart, tsCreationEnd,
                tsModificationStart, tsModificationEnd,
                nodeIdStart, nodeIdEnd,
                pageable);
    }
    */

    // YUKARIDAKILER SILINEBILIR, ASAGIDAKILER KULLANILIYOR.
    public Page<Node> getNodesWithCustomQuery(String name, String txt, String tags,
                                              Long tsCreationStart, Long tsCreationEnd,
                                              Long tsModificationStart, Long tsModificationEnd,
                                              Long nodeIdStart, Long nodeIdEnd,
                                              String syntax,
                                              Pageable pageable) {
        return nodeRepository.findByCustomQuery(name, txt, tags,
                tsCreationStart, tsCreationEnd,
                tsModificationStart, tsModificationEnd,
                nodeIdStart, nodeIdEnd,
                syntax,
                pageable);
    }

    public Page<Node> getNodesWithCustomQuery(Long nodeIcons, String name, String txt, String tags,
                                              Long tsCreationStart, Long tsCreationEnd,
                                              Long tsModificationStart, Long tsModificationEnd,
                                              Long nodeIdStart, Long nodeIdEnd,
                                              String syntax,
                                              Pageable pageable) {
        return nodeRepository.findByCustomQuery(nodeIcons, name, txt, tags,
                tsCreationStart, tsCreationEnd,
                tsModificationStart, tsModificationEnd,
                nodeIdStart, nodeIdEnd,
                syntax,
                pageable);
    }

    public Page<Node> getNodesWithCustomQuery(List<Integer> nodeIcons, String name, String txt, String tags,
                                              Long tsCreationStart, Long tsCreationEnd,
                                              Long tsModificationStart, Long tsModificationEnd,
                                              Long nodeIdStart, Long nodeIdEnd,
                                              String syntax,
                                              Pageable pageable) {
        return nodeRepository.findByCustomQuery(nodeIcons, name, txt, tags,
                tsCreationStart, tsCreationEnd,
                tsModificationStart, tsModificationEnd,
                nodeIdStart, nodeIdEnd,
                syntax,
                pageable);
    }


    public Page<Node> getFilteredFormNodes(FormSearch formSearch, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            System.out.println("hata var:");
            // Binding hatasi varsa, hataları yazdir. bilgi CHATGPT gosterdi:
            for (FieldError error : bindingResult.getFieldErrors()) {
                System.out.println("Field: " + error.getField());
                System.out.println("Code: " + error.getCode());
                System.out.println("Message: " + error.getDefaultMessage());
            }
        }

        LinkedHashSet<NodeIcon> selectedNodeIcons = formSearch.getNodeIcons();
        if(selectedNodeIcons != null) {
            for (NodeIcon nodeIcon : selectedNodeIcons) {
                System.out.println(nodeIcon.name());
            }
        }




        // ---------------------------------------------------------------------------------------------

        Pageable pageable = PageRequest.of(formSearch.getPageNumber(), formSearch.getPageSize(), Sort.by("ts_lastsave").descending());

        Page<Node> nodes;

        // TODO LocalDateTime         LocalDateTime dateWithTime = date.withHour(23).withMinute(59).withSecond(59);
        long tsCreationStart = DateTimeHelper.convertFormDateWithTimeToUnixEpochSeconds(
                formSearch.getTsCreationStartFilter(), TimeEnum.START_OF_DAY);
        long tsCreationEnd = DateTimeHelper.convertFormDateWithTimeToUnixEpochSeconds(
                formSearch.getTsCreationEndFilter(), TimeEnum.END_OF_DAY);
        long tsModificationStart = DateTimeHelper.convertFormDateWithTimeToUnixEpochSeconds(
                formSearch.getTsModificationStartFilter(), TimeEnum.START_OF_DAY);
        long tsModificationEnd = DateTimeHelper.convertFormDateWithTimeToUnixEpochSeconds(
                formSearch.getTsModificationEndFilter(), TimeEnum.END_OF_DAY);



        if (formSearch.getNodeIcons() == null) {
            //nodeIcons = 0L;
            nodes = getNodesWithCustomQuery(formSearch.getNameFilter(),
                    formSearch.getTextFilter(),
                    formSearch.getTagsFilter(),
                    tsCreationStart, tsCreationEnd,
                    tsModificationStart, tsModificationEnd,
                    formSearch.getNodeIdStartFilter(), formSearch.getNodeIdEndFilter(),
                    formSearch.getSyntaxFilter(),
                    pageable);
        } else {

            LinkedHashSet<NodeIcon> nodeIconSet = formSearch.getNodeIcons();
            //    if(nodeIconSet != null) {
                /*
                Integer[] iconIds = nodeIconSet.stream()
                        .map(nodeIcon -> nodeIcon.getIconIdIn16bit())
                        .toArray(Integer[]::new);
*/
            List<Integer> iconIds = nodeIconSet.stream()
                    .map(nodeIcon -> nodeIcon.getIconIdIn16bit())
                    .collect(Collectors.toList());


            nodes = getNodesWithCustomQuery(iconIds, //nodeIcons
                    formSearch.getNameFilter(),
                    formSearch.getTextFilter(),
                    formSearch.getTagsFilter(),
                    tsCreationStart, tsCreationEnd,
                    tsModificationStart, tsModificationEnd,
                    formSearch.getNodeIdStartFilter(), formSearch.getNodeIdEndFilter(),
                    formSearch.getSyntaxFilter(),
                    pageable);

        }

        return nodes;

    }


    // bilgi MOVED FROM NODE CONTROLLER
    public static Map<Long, Element> getRichTextsMap(String xmlContent) throws ParserConfigurationException, IOException, SAXException {
        Map<Long, Element> richTextsMap = new HashMap<>();
        int dataOffset = 0;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document sourceDocument = dBuilder.parse(new InputSource(new StringReader(xmlContent)));
        NodeList richTextList = sourceDocument.getElementsByTagName(RICH_TEXT_TAG);
        for (int r = 0; r < richTextList.getLength(); r++) {
            Element sourceRichTextTag = (Element) richTextList.item(r);
            String textContent = sourceRichTextTag.getTextContent();
            int dataLength = textContent.length();

            richTextsMap.put((long) dataOffset, sourceRichTextTag);

            // bilgi: bu hep dongunun sonunda olmali
            // DUZELTME
            if (dataLength == 0) {
                dataOffset = dataOffset + 1;
            } else {
                dataOffset += dataLength;
            }
            // DUZELTME
        }

        return richTextsMap;

    }


}

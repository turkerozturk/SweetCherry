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
package com.turkerozturk.children;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChildrenService {
    @Autowired
    private ChildrenRepository childrenRepository;

    @Autowired
    private NodeRepository nodeRepository;

    public List<Children> getChildren() {
        List<Children> children = childrenRepository.findAll();
        return children;
    }

    public List<Long> getSharedNodeIdsByMasterId(Long masterId) {

        List<Children> rawChildren = childrenRepository.findByMasterId(masterId);
        List<Long> result = new ArrayList<>();

        for(Children child : rawChildren) {
            result.add(child.getNodeId());
        }

        return result;
    }

    /**
     * Retrieves lightweight data from nodes list, to make navigation faster.
     * @param fatherId
     * @return
     */
    public List<NaviNode> getNaviNodesByFatherId(long fatherId) {

        // BASLA shared node correction
        List<Children> rawChildren = childrenRepository.findByFatherId(fatherId);
        List<Children> children = new ArrayList<>();

        for (Children child : rawChildren) {
            if(child.getMasterId() == null || child.getMasterId() == 0) {
                // normal
                children.add(child);
            } else {
                Children masterChild = findById(child.getMasterId());
                children.add(masterChild);
            }
        }
        // BITTI shared node correction

        // Children listesini sequence sutununa gore sirala
        children.sort(Comparator.comparing(Children::getSequence));

        List<Long> nodeIds = children.stream().map(Children::getNodeId).collect(Collectors.toList());

        // String concatenatedIds = String.join(", ", nodeIds.stream().map(Object::toString).toArray(String[]::new));
        // System.out.println(concatenatedIds);

        List<Node> nodes = nodeRepository.findByNodeIdIn(nodeIds);


        // BASLA if the node has sub nodes
        List<NaviNode> naviNodes = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            List<Children> childrenOfNode = childrenRepository.findByFatherId(node.getNodeId());
            if(childrenOfNode.size() > 0) {
                node.setHasChildren(true);
            }

            // İlgili Node için Children kaydını bul
            Children correspondingChild = children.stream()
                    .filter(child -> child.getNodeId() == node.getNodeId())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Corresponding child not found for nodeId: " + node.getNodeId()));

            // Prev ve Next Sibling Node Id'leri bul
            Long prevSiblingNodeId = (i > 0) ? children.get(i - 1).getNodeId() : null;
            Long nextSiblingNodeId = (i < nodes.size() - 1) ? children.get(i + 1).getNodeId() : null;


            NaviNode naviNode = new NaviNode(node.getNodeId(),
                    node.getName(),
                    node.getNodeIcon(),
                    node.isHasChildren(),
                    node.getTitleColorAsHtmlHex(),
                    node.isReadOnly(),
                    node.isBoldnessBit(),
                    (int) correspondingChild.getSequence(), // BURAYA children tablosundaki sequence alanında yazan değeri kaydediyoruz.
                    prevSiblingNodeId,  // Önceki kardeş düğümün nodeId'si
                    nextSiblingNodeId   // Sonraki kardeş düğümün nodeId'si

            );
            naviNodes.add(naviNode);


        }
        // BITTI if the node has sub nodes

        // Node listesini nodeIds sirasina gore yeniden duzenle
        Map<Long, NaviNode> naviNodeMap = naviNodes.stream()
                .collect(Collectors.toMap(NaviNode::nodeId, Function.identity()));

        List<NaviNode> sortedNaviNodes = nodeIds.stream()
                .map(naviNodeMap::get)
                .collect(Collectors.toList());


        return  sortedNaviNodes;
    }

    /**
     * Used by special cherry template page.
     * @param fatherId
     * @return
     */
    public List<Node> getNodesByFatherId(Long fatherId) {

        // BASLA shared node correction
        List<Children> rawChildren = childrenRepository.findByFatherId(fatherId);
        List<Children> children = new ArrayList<>();

        for (Children child : rawChildren) {
            if(child.getMasterId() == null || child.getMasterId() == 0) {
                // normal
                children.add(child);
            } else {
                Children masterChild = findById(child.getMasterId());
                children.add(masterChild);
            }
        }
        // BITTI shared node correction

        // Children listesini sequence sutununa gore sirala
        children.sort(Comparator.comparing(Children::getSequence));

        List<Long> nodeIds = children.stream().map(Children::getNodeId).collect(Collectors.toList());

        // String concatenatedIds = String.join(", ", nodeIds.stream().map(Object::toString).toArray(String[]::new));
        // System.out.println(concatenatedIds);

        List<Node> nodes = nodeRepository.findByNodeIdIn(nodeIds);

        // BASLA if the node has sub nodes
        for (Node node : nodes) {
            List<Children> childrenOfNode = childrenRepository.findByFatherId(node.getNodeId());
            if(childrenOfNode.size() > 0) {
                node.setHasChildren(true);
            }
        }
        // BITTI if the node has sub nodes


        // Node listesini nodeIds sirasina gore yeniden duzenle
        Map<Long, Node> nodeMap = nodes.stream()
                .collect(Collectors.toMap(Node::getNodeId, Function.identity()));

        List<Node> sortedNodes = nodeIds.stream()
                .map(nodeMap::get)
                .collect(Collectors.toList());

        // Tum Node nesnelerinin txt alanlarinı HTML'e donustur
        // Yeniden duzenlenmis Node listesini kullanarak islemleri gerceklestir
        for (Node node : sortedNodes) {
            node.processTxtToHtml();
        }

        return sortedNodes;
    }

    public Children findById(long nodeId) {
        return childrenRepository.findById(nodeId).orElse(null);
    }

    public List<Children> findAllSubChildren(long nodeId) {
        List<Children> allSubChildren = new ArrayList<>();
        findSubChildrenRecursively(nodeId, allSubChildren);
        return allSubChildren;
    }

    private void findSubChildrenRecursively(long nodeId, List<Children> allSubChildren) {
        // 1.ADIM: Verilen node_id'ye denk gelen satırı bul ve listeye ekle
        Children childNode = childrenRepository.findByNodeId(nodeId);
        if (childNode != null && !allSubChildren.contains(childNode)) {
            allSubChildren.add(childNode);
        }

        // 2.ADIM: Aynı node_id değeri ile father_id sütununda eşleşen kayıtları bul ve listeye ekle
        List<Children> subChildren = childrenRepository.findByFatherId(nodeId);
        for (Children subChild : subChildren) {
            if (!allSubChildren.contains(subChild)) {
                allSubChildren.add(subChild);
                // 3.ADIM: 2. adımda bulduğun node_id'ler için rekürsif olarak aynı işlemi yap
                findSubChildrenRecursively(subChild.getNodeId(), allSubChildren);
            }
        }
    }


}


    /*
    List<Long> allSubNodeIs = new ArrayList<>();


    public List<Long> getSubNodeIds(long nodeId) {
        List<Long> allNodeIs = new ArrayList<>();

        List<Children> childrens = childrenRepository.findByFatherId(nodeId);
        for(Children children : childrens) {
         //   retrieveSubNodeIds(children.getNodeId());
        }

        return allNodeIs;
    }

    public List<Long> retrieveSubNodeIds(List<Long> siblingNodeIds) {

        for(Long siblingNodeId : siblingNodeIds) {
            List<Long> subNodeIs = getSubNodeIds(siblingNodeId);
            allSubNodeIs.addAll(subNodeIs);

        }

        return allSubNodeIs;
    }

    */

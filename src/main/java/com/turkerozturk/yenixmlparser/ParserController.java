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
package com.turkerozturk.yenixmlparser;

import com.turkerozturk.anchor.Anchor;
import com.turkerozturk.anchor.AnchorService;
import com.turkerozturk.codebox.CodeBox;
import com.turkerozturk.codebox.CodeBoxService;
import com.turkerozturk.grid.Grid;
import com.turkerozturk.grid.GridService;
import com.turkerozturk.image.Image;
import com.turkerozturk.image.ImageService;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;


@RestController
public class ParserController {

    private static final String RICH_TEXT_TAG = "rich_text";

    @Autowired
    private NodeService nodeService;

    @Autowired
    ImageService imageService;

    @Autowired
    CodeBoxService codeBoxService;

    @Autowired
    GridService gridService;

    @Autowired
    AnchorService anchorService;


    @GetMapping("/parse/{nodeId}")
    public Map<Long, String> parse(@PathVariable long nodeId) {
        ExampleMap<Object> exampleMap = new ExampleMap<>();

        Map<Long, String> map = new HashMap<>();

        Node node = nodeService.findById(nodeId);

        String xmlContent = node.getTxt();

        try {
            Map<Long, Element> richTextsMap = getRichTextsMap(xmlContent);

            for(long offset : richTextsMap.keySet()) {

                map.put(offset, elementToString(richTextsMap.get(offset)));
                exampleMap.put(offset, richTextsMap.get(offset));

            }

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }


        if (node.getSyntax().equals("custom-colors")) {



            // BASLA Yeni OFFSETLI DENEMELER+
            Map<Integer, Anchor> cherryAnchorsMap = new HashMap<>();
            List<Anchor> cherryAnchors = anchorService.getAnchorsByNodeIdAndAnchorIsNotBlank(node.getNodeId());
            for (Anchor cherryAnchor : cherryAnchors) {
                long offset = cherryAnchor.getOffset();
                cherryAnchorsMap.put((int) offset, cherryAnchor);
                // bilgi standart
                String message = "anchor";
                if(map.containsKey(offset)) {
                    message += " (overwrite) ";
                }
                map.put(offset, message);
                exampleMap.put(offset, cherryAnchor);

            }
            Map<Integer, Image> cherryImagesMap = new HashMap<>();
            List<Image> images = imageService.getImagesByNodeIdAndPngIsNotNullAndFileNameIsEmpty(node.getNodeId());
            for (Image image : images) {
                long offset = image.getOffset();
                cherryImagesMap.put(image.getOffset(), image);
                // bilgi standart
                String message = "image";
                if(map.containsKey(offset)) {
                    message += " (overwrite) ";
                }
                map.put(offset, message);
                exampleMap.put(offset, image);

            }
            Map<Integer, CodeBox> cherryCodeBoxesMap = new HashMap<>();
            Set<CodeBox> codeBoxes = node.getCodeBoxes();//.codeBoxService.getCodeBoxesByNodeId(node.getNodeId());
            for (CodeBox codeBox : codeBoxes) {
                long offset = codeBox.getId().getOffset();
                cherryCodeBoxesMap.put(codeBox.getId().getOffset(), codeBox);
                // bilgi standart
                String message = "codeBox";
                if(map.containsKey(offset)) {
                    message += " (overwrite) ";
                }
                map.put(offset, message);
                exampleMap.put(offset, codeBox);
            }
            Map<Integer, Grid> cherryGridsMap = new HashMap<>();
            Set<Grid> grids = node.getGrids();
            for (Grid grid : grids) {
                long offset = grid.getId().getOffset();
                cherryGridsMap.put(grid.getId().getOffset(), grid);
                // bilgi standart
                String message = "grid";
                if(map.containsKey(offset)) {
                    message += " (overwrite) ";
                }
                map.put(offset, message);
                exampleMap.put(offset, grid);

            }

        }

        Map<Long, String> sortedMap = sortMapByKey(map);

        try {
            YeniXMLTransformer.parse(exampleMap, nodeId); // bilgi en onemli ve yeni yer
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }




        return sortedMap;
    }


    public String parseNodeTxt(Node node) {
        ExampleMap<Object> exampleMap = new ExampleMap<>();
        String xmlContent = node.getTxt();

        try {
            Map<Long, Element> richTextsMap = getRichTextsMap(xmlContent);

            for(long offset : richTextsMap.keySet()) {

                //map.put(offset, elementToString(richTextsMap.get(offset)));
                exampleMap.put(offset, richTextsMap.get(offset));

            }

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }


        if (node.getSyntax().equals("custom-colors")) {



            // BASLA Yeni OFFSETLI DENEMELER+
            Map<Integer, Anchor> cherryAnchorsMap = new HashMap<>();
            List<Anchor> cherryAnchors = anchorService.getAnchorsByNodeIdAndAnchorIsNotBlank(node.getNodeId());
            for (Anchor cherryAnchor : cherryAnchors) {
                long offset = cherryAnchor.getOffset();
                cherryAnchorsMap.put((int) offset, cherryAnchor);
                // bilgi standart
                //String message = "anchor";
                //if(map.containsKey(offset)) {
                //    message += " (overwrite) ";
                //}
                //map.put(offset, message);
                exampleMap.put(offset, cherryAnchor);

            }
            Map<Integer, Image> cherryImagesMap = new HashMap<>();
            List<Image> images = imageService.getImagesByNodeIdAndPngIsNotNullAndFileNameIsEmpty(node.getNodeId());
            for (Image image : images) {
                long offset = image.getOffset();
                cherryImagesMap.put(image.getOffset(), image);
                // bilgi standart
                //String message = "image";
                //if(map.containsKey(offset)) {
                //    message += " (overwrite) ";
                //}
               // map.put(offset, message);
                exampleMap.put(offset, image);

            }
            Map<Integer, CodeBox> cherryCodeBoxesMap = new HashMap<>();
            Set<CodeBox> codeBoxes = node.getCodeBoxes();//codeBoxService.getCodeBoxesByNodeId(node.getNodeId());
            for (CodeBox codeBox : codeBoxes) {
                long offset = codeBox.getId().getOffset();
                cherryCodeBoxesMap.put(codeBox.getId().getOffset(), codeBox);
                // bilgi standart
                String message = "codeBox";
                //if(map.containsKey(offset)) {
                //    message += " (overwrite) ";
                //}
               // map.put(offset, message);
                exampleMap.put(offset, codeBox);
            }
            Map<Integer, Grid> cherryGridsMap = new HashMap<>();
            Set<Grid> grids = node.getGrids();
            for (Grid grid : grids) {
                long offset = grid.getId().getOffset();
                cherryGridsMap.put(grid.getId().getOffset(), grid);
                // bilgi standart
                //String message = "grid";
                //if(map.containsKey(offset)) {
                //    message += " (overwrite) ";
                //}
                //map.put(offset, message);
                exampleMap.put(offset, grid);

            }

        }

        //Map<Long, String> sortedMap = sortMapByKey(map);
        String resultHTML = null;
        try {
            resultHTML = YeniXMLTransformer.parse(exampleMap, node.getNodeId()); // bilgi en onemli ve yeni yer
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        return resultHTML;

    }

    public Map<Long, Element> getRichTextsMap(String xmlContent) throws ParserConfigurationException, IOException, SAXException {
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

    public static String elementToString(Element element) {
        try {
            // Bir DOMSource oluşturarak Element'i temsil eden bir DOMSource oluşturun
            DOMSource source = new DOMSource(element);

            // StringWriter kullanarak dönüşüm sonucunu almak için bir StreamResult oluşturun
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            // Transformer oluşturun ve DOMSource'u StreamResult'e dönüştürün
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, result);

            // StringWriter'daki içeriği bir dize olarak alın
            String xmlString = writer.toString();

            return xmlString;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<Long, String> sortMapByKey(Map<Long, String> map) {
        // Map girdilerini bir List'e dönüştürün
        List<Map.Entry<Long, String>> list = new LinkedList<>(map.entrySet());

        // Comparator kullanarak girdileri Long değerine göre sıralayın
        Collections.sort(list, new Comparator<Map.Entry<Long, String>>() {
            public int compare(Map.Entry<Long, String> o1, Map.Entry<Long, String> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        // Sıralanmış girdilerden yeni bir Map oluşturun (LinkedHashMap kullanarak sıralı kalmasını sağlayın)
        Map<Long, String> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<Long, String> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }






}

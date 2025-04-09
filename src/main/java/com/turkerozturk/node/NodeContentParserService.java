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

import com.turkerozturk.anchor.Anchor;
import com.turkerozturk.anchor.AnchorService;
import com.turkerozturk.codebox.CodeBox;
import com.turkerozturk.codebox.CodeBoxService;
import com.turkerozturk.grid.Grid;
import com.turkerozturk.grid.GridService;
import com.turkerozturk.helpers.highlighter.pygments.CodeHighLighter;
import com.turkerozturk.helpers.highlighter.pygments.LexerEnum;
import com.turkerozturk.image.Attachment;
import com.turkerozturk.image.Image;
import com.turkerozturk.image.ImageService;
import com.turkerozturk.yenixmlparser.ExampleMap;
import com.turkerozturk.yenixmlparser.YeniXMLTransformer;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.python.core.PyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NodeContentParserService {

    private static final Logger logger = LoggerFactory.getLogger(NodeContentParserService.class);
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

    @Value("${myapp.debug}")
    private Boolean myappIsDebugEnabled;

    public Node parseNodeContent(Node node, HttpServletRequest request) {
        //String parsedContent = null;


        // duz veya zengin metin olmayan tum icerikler syntax sutununda yazan dile gore highlight edilir.
        // https://pygments.org/docs/lexers/#pygments.lexers.shell.MSDOSSessionLexer
        // adresinde hangi lexer hangi turleri renklendirebiliyor yaziyor.
        // Ornek olarak BashLexer sunlardan fazlasini renklendirir: bash, sh, ksh, zsh, shell, openrc
        if(!node.getSyntax().equals("plain-text") && !node.getSyntax().equals("custom-colors")) {
            String mappedHighlighter;
            try {
                if (node.getSyntax().equals("sh")) {
                    mappedHighlighter = "bash";
                } else if (node.getSyntax().equals("dosbatch")) {
                    mappedHighlighter = "msdossession";
                } else if (node.getSyntax().equals("js")) {
                    mappedHighlighter = "javascript";
                } else if (node.getSyntax().equals("python")) {
                    mappedHighlighter = "python2"; // eskide kaldigi icin python yerine python 2 demisler
                } else if (node.getSyntax().equals("python3")) {
                    mappedHighlighter = "python"; // aciklamasi pygment sitesinde var, 3 icin python diye degistirmisler
                } else {
                    mappedHighlighter = node.getSyntax();
                }
                String checkIfExist = LexerEnum.findClassNameByLanguageString(mappedHighlighter);
                if(checkIfExist != null) {
                    node.setTxtAsHtml(CodeHighLighter.highlightLanguage(mappedHighlighter, node.getTxt()));
                } else {
                    String parserNotFound = "PARSER NOT FOUND FOR: " + node.getSyntax() + "!!!\n\n\n";
                    node.setTxtAsHtml(StringEscapeUtils.escapeHtml4(parserNotFound + node.getTxt()));
                }
            } catch (PyException pyException) {
                node.setTxtAsHtml(StringEscapeUtils.escapeHtml4(NodeController.getErrorContent(request, pyException, node)));
            }
        } else if (node.getSyntax().equals("custom-colors")) {

            // BASLA Yeni OFFSETLI DENEMELER+
            Map<Integer, Anchor> cherryAnchorsMap = new HashMap<>();
            List<Anchor> cherryAnchors = anchorService.getAnchorsByNodeIdAndAnchorIsNotBlank(node.getNodeId());
            for(Anchor cherryAnchor : cherryAnchors) {
                cherryAnchorsMap.put(cherryAnchor.getOffset(), cherryAnchor);
            }
            Map<Integer, Image> cherryImagesMap = new HashMap<>();
            List<Image> images = imageService.getImagesByNodeIdAndPngIsNotNullAndFileNameIsEmpty(node.getNodeId());
            for(Image image : images) {
                cherryImagesMap.put(image.getOffset(), image);
            }
            if(node.getHasCodeBox()) {
                Map<Integer, CodeBox> cherryCodeBoxesMap = new HashMap<>();
                Set<CodeBox> codeBoxes = node.getCodeBoxes();
                for (CodeBox codeBox : codeBoxes) {
                    cherryCodeBoxesMap.put(codeBox.getId().getOffset(), codeBox);
                }
            }
            if(node.getHasTable()) {
                Map<Integer, Grid> cherryGridsMap = new HashMap<>();
                Set<Grid> grids = node.getGrids();
                for (Grid grid : grids) {
                    cherryGridsMap.put(grid.getId().getOffset(), grid);
                }
            }

            // bilgi 2024-04-20 oncesi xml parserim
            // String newContent = NodeTxtXmlTransformer.parse(node.getTxt(), nodeId, cherryAnchorsMap, cherryImagesMap, cherryCodeBoxesMap, cherryGridsMap);


            String newContent = parseNodeTxt(node); // bilgi 2024-04-20 SONRASI xml to html parserim


            //node.setTxt(newContent);
            node.setTxtAsHtml(newContent);
            node.setTxtAsTransformed(newContent);

            //  if(node.getHasImage() > 0 || node.getHasCodeBox() > 0 || node.getHasTable() > 0) {

            // bilgi


            //     String fullRichText = xmlParserRichTextSpecial.parse(node);
            //  model.addAttribute ( "debugTxtReplaced" , fullRichText ) ;

            // node.setTxt(fullRichText);
            // bilgi node.setTxt(fullRichText);
            //  }

            //  node.processTxtToHtml();

            // BITIR YENI OFFSETLI DENEMELER

        }

        return node;
    }

    private boolean areImagesEmbedded = false;

    public String parseNodeTxt(Node node, boolean areImagesEmbedded) {

        this.areImagesEmbedded = areImagesEmbedded;

        return parseNodeTxt(node);

    }

        public String parseNodeTxt(Node node) {
        ExampleMap<Object> exampleMap = new ExampleMap<>();
        String xmlContent = node.getTxt();




        if (node.getSyntax().equals("custom-colors")) {

            try {
                Map<Long, Element> richTextsMap = nodeService.getRichTextsMap(xmlContent);

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
            // basla attachments
            Map<Integer, Attachment> cherryAttachmentsMap = new HashMap<>();
            List<Attachment> attachments= imageService.getFindAllByNonEmptyFileName(node.getNodeId());
            for (Attachment attachment : attachments) {
                long offset = attachment.getOffset();
                cherryAttachmentsMap.put(attachment.getOffset(), attachment);
                // bilgi standart
                //String message = "image";
                //if(map.containsKey(offset)) {
                //    message += " (overwrite) ";
                //}
                // map.put(offset, message);
                exampleMap.put(offset, attachment);
            }
            // bitti attachments
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
            if(node.getHasCodeBox()) {
                Map<Integer, CodeBox> cherryCodeBoxesMap = new HashMap<>();
                Set<CodeBox> codeBoxes = node.getCodeBoxes();
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
            }
            if(node.getHasTable()) {
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

        }

        //Map<Long, String> sortedMap = sortMapByKey(map);
        String resultHTML = null;
        try {

            resultHTML = YeniXMLTransformer.parse(exampleMap, node.getNodeId(), myappIsDebugEnabled, areImagesEmbedded); // bilgi en onemli ve yeni yer
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        return resultHTML;

    }




    public static boolean isHtmlContentEmpty(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return true;
        }

        // HTML'yi parse et
        Document doc = Jsoup.parse(htmlContent);

        // Düz metni al
        String textContent = doc.text();

        // Tüm boşluk karakterlerini kaldır
        String strippedText = textContent.replaceAll("\\s+", "");

        // Eğer metin boş değilse, false döner
        if (!strippedText.isEmpty()) {
            return false;
        }

        // Görsel veya medya içerikleri kontrol et
        Elements mediaElements = doc.select("img, video, audio, iframe");
        if (!mediaElements.isEmpty()) {
            return false;
        }

        // Boş div ve span etiketlerini kontrol et
        Elements emptyElements = doc.select("div:empty, span:empty, p:empty, br");
        if (!emptyElements.isEmpty()) {
            for (org.jsoup.nodes.Element element : emptyElements) {
                // Eğer boş elementlerin içinde boşluk dışında karakter varsa, false döner
                if (!element.text().trim().isEmpty()) {
                    return false;
                }
            }
        }

        // Boş olarak kabul edilecek başka durumlar varsa burada kontrol edilebilir

        // Metin ve medya içerikleri boşsa, true döner
        return true;
    }



}

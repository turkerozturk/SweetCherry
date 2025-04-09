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
package com.turkerozturk.cherryxml;

import com.turkerozturk.anchor.Anchor;
import com.turkerozturk.codebox.CodeBox;
import com.turkerozturk.grid.Grid;
import com.turkerozturk.image.Image;
import com.turkerozturk.yenixmlparser.ParserType;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

@Component
public class NodeTxtXmlTransformer {

    private static final String DIV_TAG = "div";
    private static final String IMG_TAG = "img";

    private static final String SPAN_TAG = "span";


    private static final String A_TAG = "a";

    private static final String RICH_TEXT_TAG = "rich_text";

    private static final String DATA_TYPE_ATTR = "data-type";
    private static final String DATA_LENGTH = "data-length";
    private static final String DATA_OFFSET = "data-offset";

    private static final String ID_ATTR = "id";

    private static final String HREF_ATTR = "href";
    private static final String REL_ATTR = "rel";
    private static final String TARGET_ATTR = "target";

    private static final String SCALE_ATTR = "scale";

    private static final String SRC_ATTR = "src";

    private static final String LINK_ATTR = "link";

    private static final String BACKGROUND_ATTR = "background";
    private static final String FOREGROUND_ATTR = "foreground";
    private static final String WEIGHT_ATTR = "weight";
    private static final String STYLE_ATTR = "style";
    private static final String STRIKETHROUGH_ATTR = "strikethrough";
    private static final String UNDERLINE_ATTR = "underline";
    private static final String FAMILY_ATTR = "family";
    private static final String JUSTIFICATION_ATTR = "justification";

    private static final String BR_TAG_PLACEHOLDER = null;




    private static final String CSS_H1_TO_H6_DISPLAY_INLINE = "display: inline;";
    private static final String CSS_WHITE_SPACE_PRE = "white-space: pre;";



    private static final String H1_TAG = "h1";
    private static final String H2_TAG = "h2";
    private static final String H3_TAG = "h3";
    private static final String H4_TAG = "h4";
    private static final String H5_TAG = "h5";
    private static final String H6_TAG = "h6";

    static Document newDocument;

    static Map<ParserType, Boolean> isEnabled = new HashMap<>();
    public static String parse(String xmlContent, long nodeId, Map<Integer, Anchor> cherryAnchorsMap, Map<Integer, Image> cherryImagesMap, Map<Integer, CodeBox> cherryCodeBoxesMap, Map<Integer, Grid> cherryGridsMap) throws IOException, ParserConfigurationException, SAXException, TransformerException {

        newDocument = createNewXmlDocument();
        Element targetRootNode = newDocument.createElement(DIV_TAG);
        newDocument.appendChild(targetRootNode);

        int dataOffset = 0;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document sourceDocument = dBuilder.parse(new InputSource(new StringReader(xmlContent)));
        NodeList richTextList = sourceDocument.getElementsByTagName(RICH_TEXT_TAG);
        for (int r = 0; r < richTextList.getLength(); r++) {
            Element sourceRichTextTag = (Element) richTextList.item(r);
            String textContent = sourceRichTextTag.getTextContent();
            int dataLength = textContent.length();




            sourceRichTextTag.setAttribute(DATA_LENGTH, String.valueOf(dataLength));
            sourceRichTextTag.setAttribute(DATA_OFFSET, String.valueOf(dataOffset));

            isEnabled.put(ParserType.BINARY_FILES, true);
            isEnabled.put(ParserType.IMAGES, true);
            isEnabled.put(ParserType.ATTACHMENTS, true);
            isEnabled.put(ParserType.CODE_BOXES, true);
            isEnabled.put(ParserType.GRIDS, true);
            isEnabled.put(ParserType.LATEX_CONTENTS, true);
            isEnabled.put(ParserType.SCALES, true);
            isEnabled.put(ParserType.SPANS, true);
            isEnabled.put(ParserType.LINKS, true);
            isEnabled.put(ParserType.STYLES, true);
            isEnabled.put(ParserType.NEWLINES, true);
            isEnabled.put(ParserType.ANCHORS, true);


            Element toElement = null;
            // bilgi
            if (cherryImagesMap.containsKey(dataOffset) && isEnabled.get(ParserType.BINARY_FILES)) {

                toElement = imgOrAttachment(cherryImagesMap, dataOffset, nodeId);

            } else if (cherryCodeBoxesMap.containsKey(dataOffset) && isEnabled.get(ParserType.CODE_BOXES)) {

                toElement = codebox(cherryCodeBoxesMap, dataOffset, nodeId);

            } else if (cherryGridsMap.containsKey(dataOffset) && isEnabled.get(ParserType.GRIDS)) {

                toElement = grid(cherryGridsMap, dataOffset, nodeId);

            } else if(sourceRichTextTag.hasAttribute(LINK_ATTR) && isEnabled.get(ParserType.LINKS)) {

                toElement = link(sourceRichTextTag.getAttribute(LINK_ATTR), dataOffset, nodeId);

            } else if(sourceRichTextTag.hasAttribute(SCALE_ATTR) && isEnabled.get(ParserType.SCALES)) {

                toElement = scale(sourceRichTextTag.getAttribute(SCALE_ATTR), dataOffset, nodeId);

            } else {
                if(isEnabled.get(ParserType.SPANS)) {
                    toElement = span();
                }
            }

        if(toElement != null) {

            toElement.setAttribute(STYLE_ATTR, CSS_WHITE_SPACE_PRE);


            if(isEnabled.get(ParserType.ANCHORS)) {

                if (cherryAnchorsMap.containsKey(dataOffset)) {
                    Anchor anchor = cherryAnchorsMap.get(dataOffset);
                    toElement.setAttribute(ID_ATTR, anchor.getAnchor());
                }
            }

            if(isEnabled.get(ParserType.NEWLINES)) {

                String lines[] = splitTextContent(textContent);


                // Metni satir satir ayir ve her satiri isle
                int lineNumber = 1; // bilgi: debug icin satir nosu.
                for (String line : lines) {
                 //   dataOffset += line.length(); // bilgi hassas konu !!!
                    if (line == BR_TAG_PLACEHOLDER) {

                        Element brElement = newDocument.createElement("br");
                        toElement.appendChild(brElement);

                    } else {

                        /*
                        if(line.startsWith(" ") || line.startsWith("\t")) {
                            line = replaceLeadingWhitespaceWithHtml(line);
                        }
                        Text textNode = newDocument.createTextNode(line);
                        */
                        List<Text> textList = replaceLeadingWhitespaceWithHtml(line);
                        for(Text textNode : textList) {
                            toElement.appendChild(textNode);
                        }
                    }

                    lineNumber++;
                }



            } else { // bilgi: newline lara bolmesini istemezsek asagidaki gibi yani dokunmadan kullanir icerigi.
                Text textNode = newDocument.createTextNode(textContent);
                toElement.appendChild(textNode);
            }

            // TODO DEBUG KAPSAMINA AL
            toElement = transferAttributes(toElement, sourceRichTextTag);

            if(isEnabled.get(ParserType.STYLES)) {
                String inlineCSS = cssStyling(sourceRichTextTag); // bilgi
                String totalCSS = inlineCSS + toElement.getAttribute(STYLE_ATTR);
                toElement.setAttribute(STYLE_ATTR, totalCSS);
            }




            Node importedNode = newDocument.importNode(toElement, true);
            targetRootNode.appendChild(importedNode);

        } else {
            System.out.println("toElement is NULL: ");
        }










            // bilgi

            // DUZELTME
            if (dataLength == 0) {
                dataOffset = dataOffset + 1;
            } else {
                dataOffset += dataLength;
            }
            // DUZELTME



        }


        // Document'i String'e donustur
        StringWriter writer = new StringWriter();
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer();
        //transformer.setOutputProperty(OutputKeys.INDENT, "yes");
       // transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");//bilgi ilk XML satirini remove eder
        transformer.transform(new DOMSource(newDocument), new StreamResult(writer));

        String transformedXml = writer.toString();
        return transformedXml;
    }

    public static Element transferAttributes(Element toElement, Element fromElement) {
        for (int i = 0; i < fromElement.getAttributes().getLength(); i++) {
            toElement.setAttribute(fromElement.getAttributes().item(i).getNodeName(),
                    fromElement.getAttributes().item(i).getNodeValue());
        }
        return toElement;
    }

    public static Document createNewXmlDocument() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();
        return doc;
    }


    /**
     * CherryTree pango xml yapisindan baska yerde gormedigim, #FFFFCCCC9999 tipindeki duplicate yazilmis renk bilgisini
     * # karakteri dahil 13 karakterli renk kodlamasini #FFCC99 tipindeki bildigimiz renk gosterimine donusturur.
     * @param color
     * @return
     */
    public static String cherryToCSSColor(String color) {
        if (color.length() == 13 && color.charAt(0) == '#') {
            String sonuc = "#" + color.substring(1, 3) + color.substring(5, 7) + color.substring(9,11);
            return sonuc;
        }
        return color;
    }


    // bilgi ELEMENT PARSER METODLARI ASAGIDA

    private static Element imgOrAttachment(Map<Integer, Image> cherryImagesMap, int dataOffset, long nodeId) {
        System.out.println("var biseyler");
        Element toElement = null;
        if(isEnabled.get(ParserType.BINARY_FILES)) {
            if (cherryImagesMap.get(dataOffset).getFileName().equals("") && isEnabled.get(ParserType.IMAGES)) {
                toElement = newDocument.createElement(IMG_TAG);
                toElement.setAttribute(DATA_TYPE_ATTR, "cherryimage");
                toElement.setAttribute(SRC_ATTR, String.format("/images/%s/%s", nodeId, dataOffset));
            } else {
                if (isEnabled.get(ParserType.ATTACHMENTS)) {
                    toElement = newDocument.createElement(A_TAG);
                    toElement.setAttribute(DATA_TYPE_ATTR, "cherryattachment");
                    toElement.setAttribute(HREF_ATTR, String.format("/download/%s/%s", nodeId, dataOffset));
                    toElement.setTextContent("\uD83D\uDCCE" + cherryImagesMap.get(dataOffset).getFileName()); // 📎
                }
            }
        }
        return toElement;
    }


    private static Element codebox(Map<Integer, CodeBox> cherryCodeBoxesMap, int dataOffset, long nodeId) {
        Element toElement = newDocument.createElement(A_TAG);
        toElement.setAttribute(DATA_TYPE_ATTR, "cherrycodebox");
        toElement.setAttribute(HREF_ATTR, String.format("/codeboxes/%s/%s", nodeId, dataOffset));
        toElement.setTextContent("CODEBOX: " + cherryCodeBoxesMap.get(dataOffset).getSyntax());
        return toElement;
    }

    private static Element grid(Map<Integer, Grid> cherryGridsMap, int dataOffset, long nodeId) {
        Element toElement = newDocument.createElement(A_TAG);
        toElement.setAttribute(DATA_TYPE_ATTR, "cherrygrid");
        toElement.setAttribute(HREF_ATTR, String.format("/grids/%s/%s", nodeId, dataOffset));
        toElement.setTextContent("GRID: " + cherryGridsMap.get(dataOffset).getId().getOffset());
        return toElement;
    }

    private static Element link(String rawUri, int dataOffset, long nodeId) {
        Element toElement;

        toElement = newDocument.createElement(A_TAG);


        String raw[] = rawUri.split(" ");
        String uriType = raw[0];
        String uri = raw[1];
        CherryUriType cherryUriType = null;
        switch (CherryUriType.from(uriType)) {
            case EXTERNAL:
                cherryUriType = CherryUriType.EXTERNAL; // ⧉
                toElement.setAttribute(REL_ATTR, "noreferrer noopener");
                toElement.setAttribute(TARGET_ATTR, "_blank");
                toElement.setAttribute(HREF_ATTR, uri);
                break;
            case INTERNAL:
                if(raw.length < 3) {
                    cherryUriType = CherryUriType.INTERNAL;
                    toElement.setAttribute(HREF_ATTR, "/nodes/" + uri); // 🔗
                } else {
                    cherryUriType = CherryUriType.INTERNAL_ANCHOR; // ⚓
                    String anchorName = raw[2];
                    toElement.setAttribute(HREF_ATTR, "/nodes/" + uri + "#" + anchorName);
                }
                break;
            case FOLD:
                cherryUriType = CherryUriType.FOLD; // 📁
                byte[] decodedBytes = Base64.getDecoder().decode(uri);
                String decodedUri = new String(decodedBytes);
                toElement.setAttribute(HREF_ATTR, "file:///" + decodedUri);
                break;
            case FILE:
                cherryUriType = CherryUriType.FILE; // 🗎
                decodedBytes = Base64.getDecoder().decode(uri);
                decodedUri = new String(decodedBytes);
                toElement.setAttribute(HREF_ATTR, "file:///" + decodedUri);
                break;
        }
        toElement.setAttribute(DATA_TYPE_ATTR, "cherrylink-" + cherryUriType);
        return toElement;
    }


    private static Element scale(String tagName, int dataOffset, long nodeId) {
        Element toElement;

        // h1-h6, (small, sup, sub) ->   font-size: smaller; yapman gerekir.
        // <!-- scale:  h1-h6, small, sup, sub, Default CSS Settings
        // https://www.w3schools.com/tags/tag_hn.asp -->

        if(tagName.equals(H1_TAG)) {
            toElement = newDocument.createElement(H1_TAG);
            toElement.setAttribute(STYLE_ATTR, CSS_H1_TO_H6_DISPLAY_INLINE);
        } else if(tagName.equals(H2_TAG)) {
            toElement = newDocument.createElement(H2_TAG);
            toElement.setAttribute(STYLE_ATTR, CSS_H1_TO_H6_DISPLAY_INLINE);
        } else if(tagName.equals(H3_TAG)) {
            toElement = newDocument.createElement(H3_TAG);
            toElement.setAttribute(STYLE_ATTR, CSS_H1_TO_H6_DISPLAY_INLINE);
        } else if(tagName.equals(H4_TAG)) {
            toElement = newDocument.createElement(H4_TAG);
            toElement.setAttribute(STYLE_ATTR, CSS_H1_TO_H6_DISPLAY_INLINE);
        } else if(tagName.equals(H5_TAG)) {
            toElement = newDocument.createElement(H5_TAG);
            toElement.setAttribute(STYLE_ATTR, CSS_H1_TO_H6_DISPLAY_INLINE);
        } else if(tagName.equals(H6_TAG)) {
            toElement = newDocument.createElement(H6_TAG);
            toElement.setAttribute(STYLE_ATTR, CSS_H1_TO_H6_DISPLAY_INLINE);
        } else {
            // (small, sup, sub)
            toElement = newDocument.createElement(tagName); // bilgi: doğrudan element yapilmalarinda sakinca yok
        }
        return toElement;
    }

    private static Element span() {
        Element toElement;

        toElement = newDocument.createElement(SPAN_TAG);
        //toElement = transferAttributes(toElement, sourceRichTextTag);
        toElement.setAttribute(DATA_TYPE_ATTR, "cherrycontent");
        //toElement.setAttribute("style", "xxx");

        return toElement;
    }

    private static String cssStyling(Element sourceRichTextTag) {


        // BASLA CSS STYLE ATTRIBUTE

        StringBuilder inlineCSS = new StringBuilder();

        if(sourceRichTextTag.hasAttribute(BACKGROUND_ATTR)) {
            inlineCSS.append(String.format("background-color: %s;", cherryToCSSColor(sourceRichTextTag.getAttribute(BACKGROUND_ATTR))));
        }
        if(sourceRichTextTag.hasAttribute(FOREGROUND_ATTR)) {
            inlineCSS.append(String.format("color: %s;", cherryToCSSColor(sourceRichTextTag.getAttribute(FOREGROUND_ATTR))));
        }
            /*
            if(sourceRichTextTag.hasAttribute("scale")) {
                String scale = sourceRichTextTag.getAttribute("scale");
                // h1-h6, (small, sup, sub) ->   font-size: smaller; yapman gerekir.
                // <!-- scale:  h1-h6, small, sup, sub, Default CSS Settings
                // https://www.w3schools.com/tags/tag_hn.asp -->
                String cssScale = null;
                if(scale.equals("sup")) {
                    cssScale = "super";
                    inlineCSS.append(String.format("font-size: smaller; vertical-align: %s;", cssScale));
                } else if(scale.equals("sub")) {
                    cssScale = "sub";
                    inlineCSS.append(String.format("font-size: smaller; vertical-align: %s;", cssScale));
                } else if(scale.equals("small")) {
                    inlineCSS.append(String.format("font-size: smaller;"));
                } else {
                    // h1-h6
                    inlineCSS.append(String.format("font-size: %s;", scale)); // todo
                }
            }

             */
        if(sourceRichTextTag.hasAttribute(WEIGHT_ATTR)) {
            // = heavy
            // font-weight: bold
            String weight = sourceRichTextTag.getAttribute(WEIGHT_ATTR);
            if(weight.equals("heavy")) {
                inlineCSS.append(String.format("font-weight: %s;", "bold"));
            }
        }
        if(sourceRichTextTag.hasAttribute(STYLE_ATTR)) {
            // = italic
            // font-weight: italic
            String style = sourceRichTextTag.getAttribute(STYLE_ATTR);
            if(style.equals("italic")) {
                inlineCSS.append(String.format("font-style: %s;", "italic"));
            }
        }
        if(sourceRichTextTag.hasAttribute(STRIKETHROUGH_ATTR) || sourceRichTextTag.hasAttribute(UNDERLINE_ATTR)) {
            Set<String> cssTextDecoration = new HashSet<>();
            if (sourceRichTextTag.hasAttribute(STRIKETHROUGH_ATTR)) {
                String strikethrough = sourceRichTextTag.getAttribute(STRIKETHROUGH_ATTR);
                if (strikethrough.equals("true")) {
                    cssTextDecoration.add("line-through");
                }
                // = true
                // https://blog.udemy.com/css-strikethrough/
                //textContent-decoration
                //line-through
            }
            if (sourceRichTextTag.hasAttribute(UNDERLINE_ATTR)) {
                String underline = sourceRichTextTag.getAttribute(UNDERLINE_ATTR);
                if (underline.equals("single")) {
                    cssTextDecoration.add("underline");
                }

                // = single
                //textContent-decoration
                //underline
            }
            inlineCSS.append(String.format("textContent-decoration: %s;", String.join(" ", cssTextDecoration)));
        }
        if(sourceRichTextTag.hasAttribute(FAMILY_ATTR)) {
            // = monospace
            //font-family:
            String family = sourceRichTextTag.getAttribute(FAMILY_ATTR);
            if (family.equals("monospace")) {
                inlineCSS.append(String.format("font-family: %s;", "monospace"));
            }
        }
        if(sourceRichTextTag.hasAttribute(JUSTIFICATION_ATTR)) {
            //   https://www.w3schools.com/cssref/tryit.php?filename=trycss_text-align
            String justification = sourceRichTextTag.getAttribute(JUSTIFICATION_ATTR);
            String textAlign = null;
            if(justification.equals("left")) {
                textAlign = "left";
                inlineCSS.append(String.format("margin-right:auto; display:table;"));
            } else if (justification.equals("right")) {
                textAlign = "right";
                inlineCSS.append(String.format("margin-left:auto; display:table;"));
            } else if (justification.equals("center")) {
                textAlign = "center";
                // https://stackoverflow.com/questions/8392211/html-span-align-center-not-working
                inlineCSS.append(String.format("margin:auto; display:table;"));
            } else if (justification.equals("fill")) {
                textAlign = "justify";
                inlineCSS.append(String.format("margin-left: auto; margin-right: auto; display:table;"));
            }
            // = left, right, center, fill
            //inlineCSS.append(String.format("textContent-align: %s;", textAlign));

        }

        return inlineCSS.toString();
    }


    /**
     * Bir metod yaz, String[] return edecek, her bir \n için dizi elemanı "<br />"  ye eşit olacak,
     * aralarda kalan bitişik diğer herşey, yani karakter öbekleri de ayrı birer dizi elemanı olacaklar.
     *
     *         String textContent = "\n\nabc\n\ndef\nghi\n\n\n";
     *
     * chatgpt tek defada asagidaki metodu hazirladi.
     * @param textContent
     * @return
     */
    public static String[] splitTextContent(String textContent) {
        List<String> resultList = new ArrayList<>();

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < textContent.length(); i++) {
            char c = textContent.charAt(i);
            if (c == '\n') {
                if (buffer.length() > 0) {
                    resultList.add(buffer.toString());
                    buffer.setLength(0);
                }
                resultList.add(BR_TAG_PLACEHOLDER); // bilgi hicbirsey yani yeni satir anlaminda
            } else {
                buffer.append(c);
            }
        }

        if (buffer.length() > 0) {
            resultList.add(buffer.toString());
        }

        return resultList.toArray(new String[0]);
    }


    /**
     * Bu metot, girdi olarak aldığı stringin başındaki boşluk ve tab karakterlerini HTML karşılıkları ile değiştirir.
     * Daha sonra, ilk boşluk veya tab karakteri olmayan karaktere kadar olan kısmı değiştirmez ve
     * geri kalanı olduğu gibi bırakır. Son olarak, tüm bu işlemlerin ardından sonucu tek bir string olarak döndürür.
     * @param input
     * @return
     */
    public static List<Text> replaceLeadingWhitespaceWithHtml(String input) {
        List<Text> textList = new ArrayList<>();
        //StringBuilder output = new StringBuilder();
        boolean foundNonWhitespace = false;

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);

            if (!foundNonWhitespace && (currentChar == ' ' || currentChar == '\t')) {
                //output.append(currentChar == ' ' ? "&nbsp;" : "&#09;"); // HTML karşılıkları
                //String s = currentChar == ' ' ? "&nbsp;" : "&#09;";
                String s = currentChar == ' ' ? " " : "\t";

                Text textNode = newDocument.createTextNode(String.valueOf(s));
                textList.add(textNode);
                //output.append(currentChar == ' ' ? " " : "\t"); // HTML karşılıkları
            } else {
                //output.append(input.substring(i)); // İlk bulunmayan karakterden itibaren geri kalanı ekler
                Text textNode = newDocument.createTextNode(input.substring(i));
                textList.add(textNode);
                break;
            }

            if (!foundNonWhitespace && !Character.isWhitespace(currentChar)) {
                foundNonWhitespace = true;
            }
        }

        //return output.toString();
        return textList;

    }
}



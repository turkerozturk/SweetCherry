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

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

@Component
public class GridTxtXmlTransformer {

    private static final String ROW_TAG = "row";
    private static final String CELL_TAG = "cell";

    private static final String TABLE_TAG = "table";
    private static final String TR_TAG = "tr";
    private static final String TD_TAG = "td";
    private static final String TH_TAG = "th";

    static Document newDocument;

    public static Document parse(String xmlContent) { //throws ParserConfigurationException, IOException, SAXException, TransformerException {

        String transformedXml = null;

        try {
            newDocument = NodeTxtXmlTransformer.createNewXmlDocument();

            Element targetRootNode = newDocument.createElement(TABLE_TAG);
            targetRootNode.setAttribute("class", "table border table-hover table-sm table-striped");

            newDocument.appendChild(targetRootNode);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document sourceDocument = dBuilder.parse(new InputSource(new StringReader(xmlContent)));
            NodeList trList = sourceDocument.getElementsByTagName(ROW_TAG);

            // Son öğeyi al ve ilk sıraya ekle, içindeki td'leri th'ye dönüştür
            Node lastTrNode = trList.item(trList.getLength() - 1);
            targetRootNode.appendChild(createTrElement(newDocument, lastTrNode, true));

            // Geri kalan elemanları ekle
            for (int tr = 0; tr < trList.getLength() - 1; tr++) {
                Node sourceTrNode = trList.item(tr);
                targetRootNode.appendChild(createTrElement(newDocument, sourceTrNode, false));
            }

            // Document'i String'e dönüştür
            StringWriter writer = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); // İlk XML satırını kaldır
            transformer.transform(new DOMSource(newDocument), new StreamResult(writer));
            transformedXml = writer.toString();

            // Dönüştürülen XML'i kontrol için eklenen adım
            System.out.println(transformedXml);

        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            throw new RuntimeException(e);
        }

        return newDocument;
    }

    private static Element createTrElement(Document document, Node sourceTrNode, boolean isHeader) {
        Element targetTrTag = document.createElement(TR_TAG);
        NodeList tdList = ((Element) sourceTrNode).getElementsByTagName(CELL_TAG);
        for (int td = 0; td < tdList.getLength(); td++) {
            Element sourceTdTag = (Element) tdList.item(td);
            Element targetTdTag;
            if (isHeader) {
                targetTdTag = document.createElement(TH_TAG);
            } else {
                targetTdTag = document.createElement(TD_TAG);
            }
            targetTdTag.setTextContent(sourceTdTag.getTextContent());
            targetTrTag.appendChild(targetTdTag);
        }
        return targetTrTag;
    }
}

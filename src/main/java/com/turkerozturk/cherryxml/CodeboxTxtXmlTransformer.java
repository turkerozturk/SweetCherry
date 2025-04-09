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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

@Component
public class CodeboxTxtXmlTransformer {


    private static final Logger logger = LoggerFactory.getLogger(CodeboxTxtXmlTransformer.class);


   // static Document newDocument;

    public static Document parse(String xmlContent) { //throws ParserConfigurationException, IOException, SAXException, TransformerException {


        String transformedXml = null;
        Document sourceDocument;
        try {
         //   newDocument = NodeTxtXmlTransformer.createNewXmlDocument();

         //   Element targetRootNode = newDocument.createElement(TABLE_TAG);
           // targetRootNode.setAttribute("class", "table table-hover");

          //  newDocument.appendChild(targetRootNode);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            sourceDocument = dBuilder.parse(new InputSource(new StringReader(xmlContent)));

            /*
            // Document'i String'e donustur
            StringWriter writer = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            //transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            // transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");//bilgi ilk XML satirini remove eder
            transformer.transform(new DOMSource(sourceDocument), new StreamResult(writer));
            transformedXml = writer.toString();
*/
        } catch (ParserConfigurationException e) {
            logger.error(e.getMessage());
            return null;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        } catch (SAXException e) {
            logger.error(e.getMessage());
            return null;
        }


        // return transformedXml;
        return sourceDocument;


    }


}

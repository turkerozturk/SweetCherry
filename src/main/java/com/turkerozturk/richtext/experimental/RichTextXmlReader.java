package com.turkerozturk.richtext.experimental;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Independent, experimental reader; not connected to the application's active parser. */
public final class RichTextXmlReader {
    /** Preserves empty runs and all attributes without interpreting embedded-object offsets. */
    public RichTextDocument read(String xml) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override public void fatalError(org.xml.sax.SAXParseException error) throws SAXException {
                    throw error;
                }
            });
            var root = builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            if (!"node".equals(root.getTagName()) || root.hasAttributes()) {
                throw new IllegalArgumentException("Expected a CTB node text root without attributes");
            }
            var runs = new ArrayList<RichTextDocument.TextRun>();
            int offset = 0;
            for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child instanceof Element element) {
                    if (!"rich_text".equals(element.getTagName())) {
                        throw new IllegalArgumentException("Unsupported XML element: " + element.getTagName());
                    }
                    for (Node content = element.getFirstChild(); content != null; content = content.getNextSibling()) {
                        if (content instanceof Element) {
                            throw new IllegalArgumentException("Nested elements in rich_text are unsupported");
                        }
                    }
                    var attributes = new LinkedHashMap<String, String>();
                    for (int i = 0; i < element.getAttributes().getLength(); i++) {
                        var attribute = element.getAttributes().item(i);
                        attributes.put(attribute.getNodeName(), attribute.getNodeValue());
                    }
                    var run = new RichTextDocument.TextRun(element.getTextContent(), attributes, offset);
                    runs.add(run);
                    offset += run.characterCount();
                } else if ((child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE)
                        && !child.getTextContent().isBlank()) {
                    throw new IllegalArgumentException("Text outside rich_text is unsupported");
                }
            }
            return new RichTextDocument(runs);
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("Invalid or prohibited rich-text XML", error);
        }
    }
}

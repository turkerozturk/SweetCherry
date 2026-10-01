package com.turkerozturk.richtext.experimental;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import com.turkerozturk.image.Image;
import com.turkerozturk.anchor.Anchor;
import com.turkerozturk.codebox.CodeBox;
import com.turkerozturk.grid.Grid;

/** Creates request-local snapshots; no repositories, database writes or application hooks. */
public final class EmbeddedContentAdapter {
    /** Classifies image-table records before accessing their binary contents. */
    public EmbeddedContent fromImage(Image image) {
        if (image.getAnchor() != null && !image.getAnchor().isEmpty()) {
            return new EmbeddedContent.Anchor(image.getAnchor());
        }
        if (image.getFileName() != null && !image.getFileName().isEmpty()) {
            return new EmbeddedContent.Attachment(image.getFileName());
        }
        return new EmbeddedContent.PngImage(image.getPng(), image.getLink(), image.getJustification());
    }
    public EmbeddedContent.Anchor fromAnchor(Anchor anchor) {
        return new EmbeddedContent.Anchor(anchor.getAnchor());
    }
    public EmbeddedContent.CodeBox fromCodeBox(CodeBox box) {
        return new EmbeddedContent.CodeBox(box.getTxt(), box.getSyntax());
    }
    public EmbeddedContent.Table fromTable(Grid table) { return readTable(table.getTxt()); }

    /** Reads only table/row/cell structure; rejects DTDs and preserves table metadata without applying CSS. */
    public EmbeddedContent.Table readTable(String xml) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override public void fatalError(org.xml.sax.SAXParseException error) throws org.xml.sax.SAXException {
                    throw error;
                }
            });
            var root = builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            if (!"table".equals(root.getTagName())) throw new IllegalArgumentException("Expected table XML");
            var attributes = new LinkedHashMap<String, String>();
            for (int i = 0; i < root.getAttributes().getLength(); i++) {
                var attr = root.getAttributes().item(i);
                attributes.put(attr.getNodeName(), attr.getNodeValue());
            }
            var rows = new ArrayList<java.util.List<String>>();
            for (var row : children(root, "row")) {
                var cells = new ArrayList<String>();
                for (var cell : children(row, "cell")) {
                    for (Node child = cell.getFirstChild(); child != null; child = child.getNextSibling()) {
                        if (child instanceof Element) throw new IllegalArgumentException("Nested table cell content");
                    }
                    cells.add(cell.getTextContent());
                }
                rows.add(cells);
            }
            return new EmbeddedContent.Table(rows, attributes);
        } catch (IllegalArgumentException error) { throw error; }
        catch (Exception error) { throw new IllegalArgumentException("Invalid or prohibited table XML", error); }
    }

    /** Refuses unexpected elements or non-whitespace text instead of silently losing data. */
    private java.util.List<Element> children(Element parent, String expected) {
        var result = new ArrayList<Element>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element) {
                if (!expected.equals(element.getTagName()) || element.hasAttributes()) {
                    throw new IllegalArgumentException("Unsupported table child: " + element.getTagName());
                }
                result.add(element);
            } else if ((child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE)
                    && !child.getTextContent().isBlank()) {
                throw new IllegalArgumentException("Unexpected table text");
            }
        }
        return result;
    }
}

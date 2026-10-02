package com.turkerozturk.richtext.experimental;

import java.io.StringWriter;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/** Serializes text-only CTB XML; never writes a database or modifies embedded-object records. */
public final class RichTextXmlWriter {
    /** Preserves every run and attribute while producing deterministic, escaped XML 1.0. */
    public String write(RichTextDocument document) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var xml = factory.newDocumentBuilder().newDocument();
            var root = xml.createElement("node");
            xml.appendChild(root);
            int expectedOffset = 0;
            for (var run : document.runs()) {
                if (run.textOffset() != expectedOffset) {
                    throw new IllegalArgumentException("Text runs must have contiguous code-point offsets");
                }
                validateCharacters(run.text());
                var element = xml.createElement("rich_text");
                for (var attribute : new TreeMap<>(run.attributes()).entrySet()) {
                    validateCharacters(attribute.getValue());
                    element.setAttribute(attribute.getKey(), attribute.getValue());
                }
                element.appendChild(xml.createTextNode(run.text()));
                root.appendChild(element);
                expectedOffset = Math.addExact(expectedOffset, run.characterCount());
            }
            var transformers = TransformerFactory.newInstance();
            transformers.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            var transformer = transformers.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            var result = new StringWriter();
            transformer.transform(new DOMSource(xml), new StreamResult(result));
            return result.toString();
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("Cannot serialize rich-text model as XML", error);
        }
    }

    /** Refuses XML-invalid controls and unpaired surrogates instead of silently replacing characters. */
    private void validateCharacters(String text) {
        text.codePoints().forEach(codePoint -> {
            boolean valid = codePoint == 9 || codePoint == 10 || codePoint == 13
                    || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                    || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                    || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
            if (!valid) throw new IllegalArgumentException("Character is not allowed in XML 1.0");
        });
    }
}

package com.turkerozturk.richtext.editing;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import com.turkerozturk.richtext.experimental.EmbeddedContentAdapter;

/** Converts plain CTB table cells between storage order (header last) and editor order (header first). */
final class RichTextTableCodec {
    private RichTextTableCodec() { }

    /** Keeps unsupported/oversized tables protected rather than flattening their data. */
    static List<List<String>> open(String xml) {
        var rows = new EmbeddedContentAdapter().readTable(xml).rows();
        validate(rows);
        var display = new ArrayList<List<String>>();
        display.add(rows.get(rows.size() - 1)); display.addAll(rows.subList(0, rows.size() - 1));
        return List.copyOf(display);
    }

    /** Reads only bounded rectangular string cell arrays, never pasted HTML or arbitrary XML. */
    static Map<String, List<List<String>>> decode(String json) {
        try {
            if (json == null || json.length() > 2_000_000) throw new IllegalArgumentException("Table data too large");
            var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            if (root == null || !root.isObject() || root.size() > 20) throw new IllegalArgumentException("Invalid table data");
            var result = new LinkedHashMap<String, List<List<String>>>();
            var fields = root.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                if (!field.getKey().matches("(?:new-table:[a-zA-Z0-9-]{1,64}|grid:[0-9]+)") || !field.getValue().isArray())
                    throw new IllegalArgumentException("Invalid table key");
                var rows = new ArrayList<List<String>>();
                for (var row : field.getValue()) {
                    if (!row.isArray()) throw new IllegalArgumentException("Invalid row");
                    var cells = new ArrayList<String>();
                    for (var cell : row) {
                        if (!cell.isTextual()) throw new IllegalArgumentException("Invalid cell");
                        cells.add(cell.textValue());
                    }
                    rows.add(List.copyOf(cells));
                }
                validate(rows); result.put(field.getKey(), List.copyOf(rows));
            }
            return Map.copyOf(result);
        } catch (java.io.IOException error) { throw new IllegalArgumentException("Invalid table data", error); }
    }

    /** Preserves existing table attributes and dimensions; new tables use automatic column widths. */
    static String write(List<List<String>> display, String original) {
        validate(display);
        Map<String, String> attributes;
        if (original != null) {
            var existing = new EmbeddedContentAdapter().readTable(original);
            validate(existing.rows());
            if (existing.rows().size() != display.size() || existing.rows().get(0).size() != display.get(0).size())
                throw new IllegalArgumentException("Existing table dimensions cannot be changed in this version");
            attributes = existing.attributes();
        } else attributes = Map.of("col_widths", String.join(",", java.util.Collections.nCopies(display.get(0).size(), "0")));
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var doc = factory.newDocumentBuilder().newDocument();
            var table = doc.createElement("table"); doc.appendChild(table);
            attributes.forEach(table::setAttribute);
            var storage = new ArrayList<List<String>>(display.subList(1, display.size())); storage.add(display.get(0));
            for (var row : storage) {
                var element = doc.createElement("row"); table.appendChild(element);
                for (var value : row) { var cell = doc.createElement("cell"); cell.appendChild(doc.createTextNode(value)); element.appendChild(cell); }
            }
            var transformers = TransformerFactory.newInstance();
            transformers.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            var output = new java.io.StringWriter(); transformers.newTransformer().transform(new DOMSource(doc), new StreamResult(output));
            return output.toString();
        } catch (Exception error) { throw new IllegalArgumentException("Cannot write table XML", error); }
    }

    private static void validate(List<List<String>> rows) {
        if (rows.isEmpty() || rows.size() > 100 || rows.get(0).isEmpty() || rows.get(0).size() > 20) throw new IllegalArgumentException("Invalid table dimensions");
        for (var row : rows) {
            if (row.size() != rows.get(0).size()) throw new IllegalArgumentException("Nonrectangular table");
            for (var cell : row) {
                if (cell == null || cell.length() > 5000 || cell.codePoints().anyMatch(c -> !compliant(c))) throw new IllegalArgumentException("Invalid cell text");
            }
        }
    }
    private static boolean compliant(int c) { return c == 9 || c == 10 || c == 13 || c >= 32 && c <= 0xD7FF || c >= 0xE000 && c <= 0xFFFD || c >= 0x10000 && c <= 0x10FFFF; }
}

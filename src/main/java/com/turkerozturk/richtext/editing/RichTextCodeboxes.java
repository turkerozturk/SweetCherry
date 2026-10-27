package com.turkerozturk.richtext.editing;

import java.util.*;
import com.turkerozturk.helpers.CodeIcon;
import com.turkerozturk.helpers.highlighter.CodeHighLighter;

/** Bounded codebox payloads; code is always data, never executed or parsed as HTML. */
public final class RichTextCodeboxes {
    private RichTextCodeboxes() { }
    public record Value(String text, String syntax) { }
    public record Language(String syntax, String icon) { }
    static List<Language> languages() {
        var names = new TreeSet<String>(CodeHighLighter.supportedSyntaxes());
        for (var icon : CodeIcon.values()) if (!icon.getSyntax().equals("other")) names.add(icon.getSyntax());
        names.remove("plain-text");
        var result = new ArrayList<Language>(); result.add(new Language("plain-text", "ct_code"));
        for (String name : names) result.add(new Language(name, CodeIcon.getCodeIconBySyntax(name).getNodeIcon().getIconName()));
        return List.copyOf(result);
    }
    static Map<String, Value> decode(String json) {
        try {
            if (json == null || json.length() > 2_000_000) throw new IllegalArgumentException("Codebox data too large");
            var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            if (root == null || !root.isObject() || root.size() > 20) throw new IllegalArgumentException("Invalid codebox data");
            var result = new LinkedHashMap<String, Value>();
            var fields = root.fields();
            while (fields.hasNext()) {
                var entry = fields.next(); var value = entry.getValue();
                if (!entry.getKey().matches("(?:new-codebox:[a-zA-Z0-9-]{1,64}|codebox:[0-9]+)")
                        || !value.isObject() || value.size() != 2 || !value.path("text").isTextual() || !value.path("syntax").isTextual())
                    throw new IllegalArgumentException("Invalid codebox");
                String text = value.get("text").textValue(), syntax = value.get("syntax").textValue();
                if (text.length() > 200_000 || text.codePoints().anyMatch(c -> !(c == 9 || c == 10 || c == 13 || c >= 32 && c <= 0xD7FF || c >= 0xE000 && c <= 0xFFFD || c >= 0x10000 && c <= 0x10FFFF))
                        || syntax.length() > 100 || !syntax.matches("[a-zA-Z0-9_+.#-]+")) throw new IllegalArgumentException("Invalid codebox text/syntax");
                result.put(entry.getKey(), new Value(text, syntax));
            }
            return Map.copyOf(result);
        } catch (java.io.IOException error) { throw new IllegalArgumentException("Invalid codebox data", error); }
    }
    static boolean supported(String syntax) { return languages().stream().anyMatch(language -> language.syntax().equals(syntax)); }
}

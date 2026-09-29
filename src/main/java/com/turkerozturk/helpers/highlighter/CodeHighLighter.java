package com.turkerozturk.helpers.highlighter;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class CodeHighLighter {
    private static final Set<String> LANGUAGES = loadLanguages();
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("sh", "bash"), Map.entry("shell", "bash"),
            Map.entry("dosbatch", "dos"), Map.entry("batch", "dos"),
            Map.entry("js", "javascript"), Map.entry("python3", "python"),
            Map.entry("python2", "python"), Map.entry("html", "xml"),
            Map.entry("html5", "xml"), Map.entry("c++", "cpp"),
            Map.entry("c#", "csharp"), Map.entry("plain-text", "plaintext"));

    private static volatile boolean syntaxHighlightingEnabled;

    @Value("${myapp.syntax-highlighting.enabled:false}")
    public void setSyntaxHighlightingEnabled(boolean enabled) {
        syntaxHighlightingEnabled = enabled;
    }

    /** Returns a bundled language name or null; never inserts a CTB value into a script URL. */
    public static String languageFor(String syntax) {
        if (syntax == null) {
            return null;
        }
        String name = syntax.toLowerCase(Locale.ROOT);
        name = ALIASES.getOrDefault(name, name);
        return LANGUAGES.contains(name) ? name : null;
    }

    /** Renders escaped code inside controlled markup; the browser adds colors only when enabled. */
    public static String highlightLanguage(String languageName, String code) {
        String language = syntaxHighlightingEnabled ? languageFor(languageName) : null;
        String attribute = language == null ? "" : " class=\"cherry-highlight\" data-language=\"" + language + "\"";
        return "<pre class=\"highlight\"><code" + attribute + ">"
                + StringEscapeUtils.escapeHtml4(code == null ? "" : code) + "</code></pre>";
    }

    /** Accepts CherryTree syntax values, including its aliases for shell and Python. */
    public static String mappedhighlightLanguage(String languageName, String code) {
        return highlightLanguage(languageName, code);
    }

    /** Marks embedded code boxes for the browser without parsing untrusted text as XML. */
    public static String enabledLanguageFor(String syntax) {
        return syntaxHighlightingEnabled ? languageFor(syntax) : null;
    }

    /** Reads the language allowlist produced from the bundled highlight.js grammar filenames. */
    private static Set<String> loadLanguages() {
        try (InputStream input = CodeHighLighter.class.getResourceAsStream("/highlightjs/languages.txt")) {
            if (input == null) {
                throw new IllegalStateException("Bundled highlight.js language list is missing");
            }
            Set<String> languages = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                reader.lines().forEach(languages::add);
            }
            return Collections.unmodifiableSet(languages);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read bundled highlight.js language list", exception);
        }
    }
}

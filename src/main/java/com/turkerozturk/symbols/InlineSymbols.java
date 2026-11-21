package com.turkerozturk.symbols;

import java.util.*;
import java.util.regex.Pattern;

/** Shared allowlist. Markers are presentation metadata, never executable template expressions. */
public final class InlineSymbols {
    public record Icon(String kind, String name, String glyph, List<String> aliases) {}
    public record Match(int start, int end, Icon icon) {}
    private static final Pattern MARKER = Pattern.compile("\\{\\{([^{}\\r\\n]{1,100})\\}\\}");
    private static final Map<String, Icon> ICONS = load();
    private InlineSymbols() {}
    private static Map<String, Icon> load() {
        try (var input = InlineSymbols.class.getResourceAsStream("/symbols/catalog.properties")) {
            if (input == null) throw new IllegalStateException("Missing symbol catalogue");
            var properties = new java.util.Properties();
            properties.load(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8));
            var items = new ArrayList<Icon>();
            for (String key : properties.stringPropertyNames()) {
                String[] parts = properties.getProperty(key).split("\\|", -1);
                if (parts.length != 4) throw new IllegalStateException("Invalid bundled symbol");
                items.add(new Icon(parts[0], parts[1], parts[2], parts[3].isEmpty() ? List.of() : List.of(parts[3].split(","))));
            }
            var result = new HashMap<String, Icon>();
            for (var icon : items) {
                if (icon.kind().equals("ctb") && icon.name().matches("[A-Za-z0-9_-]{1,80}"))
                    result.put("ctb:" + icon.name().toLowerCase(Locale.ROOT), icon);
                else if (Set.of("solid", "regular", "brands").contains(icon.kind()) && icon.name().matches("[a-z0-9-]+") && icon.glyph().codePointCount(0, icon.glyph().length()) == 1) {
                    var names = new ArrayList<>(icon.aliases()); names.add(icon.name());
                    for (String name : names) if (name.matches("[a-z0-9-]+")) {
                        result.put("fa-" + icon.kind() + " fa-" + name, icon);
                        result.put(switch (icon.kind()) { case "solid" -> "fas"; case "regular" -> "far"; default -> "fab"; } + " fa-" + name, icon);
                    }
                } else throw new IllegalStateException("Invalid bundled symbol");
            }
            return Map.copyOf(result);
        } catch (java.io.IOException error) { throw new IllegalStateException("Unreadable symbol catalogue", error); }
    }
    public static Icon resolve(String body) {
        String key = body.replaceAll("(?U)[\\s\uFEFF]+", " ").trim();
        if (key.regionMatches(true, 0, "ctb:", 0, 4)) key = key.toLowerCase(Locale.ROOT);
        return ICONS.get(key);
    }
    public static List<Match> matches(String text) {
        var result = new ArrayList<Match>();
        var matcher = MARKER.matcher(Objects.toString(text, ""));
        while (matcher.find()) {
            var icon = resolve(matcher.group(1));
            if (icon != null && (matcher.start() == 0 || text.charAt(matcher.start()-1) != '{')
                    && (matcher.end() == text.length() || text.charAt(matcher.end()) != '}'))
                result.add(new Match(matcher.start(), matcher.end(), icon));
        }
        return result;
    }
    /** Only recognized icon markers disappear. Real Unicode and unrelated {{expressions}} survive. */
    public static String filename(String text) {
        String value = Objects.toString(text, ""); var out = new StringBuilder(); int at = 0;
        for (var match : matches(value)) { out.append(value, at, match.start()); at = match.end(); }
        return out.append(value.substring(at)).toString();
    }
    public static String plain(String text) {
        String value = Objects.toString(text, ""); var out = new StringBuilder(); int at = 0;
        for (var match : matches(value)) { out.append(value, at, match.start()).append('[').append(match.icon().name()).append(']'); at = match.end(); }
        return out.append(value.substring(at)).toString();
    }
}

package com.turkerozturk.richtext.editing;

import java.net.URI;
import java.util.Set;
import com.turkerozturk.richtext.experimental.RichTextDocument;

/** Restricts newly supplied links to CTB HTTP(S) links while preserving existing link metadata. */
final class ExternalRichTextLinks {
    private ExternalRichTextLinks() { }

    /** Allows unchanged legacy/internal links; validates every newly introduced nonempty link value. */
    static void validateChanges(RichTextDocument edited, RichTextDocument original) {
        Set<String> existing = original.runs().stream().map(run -> run.attributes().get("link"))
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        for (var run : edited.runs()) {
            String link = run.attributes().get("link");
            if (link != null && !link.isEmpty() && !existing.contains(link)) validate(link);
        }
    }

    /** Requires an absolute HTTP(S) URL with a host, without embedded credentials or control characters. */
    static void validate(String link) {
        if (!link.startsWith("webs ") || link.length() > 4096 || link.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Invalid external link");
        URI uri = URI.create(link.substring(5));
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null)
            throw new IllegalArgumentException("Only absolute HTTP(S) links are accepted");
    }
}

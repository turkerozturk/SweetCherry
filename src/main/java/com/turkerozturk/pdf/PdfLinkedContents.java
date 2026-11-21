package com.turkerozturk.pdf;

import com.turkerozturk.node.fragments.NodeFragmentService;
import com.turkerozturk.node.fragments.NodeFragmentSettings;
import com.turkerozturk.richtext.experimental.PdfObjectSelection;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** One export job, one inclusion budget; referenced bodies pass the fragment visibility gate. */
final class PdfLinkedContents {
    private final NodeFragmentService fragments;
    private final NodeFragmentSettings settings;
    private final PdfExportLimits limits;
    private final PdfObjectSelection objects;
    private int attempts;
    private int sequence;
    private long includedCharacters;

    PdfLinkedContents(NodeFragmentService fragments, NodeFragmentSettings settings,
            PdfExportLimits limits, PdfObjectSelection objects) {
        this.fragments = java.util.Objects.requireNonNull(fragments);
        this.settings = java.util.Objects.requireNonNull(settings);
        this.limits = limits; this.objects = objects;
    }

    String expand(String html, long contentId) {
        var document = Jsoup.parseBodyFragment(html);
        document.outputSettings().prettyPrint(false);
        expand(document.body(), contentId, Set.of(contentId), 0);
        String result = document.body().html();
        if (result.length() > limits.getMaxHtmlCharacters())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
        return result;
    }

    private void expand(Element body, long contentId, Set<Long> path, int depth) {
        // Snapshot: inserted descendants are traversed only by the recursive call.
        for (var link : body.select("a[href]")) {
            URI uri;
            long occurrenceId;
            try {
                uri = URI.create(link.attr("href"));
                if (uri.getScheme() != null || uri.getRawAuthority() != null
                        || uri.getPath() == null || !uri.getPath().matches("/nodes/[1-9][0-9]*")) continue;
                occurrenceId = Long.parseLong(uri.getPath().substring(7));
            } catch (IllegalArgumentException invalid) { continue; }
            if (occurrenceId == contentId && uri.getRawFragment() != null) continue;
            if (path.contains(occurrenceId) || depth >= settings.depth() || attempts >= settings.inclusions()) continue;
            attempts++;
            NodeFragmentService.Fragment fragment;
            try { fragment = fragments.fragmentForPdf(occurrenceId, objects); }
            catch (NodeFragmentService.Problem unavailable) { continue; }
            long targetId = Long.parseLong(fragment.contentId());
            if (path.contains(targetId)) continue;
            // Includes base64 objects. Keep repeated targets bounded across all subtree roots.
            long size = fragment.html().length() + (fragment.name() == null ? 0 : fragment.name().length());
            if (includedCharacters + size > Math.min(4_194_304L, limits.getMaxHtmlCharacters())) continue;
            includedCharacters += size;
            String prefix = "sc-fragment-" + (++sequence) + "-";
            var parsed = Jsoup.parseBodyFragment(fragment.html());
            parsed.outputSettings().prettyPrint(false);
            for (var item : parsed.select("[id]")) item.attr("id", prefix + item.id());
            for (var item : parsed.select("a[href]")) {
                try {
                    var href = URI.create(item.attr("href"));
                    boolean local = href.getScheme() == null && href.getRawAuthority() == null;
                    boolean own = ("/nodes/" + targetId).equals(href.getPath())
                            || ("/nodes/" + occurrenceId).equals(href.getPath());
                    if (local && href.getRawFragment() != null && (own || href.getPath() == null || href.getPath().isEmpty()))
                        item.attr("href", "#" + prefix + href.getRawFragment());
                } catch (IllegalArgumentException ignored) { /* Final PDF sanitizer removes unsafe links. */ }
            }
            var next = new HashSet<>(path); next.add(targetId);
            if ("custom-colors".equals(fragment.syntax())) expand(parsed.body(), targetId, next, depth + 1);
            PdfFragmentFlow.inline(parsed.body());
            var section = new Element("span").addClass("pdf-linked-content");
            if (!"custom-colors".equals(fragment.syntax())) section.attr("data-symbols-skip", "");
            // As in the reader's source-free view, start exactly where the link occurred.
            section.appendChildren(new java.util.ArrayList<>(parsed.body().childNodes()));
            link.replaceWith(section);
        }
    }
}

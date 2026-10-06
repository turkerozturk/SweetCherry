package com.turkerozturk.pdf;

/** Session preference contains no tenant or node data. */
public record PdfExportOptions(String paper, String orientation, boolean colors, boolean nodeTitle,
        boolean outline, boolean filenameHeader, boolean pageNumbers, boolean metadata,
        boolean sourceName, boolean sourceAddress) implements java.io.Serializable {
    public PdfExportOptions {
        if (paper == null || orientation == null || !java.util.Set.of("A4", "Letter").contains(paper)
                || !java.util.Set.of("portrait", "landscape").contains(orientation))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        if (!metadata) { sourceName = false; sourceAddress = false; }
    }
    public static PdfExportOptions defaults() {
        return new PdfExportOptions("A4", "portrait", true, true, true, false, false, false, false, false);
    }
}

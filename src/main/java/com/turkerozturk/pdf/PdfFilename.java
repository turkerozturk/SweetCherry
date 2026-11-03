package com.turkerozturk.pdf;

public final class PdfFilename {
    private PdfFilename() {}
    public static String create(String title, long nodeId) {
        String name = title == null ? "Node-" + nodeId : title;
        name = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").strip();
        if (name.isBlank()) name = "Node-" + nodeId;
        if (name.length() > 120) name = name.substring(0, 120);
        return name + ".pdf";
    }
}

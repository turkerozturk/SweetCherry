package com.turkerozturk.richtext.experimental;

/** PDF-only preferences; CTB contents and live rendering remain unchanged. */
public record PdfObjectSelection(boolean images, boolean codeboxes, boolean tables, boolean latex)
        implements java.io.Serializable {
    public static PdfObjectSelection all() { return new PdfObjectSelection(true, true, true, true); }
    public boolean includes(RichTextLayout.EmbeddedObject ref, EmbeddedContent payload) {
        if (isLatex(payload)) return latex;
        return switch (ref.kind()) {
            case IMAGE -> images;
            case CODEBOX -> codeboxes;
            case TABLE -> tables;
            default -> true;
        };
    }
    public String kind(RichTextLayout.EmbeddedObject ref, EmbeddedContent payload) {
        return isLatex(payload) ? "latex" : ref.kind().name().toLowerCase(java.util.Locale.ROOT);
    }
    private boolean isLatex(EmbeddedContent payload) {
        return payload instanceof EmbeddedContent.Attachment attachment
                && "__ct_special.tex".equals(attachment.filename());
    }
}

package com.turkerozturk.pdf;

/** Small embedded paperclip, independent of emoji fonts or external assets. */
final class PdfAttachmentIcon {
    static final String URI = create();
    private static String create() {
        var image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(java.awt.Color.DARK_GRAY);
            graphics.setStroke(new java.awt.BasicStroke(1.4f));
            var path = new java.awt.geom.Path2D.Double();
            path.moveTo(5, 11); path.lineTo(10, 6); path.curveTo(12, 4, 10, 2, 8, 4);
            path.lineTo(3, 9); path.curveTo(-1, 13, 4, 18, 8, 14); path.lineTo(14, 8);
            path.curveTo(19, 3, 12, -2, 8, 2); path.lineTo(2, 8); graphics.draw(path);
            var output = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", output);
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
        finally { graphics.dispose(); }
    }
}

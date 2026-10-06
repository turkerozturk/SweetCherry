package com.turkerozturk.pdf;

import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import org.xhtmlrenderer.resource.ImageResource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Offline XHTML renderer: fonts come from the bundled fonts-extra JAR, images from CTB snapshots. */
@Component
public class NodePdfRenderer {
    public byte[] render(org.w3c.dom.Document document) {
        try {
            var renderer = new ITextRenderer();
            renderer.getSharedContext().setUserAgentCallback(new ITextUserAgent(renderer.getOutputDevice(), renderer.getSharedContext().getDotsPerPixel()) {
                @Override public InputStream resolveAndOpenStream(String uri) {
                    String path = "resources/css/XhtmlNamespaceHandler.css";
                    var resource = NodePdfRenderer.class.getClassLoader().getResource(path);
                    return resource != null && resource.toExternalForm().equals(uri)
                            ? NodePdfRenderer.class.getClassLoader().getResourceAsStream(path) : null;
                }
                @Override public byte[] getBinaryResource(String uri) {
                    if (!uri.matches("sc-font:Liberation(Sans|Mono)-(Regular|Bold|Italic|BoldItalic)\\.ttf")) return null;
                    String path = "liberation/" + uri.substring("sc-font:".length());
                    try (var input = NodePdfRenderer.class.getClassLoader().getResourceAsStream(path)) {
                        if (input == null) throw new IllegalStateException("Bundled PDF font missing: " + path);
                        return input.readAllBytes();
                    } catch (java.io.IOException error) { throw new IllegalStateException("PDF font could not be read", error); }
                }
                @Override public ImageResource getImageResource(String uri) {
                    if (!uri.startsWith("data:image/png;base64,")) throw new IllegalArgumentException("Non-embedded PDF image");
                    var resource = super.getImageResource(uri);
                    if (resource.getImage() == null) throw new IllegalArgumentException("Unreadable PDF image");
                    return resource;
                }
            });
            renderer.setDocument(document, null);
            renderer.layout();
            var output = new ByteArrayOutputStream();
            renderer.createPDF(output);
            return output.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("Node PDF rendering failed", error);
        }
    }
}

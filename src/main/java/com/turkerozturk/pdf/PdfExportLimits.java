package com.turkerozturk.pdf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Positive PDF workload limits; defaults preserve the existing Raspberry Pi policy. */
@Component
@ConfigurationProperties(prefix = "myapp.pdf")
public class PdfExportLimits {
    private int maxNodes = 512, maxDepth = 64, maxNodeTextCharacters = 8 * 1024 * 1024;
    private int maxHtmlCharacters = 64 * 1024 * 1024, maxImageBytes = 48 * 1024 * 1024;
    private int maxImagePixels = 40_000_000;
    public int getMaxNodes() { return maxNodes; }
    public void setMaxNodes(int value) { maxNodes = positive(value); }
    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int value) { maxDepth = positive(value); }
    public int getMaxNodeTextCharacters() { return maxNodeTextCharacters; }
    public void setMaxNodeTextCharacters(int value) { maxNodeTextCharacters = positive(value); }
    public int getMaxHtmlCharacters() { return maxHtmlCharacters; }
    public void setMaxHtmlCharacters(int value) { maxHtmlCharacters = positive(value); }
    public int getMaxImageBytes() { return maxImageBytes; }
    public void setMaxImageBytes(int value) { maxImageBytes = positive(value); }
    public int getMaxImagePixels() { return maxImagePixels; }
    public void setMaxImagePixels(int value) { maxImagePixels = positive(value); }
    private int positive(int value) {
        if (value < 1) throw new IllegalArgumentException("PDF limits must be positive");
        return value;
    }
}

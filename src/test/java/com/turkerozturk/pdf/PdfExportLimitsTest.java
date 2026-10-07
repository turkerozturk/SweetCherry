package com.turkerozturk.pdf;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PdfExportLimitsTest {
    @Test void defaultsMatchExistingLimitsAndRejectNonpositiveValues() {
        var limits = new PdfExportLimits();
        assertThat(limits.getMaxNodes()).isEqualTo(512);
        assertThat(limits.getMaxDepth()).isEqualTo(64);
        assertThat(limits.getMaxNodeTextCharacters()).isEqualTo(8388608);
        assertThat(limits.getMaxHtmlCharacters()).isEqualTo(67108864);
        assertThat(limits.getMaxImageBytes()).isEqualTo(50331648);
        assertThat(limits.getMaxImagePixels()).isEqualTo(40000000);
        assertThatThrownBy(() -> limits.setMaxNodes(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void relaxedHtmlLimitAcceptsContentRejectedByLowerLimit() {
        var limits = new PdfExportLimits(); limits.setMaxHtmlCharacters(10);
        var document = new NodePdfDocument(limits);
        assertThatThrownBy(() -> document.prepare("Title", "<div>Long content</div>", 1))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(413));
        limits.setMaxHtmlCharacters(100);
        assertThat(document.prepare("Title", "<div>Long content</div>", 1)
                .getDocumentElement().getTextContent()).contains("Long content");
    }

    @Test void bindsCustomLimitsFromConfigurationProperties() {
        var values = new java.util.HashMap<String, String>();
        values.put("myapp.pdf.max-nodes", "1024");
        values.put("myapp.pdf.max-depth", "128");
        var binder = new org.springframework.boot.context.properties.bind.Binder(
                new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(values));
        var limits = binder.bind("myapp.pdf", org.springframework.boot.context.properties.bind.Bindable.of(PdfExportLimits.class)).get();
        assertThat(limits.getMaxNodes()).isEqualTo(1024);
        assertThat(limits.getMaxDepth()).isEqualTo(128);
        assertThat(limits.getMaxImageBytes()).isEqualTo(50331648);
    }
}

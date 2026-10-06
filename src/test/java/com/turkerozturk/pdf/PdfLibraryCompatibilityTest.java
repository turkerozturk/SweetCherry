/*
 * This file is part of the SweetCherry project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/SweetCherry
 *
 * Copyright (c) 2024 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.pdf;

import org.junit.jupiter.api.Test;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the Java 17 PDF library combination before switching the node export renderer. */
class PdfLibraryCompatibilityTest {
    @Test void rendersReadablePdfWithClickableExternalLink() throws Exception {
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString("<html xmlns='http://www.w3.org/1999/xhtml'><head><title>Compatibility</title></head>"
                + "<body><h1>SweetCherry PDF</h1><p><a href='https://github.com/turkerozturk/SweetCherry'>Project</a></p></body></html>");
        renderer.layout();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        renderer.createPDF(output);
        try (var pdf = Loader.loadPDF(output.toByteArray())) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(pdf)).contains("SweetCherry PDF", "Project");
            assertThat(pdf.getPage(0).getAnnotations().stream()
                    .filter(PDAnnotationLink.class::isInstance).map(PDAnnotationLink.class::cast)
                    .map(PDAnnotationLink::getAction).filter(PDActionURI.class::isInstance)
                    .map(PDActionURI.class::cast).map(PDActionURI::getURI))
                    .contains("https://github.com/turkerozturk/SweetCherry");
        }
    }
}

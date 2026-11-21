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
package com.turkerozturk.helpers;

import org.springframework.stereotype.Component;

/** List exports share the offline renderer, Unicode fonts and symbol support of node PDFs. */
@Component
public class PDFFromHTMLHelper {
    public byte[] createPdfFromHtml(String sourceHTMLContent) {
        var input = org.jsoup.Jsoup.parse(sourceHTMLContent);
        var options = new com.turkerozturk.pdf.PdfExportOptions("A4", "portrait", true,
                false, false, false, true, false, false, false);
        var document = new com.turkerozturk.pdf.NodePdfDocument().prepare("SweetCherry",
                input.body().html(), 0, options, null);
        return new com.turkerozturk.pdf.NodePdfRenderer().render(document);
    }
}

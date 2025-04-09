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

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

@Component
public class PDFFromHTMLHelper {

    /**
     *
     * @param sourceHTMLContent
     * @return
     */
    public byte[] createPdfFromHtml(String sourceHTMLContent) {

        try (Document document = new Document(PageSize.A4);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, outputStream);
            document.open();
            HTMLWorker htmlWorker = new HTMLWorker(document);
            htmlWorker.parse(new StringReader(sourceHTMLContent));
            document.close();

            return outputStream.toByteArray();
        } catch (DocumentException | IOException exception) {

            System.out.println(exception.getMessage());
            return new byte[0];
        }
    }

}

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
package com.turkerozturk.image;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Controller
public class ImageController {
    @Autowired
    private ImageService imageService;

    @GetMapping("/images")
    public String getAllChildrenAsHtml(Model model) {
        model.addAttribute("images", imageService.getImages());
        return "images";
    }

    @GetMapping("/images/{node_id}")
    public String getImagesByNodeId(@PathVariable("node_id") Long nodeId, Model model) throws IOException {
        List<Image> images = imageService.getImagesByNodeIdAndPngIsNotNullAndFileNameIsEmpty(nodeId);
            model.addAttribute("images", images);
            model.addAttribute("imgUtil", new ImageUtil());
            return "image";
    }

    @GetMapping("/attachments")
    public String getAllAttachments(Model model,
                                    @CookieValue(value = "viewMode", defaultValue = "mobile") String viewMode) {
        List<Image> images = imageService.getAttachedFiles();
        if(!images.isEmpty()) {
            model.addAttribute("images", images);
        }

        model.addAttribute("viewMode", viewMode);

        if ("mobile".equals(viewMode)) {
            return "attachmentsMobile";
        } else {
            return "attachments";
        }

    }

    @GetMapping("/download/{node_id}/{offset}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("node_id") Long nodeId,
                                                 @PathVariable("offset") Integer offset,
                                                 Model model, HttpServletRequest request) {

        Optional<Image> imageNode = imageService.findByNodeIdAndOffset(nodeId, offset);

        if (imageNode.isPresent()) {
            Image image = imageNode.get();
            byte[] fileBytes = image.getPng(); // Bu aslinda png degil, sutunun adi oyle.

            String fileName = image.getFileName();
            String fileType = getFileExtension(fileName);

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

            switch (fileType.toLowerCase()) {
                case "pdf":
                    mediaType = MediaType.APPLICATION_PDF;
                    break;
                // https://stackoverflow.com/questions/4212861/what-is-a-correct-mime-type-for-docx-pptx-etc
                case "docx":
                    mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                    break;
                case "pptx":
                    mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
                    break;
                case "zip":
                    mediaType = MediaType.parseMediaType("application/zip");
                    break;
                case "txt":
                    mediaType = MediaType.TEXT_PLAIN;
                    break;
                case "exe":
                    // Uygulama/x-msdownload, application/octet-stream veya baska bir uygun medya turu secebilirsiniz.
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                    break;
                // Diger dosya turleri icin case'ler eklenebilir...
                default:
                    break;
            }

            ByteArrayResource resource = new ByteArrayResource(fileBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            String encodedFileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(encodedFileName).build());
            headers.setContentLength(fileBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/images/{node_id}/{offset}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] showPngImage(@PathVariable("node_id") Long nodeId, @PathVariable("offset") Integer offset, Model model, HttpServletRequest request) {

        Optional<Image> imageNode = imageService.findByNodeIdAndOffset(nodeId, offset);
        if (imageNode.isPresent()) {
            Image image = imageNode.get();
            String fileName = image.getFileName();
            if(fileName == null || fileName.trim().equals("")) {

                byte[] fileBytes = image.getPng(); // Bu aslinda png degil, sutunun adi oyle.
                return fileBytes;

            }
        }
        return null; // bilgi todo
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } else {
            return "";
        }
    }

}

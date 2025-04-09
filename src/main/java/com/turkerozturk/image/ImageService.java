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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ImageService {
    @Autowired
    private ImageRepository imageRepository;

    public List<Image> getImages() {
        List<Image> images = imageRepository.findAll();
        return images;
    }

    public List<Image> getImagesByNodeId(Long nodeId) {
        List<Image> images = imageRepository.findAllByNodeId(nodeId);
        return images;
    }

    public List<Image> getImagesByNodeIdAndPngIsNotNullAndFileNameIsEmpty(Long nodeId) {
        List<Image> images = imageRepository.findByNodeIdAndPngIsNotNullAndFileNameIsEmpty(nodeId);
        return images;
    }

    //Attachments of node
    public List<Attachment> getFindAllByNonEmptyFileName(Long nodeId) {
        List<Image> images = imageRepository.findAllByNonEmptyFileName(nodeId);
        List<Attachment> attachments = new ArrayList<>();
        for (Image image : images) {
            Attachment attachment = new Attachment(image);
            attachments.add(attachment);
        }

        return attachments;
    }

    public List<Image> getAttachedFiles() {
        List<Image> images = imageRepository.findAllByNonEmptyFileName();
        return images;
    }

    public Optional<Image> findByNodeIdAndOffset(Long nodeId, Integer offset) {
        return imageRepository.findByNodeIdAndOffset(nodeId, offset);
    }

}

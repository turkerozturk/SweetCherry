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
package com.turkerozturk.upload;


import com.turkerozturk.multipledatabases.MultitenantConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileAlreadyExistsException;
import java.util.Locale;

@Service
public class StorageService {
    public static final class UploadException extends IOException {
        private final String messageKey;
        UploadException(String messageKey) { super(messageKey); this.messageKey=messageKey; }
        public String messageKey() { return messageKey; }
    }




    public String uploadImageToFileSystem(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new UploadException("uploadPage.error.empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.length() > 120
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".txt")
                || !fileName.matches("[\\p{L}\\p{N} _.-]+")
                || fileName.startsWith(".") || fileName.contains("..")) {
            throw new UploadException("uploadPage.error.name");
        }
        Path directory = Path.of(MultitenantConfiguration.PATH_OF_ALL_DATA_SOURCE_CONNECTION_FILES)
                .toAbsolutePath().normalize();
        Path targetFileFullPath = directory.resolve(fileName).normalize();
        if (!targetFileFullPath.startsWith(directory)) {
            throw new UploadException("uploadPage.error.path");
        }
        try (var input = file.getInputStream()) {
            Files.copy(input, targetFileFullPath);
        } catch (FileAlreadyExistsException exception) {
            throw new UploadException("uploadPage.error.exists");
        }

        return fileName;
    }


}

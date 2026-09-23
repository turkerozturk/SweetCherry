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
package com.turkerozturk.export.exportedfiles;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/delete")
public class FileDeleteController {

    @Value("${user.dir}")
    private String applicationPath;

    @Value("${myapp.exportingFolderName}")
    private String exportingFolderName;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        String filePath = applicationPath + File.separator + exportingFolderName + File.separator + filename;
        File file = new File(filePath);

        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                return ResponseEntity.ok("File deleted successfully");
            } else {
                return ResponseEntity.status(500).body("Failed to delete file");
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}


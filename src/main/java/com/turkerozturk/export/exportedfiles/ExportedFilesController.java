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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class ExportedFilesController {

    private static final Logger logger = LoggerFactory.getLogger(ExportedFilesController.class);


    @Value("${user.dir}")
    private String applicationPath;

    @Value("${myapp.exportingFolderName}")
    private String exportingFolderName;

    @GetMapping("/exportedFiles")
    public String listFiles(Model model) {
        String directoryPath = applicationPath + File.separator + exportingFolderName;
        logger.info("Exported Files directory Path: " + directoryPath);

        File directory = new File(directoryPath);

        // Klasör yoksa oluştur
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".ctb") || name.toLowerCase().endsWith(".old"));

        List<FileInfo> fileInfos = new ArrayList<>();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                fileInfos.add(new FileInfo(file.getName(), file.length(), new Date(file.lastModified())));
            }
        }

        model.addAttribute("files", fileInfos);
        return "exportedFiles";
    }



}

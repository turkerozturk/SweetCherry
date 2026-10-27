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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.io.IOException;

@Controller
@RequestMapping("/delete")
public class FileDeleteController {

    @Value("${user.dir}")
    private String applicationPath;

    @Value("${myapp.exportingFolderName}")
    private String exportingFolderName;

    @PostMapping("/{filename:.+}")
    public ModelAndView deleteFile(@PathVariable String filename) {
        final Path file;
        try {
            file = ExportedFilePaths.resolve(applicationPath, exportingFolderName, filename);
        } catch (IllegalArgumentException exception) {
            return result(HttpStatus.BAD_REQUEST, "exportedFiles.deleteInvalid", filename);
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return result(HttpStatus.NOT_FOUND, "exportedFiles.deleteMissing", filename);
        }
        try {
            Files.delete(file);
            return result(HttpStatus.OK, "exportedFiles.deleteSuccess", filename);
        } catch (IOException exception) {
            return result(HttpStatus.INTERNAL_SERVER_ERROR, "exportedFiles.deleteFailed", filename);
        }
    }
    private ModelAndView result(HttpStatus status, String message, String filename) {
        var view = new ModelAndView("exportedFileResult");
        view.setStatus(status);
        view.addObject("deleteMessage", message);
        view.addObject("deleteSucceeded", status == HttpStatus.OK);
        view.addObject("filename", filename);
        return view;
    }
}

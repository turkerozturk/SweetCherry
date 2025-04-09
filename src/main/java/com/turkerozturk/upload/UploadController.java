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

import com.turkerozturk.helpers.highlighter.pygments.CodeHighLighter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class UploadController {

    @Autowired
    StorageService storageService;


/*
    @PostMapping("/fileSystem")
    public ResponseEntity<?> uploadImageToFIleSystem(@RequestParam("dosya") MultipartFile file) throws IOException {
        String uploadImage = storageService.uploadImageToFileSystem(file);
        return ResponseEntity.status(HttpStatus.OK)
                .body(uploadImage);
    }
*/

    @GetMapping("/upload-form")
    public String uploadForm(Model model) {

        String exampleDataSource="name=My Database\n" +
                "security-role=USER\n" +
                "datasource.url=jdbc:sqlite:C:/Users/username/Documents/CTB/myDatabase.ctb\n" +
                "datasource.driver-class-name=org.sqlite.JDBC\n" +
                "# datasource.username=admin\n" +
                "# datasource.password=admin\n" +
                "datasource.init-mode=always\n" +
                "# https://www.baeldung.com/multitenancy-with-spring-data-jpa";


        //model.addAttribute("codeHighLighter", new CodeHighLighter());

        CodeHighLighter codeHighLighter = new CodeHighLighter();

        String exampleDataSourceHighlighted = codeHighLighter.mappedhighlightLanguage("ini",
                exampleDataSource);

        model.addAttribute("exampleDataSourceHighlighted", exampleDataSourceHighlighted);


        return "upload-form";



    }

    @PostMapping("/upload-database")
    public String uploadImageToFileSystem(@RequestParam("dosya") MultipartFile file, Model model) throws IOException {
        try {
            String uploadImage = storageService.uploadImageToFileSystem(file);
            model.addAttribute("message", "Dosya başarılı bir şekilde yüklendi: " + uploadImage);
        } catch (IOException e) {
            model.addAttribute("message", "Dosya yükleme sırasında bir hata oluştu: " + e.getMessage());
        }
        return "upload";
    }


}

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

import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TargetFilePathHelper {

    final static String STATIC = "static";
    public static String getTargetPath(String sourcePath) {

        String resourcesPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        String targetPath = resourcesPath + STATIC + File.separator + sourcePath;

        // Remove leading slash if exists
        if (targetPath.startsWith("/")) {
            targetPath = targetPath.substring(1);
        }
        System.out.println(targetPath);

        return targetPath;
    }

    public static String getTargetPathWirhoutStatic(String sourcePath) {

        String resourcesPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        String targetPath = resourcesPath + sourcePath;

        // Remove leading slash if exists
        if (targetPath.startsWith("/")) {
            targetPath = targetPath.substring(1);
        }
        System.out.println(targetPath);

        return targetPath;
    }





}

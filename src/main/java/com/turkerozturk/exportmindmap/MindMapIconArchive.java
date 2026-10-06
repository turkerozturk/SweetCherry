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
package com.turkerozturk.exportmindmap;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Bundles the current classpath icon resources beside a FreeMind map, including from a packaged JAR. */
final class MindMapIconArchive {
    byte[] create(String filename, byte[] map) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(filename));
            zip.write(map);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("ctbicons/readme.txt"));
            zip.write(("These icons were taken from the CherryTree source code and converted and resized for SweetCherry.\n"
                    + "CherryTree: https://github.com/giuspen/cherrytree\n"
                    + "CherryTree is distributed under the GNU General Public License version 3 or later.\n"
                    + "Newer CherryTree versions may contain additional icons; some icons may be missing from this bundle.\n"
                    + "Extract the archive and keep ctbicons beside the .mm file.\n").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            Set<String> names = new HashSet<>();
            for (Resource icon : new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:static/img/ctbicons/*")) {
                String name = icon.getFilename();
                if (name == null || !name.matches("[A-Za-z0-9_.-]+\\.png") || !names.add(name)) continue;
                zip.putNextEntry(new ZipEntry("ctbicons/" + name));
                try (var input = icon.getInputStream()) { input.transferTo(zip); }
                zip.closeEntry();
            }
            if (names.isEmpty()) throw new IOException("CherryTree icon resources are unavailable");
        }
        return output.toByteArray();
    }
}

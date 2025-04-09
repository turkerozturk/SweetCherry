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

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

public class StringHelper {

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == 0 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " bytes";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
    }

    public static String[] splitStringWithOffsets(String text, List<Integer> offsets) {
        String[] pieces = new String[offsets.size() + 1];
        int start = 0;
        for (int i = 0; i < offsets.size(); i++) {
            int offset = offsets.get(i);
            pieces[i] = text.substring(start, offset);
            start = offset;
        }
        // Son parçayı al
        pieces[offsets.size()] = text.substring(start);
        return pieces;
    }

    public static boolean isPositiveInteger(String str) {
        // Bos string kontrolu
        if (str == null || str.isEmpty()) {
            return false;
        }

        // String'in her bir karakterini kontrol et
        for (int i = 0; i < str.length(); i++) {
            // Karakterin rakam olup olmadigini kontrol et
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }

        // String sadece rakamlardan olusuyorsa true don
        return true;
    }

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new Random();

    public static String generateRandomString(int length) {
        StringBuilder randomString = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            randomString.append(CHARACTERS.charAt(randomIndex));
        }

        return randomString.toString();
    }

}

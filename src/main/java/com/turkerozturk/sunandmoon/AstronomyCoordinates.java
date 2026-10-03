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
package com.turkerozturk.sunandmoon;

import java.util.Locale;
import java.util.regex.Pattern;

public final class AstronomyCoordinates {
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)");
    private static final Pattern DMS = Pattern.compile("([+-]?\\d+)°\\s*(\\d+)'\\s*(\\d+(?:\\.\\d+)?)\"");
    private AstronomyCoordinates() {}

    /** Parses signed decimal degrees or DMS, retaining fractional seconds and hemisphere. */
    public static double parse(String input, boolean latitude) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Coordinate is required");
        String value = input.trim().toUpperCase(Locale.ROOT);
        char direction = value.charAt(value.length() - 1);
        boolean hemisphere = "NSEW".indexOf(direction) >= 0;
        if (hemisphere) {
            if (latitude ? "NS".indexOf(direction) < 0 : "EW".indexOf(direction) < 0)
                throw new IllegalArgumentException("Coordinate hemisphere does not match axis");
            value = value.substring(0, value.length() - 1).trim();
            if (value.startsWith("-") || value.startsWith("+"))
                throw new IllegalArgumentException("Use a sign or hemisphere, not both");
        }
        double degrees;
        if (DECIMAL.matcher(value).matches()) {
            degrees = Double.parseDouble(value);
        } else {
            var parts = DMS.matcher(value);
            if (!parts.matches()) throw new IllegalArgumentException("Invalid coordinate format");
            double d = Double.parseDouble(parts.group(1));
            int minutes = Integer.parseInt(parts.group(2));
            double seconds = Double.parseDouble(parts.group(3));
            if (minutes >= 60 || seconds >= 60) throw new IllegalArgumentException("Invalid DMS minutes or seconds");
            degrees = Math.abs(d) + minutes / 60.0 + seconds / 3600.0;
            if (value.startsWith("-")) degrees = -degrees;
        }
        if (hemisphere && (direction == 'S' || direction == 'W')) degrees = -degrees;
        if (!Double.isFinite(degrees) || Math.abs(degrees) > (latitude ? 90 : 180))
            throw new IllegalArgumentException("Coordinate out of range");
        return degrees;
    }
}

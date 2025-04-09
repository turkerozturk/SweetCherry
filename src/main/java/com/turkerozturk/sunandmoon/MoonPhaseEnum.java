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

import java.util.HashMap;
import java.util.Map;

public enum MoonPhaseEnum {

    NEW_MOON(1, "\uD83C\uDF11", "NEW_MOON.gif", "NEW_MOON"),
    FIRST_QUARTER(2, "\uD83C\uDF13", "FIRST_QUARTER.gif", "FIRST_QUARTER"),
    FULL_MOON(3, "\uD83C\uDF15", "FULL_MOON.gif", "FULL_MOON"),
    LAST_QUARTER(4, "\uD83C\uDF17", "LAST_QUARTER.gif", "LAST_QUARTER");


    /*
    🌑 U+1F311 New Moon
    🌒 U+1F312 Waxing Crescent Moon
    🌓 U+1F313 First Quarter Moon
    🌘 U+1F318 Waning Crescent Moon
    🌕 U+1F315 Full Moon
    🌖 U+1F316 Waning Gibbous Moon
    🌗 U+1F317 Last Quarter Moon
    🌘 U+1F318 Waning Crescent Moon
     */


    int phaseNum;

    String phaseEmoticon;

    String phaseIconPath;

    String time4jName;


    MoonPhaseEnum(int phaseNum, String phaseEmoticon, String phaseIconPath, String time4jName) {
        this.phaseNum = phaseNum;
        this.phaseEmoticon = phaseEmoticon;
        this.phaseIconPath = phaseIconPath;
        this.time4jName = time4jName;
    }

    private static final Map<Integer, MoonPhaseEnum> _map = new HashMap<Integer, MoonPhaseEnum>();
    static {
        for (MoonPhaseEnum enumVariableName : MoonPhaseEnum.values())
            _map.put(enumVariableName.phaseNum, enumVariableName);
    }

    public static MoonPhaseEnum byPhaseNumber(Integer phaseNum) {
        MoonPhaseEnum moonPhaseEnum = _map.get(phaseNum);
        return moonPhaseEnum != null ? moonPhaseEnum : null;
    }

    private static final Map<String, MoonPhaseEnum> _map2 = new HashMap<String, MoonPhaseEnum>();
    static {
        for (MoonPhaseEnum enumVariableName : MoonPhaseEnum.values())
            _map2.put(enumVariableName.time4jName, enumVariableName);
    }

    public static MoonPhaseEnum byTime4jName(String phaseNum) {
        MoonPhaseEnum moonPhaseEnum = _map2.get(phaseNum);
        return moonPhaseEnum != null ? moonPhaseEnum : null;
    }

}

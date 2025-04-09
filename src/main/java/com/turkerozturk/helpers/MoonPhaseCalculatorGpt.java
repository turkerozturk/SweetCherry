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
/*
 * This file is part of the SweetCherry project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/morethanpomodoro
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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class MoonPhaseCalculatorGpt {

    public enum MoonPhase {
        NEW_MOON,
        WAXING_CRESCENT,
        FIRST_QUARTER,
        WAXING_GIBBOUS,
        FULL_MOON,
        WANING_GIBBOUS,
        LAST_QUARTER,
        WANING_CRESCENT
    }

    public static MoonPhase calculateMoonPhase(LocalDate date) {
        long daysSinceNewMoon = ChronoUnit.DAYS.between(LocalDate.of(2000, 1, 6), date);

        double synodicMonth = 29.53058868;
        double phase = (daysSinceNewMoon % synodicMonth) / synodicMonth;

        if (phase < 0.0625 || phase >= 0.9375) {
            return MoonPhase.NEW_MOON;
        } else if (phase < 0.1875) {
            return MoonPhase.WAXING_CRESCENT;
        } else if (phase < 0.3125) {
            return MoonPhase.FIRST_QUARTER;
        } else if (phase < 0.4375) {
            return MoonPhase.WAXING_GIBBOUS;
        } else if (phase < 0.5625) {
            return MoonPhase.FULL_MOON;
        } else if (phase < 0.6875) {
            return MoonPhase.WANING_GIBBOUS;
        } else if (phase < 0.8125) {
            return MoonPhase.LAST_QUARTER;
        } else {
            return MoonPhase.WANING_CRESCENT;
        }
    }

    public static void main(String[] args) {
        for(int i = 1; i <= 31; i++) {
            LocalDate date = LocalDate.of(2024, 3, i);
            MoonPhase phase = calculateMoonPhase(date);
            System.out.println("Moon phase on " + date + " is: " + phase);
        }
    }
}

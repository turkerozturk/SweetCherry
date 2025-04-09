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

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

// chatgpt tek defada cevirdi: https://raw.githubusercontent.com/rickycodes/timemoji/master/src/moonmoji.rs

public class MoonPhaseCalculator {
    private static final double PI = Math.PI;
    private static final double DAY_MILLIS = 1000.0 * 60.0 * 60.0 * 24.0;
    private static final double J1970 = 2440588.0;
    private static final double J2000 = 2451545.0;
    private static final double RADS = PI / 180.0;
    private static final double SDIST = 149598000.0;
    private static final double EARTH = RADS * 23.4397;
    private static final double ZERO = 0.0;
    private static final double ONE = 1.0;

    private static class MoonCoords {
        public double ra;
        public double dec;
        public double dist;

        public MoonCoords(double ra, double dec, double dist) {
            this.ra = ra;
            this.dec = dec;
            this.dist = dist;
        }
    }

    private static double toDays(ZonedDateTime date) {
        long millis = date.toInstant().toEpochMilli();
        return millis / DAY_MILLIS - 0.5 + J1970 - J2000;
    }

    private static MoonCoords moonCoords(double d) {
        double l = RADS * (218.316 + 13.176396 * d);
        double m = RADS * (134.963 + 13.064993 * d);
        double f = RADS * (93.272 + 13.229350 * d);

        double longi = l + RADS * 6.289 * Math.sin(m);
        double lat = RADS * 5.128 * Math.sin(f);

        return new MoonCoords(
                Math.atan2(Math.sin(longi) * Math.cos(EARTH) - Math.tan(lat) * Math.sin(EARTH), Math.cos(longi)),
                Math.asin(Math.sin(lat) * Math.sin(EARTH) + Math.cos(lat) * Math.cos(EARTH) * Math.sin(longi)),
                385001.0 - 20905.0 * Math.cos(m)
        );
    }

    private static double getPhase(ZonedDateTime date) {
        double d = toDays(date);

        MoonCoords m = moonCoords(d);

        double phi = Math.acos(Math.sin(EARTH) * Math.sin(m.dec) + Math.cos(EARTH) * Math.cos(m.dec) * Math.cos(0));
        double inc = Math.atan2(SDIST * Math.sin(phi), m.dist - SDIST * Math.cos(phi));
        double angle = Math.atan2(Math.sin(0) * Math.cos(EARTH) - Math.tan(m.dec) * Math.sin(EARTH), Math.cos(0));
        return 0.5 + 0.5 * inc * ONE * Math.signum(angle) / PI;
    }

    private static int stepPhase(double phase, Double randomValue) {
        double extraEmoji = randomValue != null && randomValue <= 0.1 ? 1 : 0;
        phase *= PHASE_WEIGHT;
        for (int i = 0; i < PHASES.length; i++) {
            phase -= PHASES[i].weight;
            if (phase < 0) {
                if (extraEmoji == 1 && i == 0) {
                    return 8;
                }

                if (extraEmoji == 1 && i == 4) {
                    return 9;
                }
                return i;
            }
        }
        return 0;
    }

    public static void main(String[] args) {
        //ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Istanbul"));
        for (int i = 0; i < 30; i++) {
            currentTime = currentTime.plusDays(1);
            double phase = getPhase(currentTime);
            System.out.println(currentTime.toLocalDate() + " - " + PHASES[stepPhase(phase, null)].name);
        }
    }

    private static class MoonMoji {
        public String emoji;
        public String name;
        public double weight;

        public MoonMoji(String emoji, String name, double weight) {
            this.emoji = emoji;
            this.name = name;
            this.weight = weight;
        }
    }

    private static final MoonMoji[] PHASES = {
            new MoonMoji("🌑", "New Moon", 1.0),
            new MoonMoji("🌒", "Waxing Crescent", 6.3825),
            new MoonMoji("🌓", "First Quarter", 1.0),
            new MoonMoji("🌔", "Waxing Gibbous", 6.3825),
            new MoonMoji("🌕", "Full Moon", 1.0),
            new MoonMoji("🌖", "Waning Gibbous", 6.3825),
            new MoonMoji("🌗", "Last Quarter", 1.0),
            new MoonMoji("🌘", "Waning Crescent", 6.3825),
            new MoonMoji("🌚", "New Moon *", 0.0),
            new MoonMoji("🌝", "Full Moon *", 0.0)
    };

    private static final double PHASE_WEIGHT = 1.0 + 6.3825 + 1.0 + 6.3825 + 1.0 + 6.3825 + 1.0 + 6.3825 + 0.0 + 0.0;
}

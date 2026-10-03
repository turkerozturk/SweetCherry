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

import org.shredzone.commons.suncalc.MoonTimes;
import org.shredzone.commons.suncalc.SunTimes;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CommonsSunCalc {
    /** Calculates rise/set events for the configured local day without network access. */
    public SolarSystem calculate(Instant now, ZoneId zone, double latitude, double longitude) {
        var date = now.atZone(zone).toLocalDate();
        // A local calendar day can last 23 or 25 hours at a daylight-saving transition.
        var window = Duration.between(date.atStartOfDay(zone), date.plusDays(1).atStartOfDay(zone));
        var sun = SunTimes.compute().on(date.getYear(), date.getMonthValue(), date.getDayOfMonth())
                .timezone(zone.getId()).latitude(latitude).longitude(longitude).limit(window).execute();
        var moon = MoonTimes.compute().on(date.getYear(), date.getMonthValue(), date.getDayOfMonth())
                .timezone(zone.getId()).latitude(latitude).longitude(longitude).limit(window).execute();
        return new SolarSystem(sun.getRise(), sun.getSet(), moon.getRise(), moon.getSet(),
                format(sun.getRise(), zone), format(sun.getSet(), zone),
                format(moon.getRise(), zone), format(moon.getSet(), zone));
    }

    /** A missing event is normal at some dates/latitudes and must not break the page. */
    private static String format(ZonedDateTime event, ZoneId zone) {
        return event == null ? "—" : event.withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}

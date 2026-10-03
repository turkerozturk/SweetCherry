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

import net.time4j.Moment;
import net.time4j.calendar.astro.MoonPhase;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class MoonTime4j {
    public record Snapshot(String moonPhaseWidget, String lastPhaseKey, String nextPhaseKey,
                           String lastFormatted, String nextFormatted, int illuminationPercent) {}

    /** Builds an immutable phase snapshot for the supplied instant and IANA time zone. */
    public Snapshot calculate(Instant now, ZoneId zone) {
        Moment moment = Moment.from(now);
        Moment previous = null, next = null;
        MoonPhase previousPhase = null, nextPhase = null;
        for (MoonPhase phase : MoonPhase.values()) {
            Moment before = phase.before(moment);
            Moment after = phase.atOrAfter(moment);
            if (previous == null || before.isAfter(previous)) { previous = before; previousPhase = phase; }
            if (next == null || after.isBefore(next)) { next = after; nextPhase = phase; }
        }
        var formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withZone(zone);
        String widget = MoonPhaseEnum.byTime4jName(previousPhase.name()).phaseEmoticon + " "
                + days(instant(previous), now) + ">" + days(now, instant(next)) + " "
                + MoonPhaseEnum.byTime4jName(nextPhase.name()).phaseEmoticon;
        return new Snapshot(widget, "astronomy.phase." + previousPhase.name(),
                "astronomy.phase." + nextPhase.name(), formatter.format(instant(previous)),
                formatter.format(instant(next)), (int) Math.round(MoonPhase.getIllumination(moment) * 100));
    }

    /** Converts the astronomical event to a Java instant for zone-aware presentation. */
    private static Instant instant(Moment moment) {
        return Instant.ofEpochSecond(moment.getPosixTime(), moment.getNanosecond());
    }

    /** Each dash represents a complete elapsed day, not the first digit of a duration string. */
    private static String days(Instant start, Instant end) {
        return "-".repeat((int) Math.max(0, Math.min(31, Duration.between(start, end).toDays())));
    }
}

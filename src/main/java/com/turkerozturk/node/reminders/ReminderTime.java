package com.turkerozturk.node.reminders;

import java.time.*;
import java.time.format.DateTimeFormatter;

/** Explicit zone and minute precision; never silently shift a DST gap or guess an overlap. */
public final class ReminderTime {
    private ReminderTime() { }
    public static Instant resolve(String local, String zone) {
        try {
            if (local == null || local.length() != 16 || zone == null || zone.length() > 100) throw new IllegalArgumentException();
            var date = LocalDateTime.parse(local); var rules = ZoneId.of(zone).getRules();
            if (date.getYear() < 1 || date.getYear() > 9999) throw new IllegalArgumentException();
            var offsets = rules.getValidOffsets(date);
            if (offsets.size() != 1) throw new IllegalArgumentException();
            return date.toInstant(offsets.get(0));
        } catch (DateTimeException invalid) { throw new IllegalArgumentException("invalidTime", invalid); }
    }
    public static String local(String instant, String zone) {
        return DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm").format(Instant.parse(instant).atZone(ZoneId.of(zone)));
    }
    public static String display(String instant, String zone) { return local(instant,zone).replace('T',' ') + " · " + zone; }
}

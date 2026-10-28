package com.turkerozturk.calendar;

import java.time.*;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CalendarSettings {
    private final DayOfWeek firstDay;private final ZoneId zone;private final int maxEntries;
    public CalendarSettings(@Value("${myapp.calendar.first-day-of-week:MONDAY}")String first,
            @Value("${myapp.calendar.time-zone:system}")String zone,@Value("${myapp.calendar.max-entries:10000}")int limit) {
        DayOfWeek parsed;ZoneId parsedZone;
        try{parsed=firstDay(first);}catch(RuntimeException invalid){parsed=DayOfWeek.MONDAY;org.slf4j.LoggerFactory.getLogger(getClass()).warn("Invalid myapp.calendar.first-day-of-week; using MONDAY (1).");}
        try{parsedZone="system".equalsIgnoreCase(zone)?ZoneId.systemDefault():ZoneId.of(zone);}
        catch(RuntimeException invalid){parsedZone=ZoneId.systemDefault();org.slf4j.LoggerFactory.getLogger(getClass()).warn("Invalid myapp.calendar.time-zone; using system time zone.");}
        this.firstDay=parsed;this.zone=parsedZone;maxEntries=Math.max(1,Math.min(50000,limit));
    }
    static DayOfWeek firstDay(String value){String text=value.strip().toUpperCase(Locale.ROOT);return text.matches("[1-7]")?DayOfWeek.of(Integer.parseInt(text)):DayOfWeek.valueOf(text);}
    public DayOfWeek firstDay(){return firstDay;}
    public ZoneId zone(){return zone;}
    public int maxEntries(){return maxEntries;}
    long timestamp(String value) {
        try {
            if(value==null||value.length()>16)throw new IllegalArgumentException();
            LocalDateTime local=LocalDateTime.parse(value);
            if(local.getYear()<1||local.getYear()>9999||local.getSecond()!=0||local.getNano()!=0)throw new IllegalArgumentException();
            var offsets=zone.getRules().getValidOffsets(local);
            if(offsets.isEmpty())throw new IllegalArgumentException(); // DST gaps must not silently shift the requested time.
            return local.toEpochSecond(offsets.get(0)); // Documented earlier offset for an overlap.
        }catch(RuntimeException invalid){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"invalidDate");}
    }
}

package com.turkerozturk.node.reminders;

import java.time.*;

/** Annual wall-clock recurrence keeps its original month/day, including a February 29 anchor. */
public final class AnnualReminder {
    private AnnualReminder() { }
    public static Instant occurrence(String anchor,String zone,String leapDay,int year) {
        if(year<1||year>9999||!java.util.Set.of("feb28","mar1").contains(leapDay))throw new IllegalArgumentException("invalidTime");
        var original=LocalDateTime.parse(anchor);int month=original.getMonthValue(),day=original.getDayOfMonth();
        if(month==2&&day==29&&!Year.isLeap(year)){if(leapDay.equals("mar1")){month=3;day=1;}else day=28;}
        // During recurrence only: advance DST gaps; select the earlier offset in overlaps.
        return LocalDate.of(year,month,day).atTime(original.toLocalTime()).atZone(ZoneId.of(zone)).withEarlierOffsetAtOverlap().toInstant();
    }
    public static Instant next(String anchor,String zone,String leapDay,Instant after) {
        int year=after.atZone(ZoneId.of(zone)).getYear();
        for(int y=Math.max(1,year);y<=9999;y++){var due=occurrence(anchor,zone,leapDay,y);if(due.isAfter(after))return due;}
        throw new IllegalArgumentException("invalidTime");
    }
    public static Instant current(String anchor,String zone,String leapDay,Instant stored,Instant now) {
        int year=now.atZone(ZoneId.of(zone)).getYear();var candidate=occurrence(anchor,zone,leapDay,year);
        if(candidate.isAfter(now)&&year>1)candidate=occurrence(anchor,zone,leapDay,year-1);
        return !candidate.isAfter(now)&&candidate.isAfter(stored)?candidate:stored;
    }
}

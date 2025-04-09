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

import jakarta.annotation.PostConstruct;
import net.time4j.*;
import net.time4j.calendar.astro.MoonPhase;
import net.time4j.engine.TimeMetric;
import net.time4j.format.expert.ChronoFormatter;
import net.time4j.format.expert.PatternType;
import net.time4j.tz.Timezone;
import net.time4j.tz.ZonalOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simdilik tek amaci Time4j kutuphanesini kullanarak icinde bulundugumuz iki ay halini gorsellestiren bir html string
 * icerik dondurmek. Bu sinifa moment yani simdiki zamani gonderip getMoonPhaseWidget() metodu ile html icerigi almak.
 * Ay hakkinda diger bilgileri Time4j ve commons-suncalc ile almak kolay. Ama bu ay fazlarini almak zor. Bir tam gunum
 * harcandi.
 */
@Component
public class MoonTime4j {

    @Value("${astronomy.zonalOffset}")
    private Integer zonalOffset;
    private Moment moment;
    private int illuminationPercent;
    private MoonPhaseEnum lastMoonPhase;
    private MoonPhaseEnum nextMoonPhase;
    private Moment lastMoonPhaseMoment;
    private Moment nextMoonPhaseMoment;
    private String moonPhaseWidget;
    private String moonPhaseWidgetSecondary;


    public MoonTime4j() {
    }

    @PostConstruct
    public void setup() {
        Moment moment = Moment.nowInSystemTime();
        this.moment = moment;
        this.illuminationPercent = (int) MoonPhase.getIllumination(moment);
        prepareLastMoonPhase(moment);
        prepareNextMoonPhase(moment);

        prepareMoonPhaseWidget();
        prepareMoonPhaseWidgetSecondary();
    }

    private void prepareMoonPhaseWidget() {
        StringBuilder sb = new StringBuilder();

        Duration durationFromLastToNow = calculateDurationBetween(lastMoonPhaseMoment, moment);
        Duration durationFromNowToNext = calculateDurationBetween(moment, nextMoonPhaseMoment);

        sb.append("\n");
        sb.append(lastMoonPhase.phaseEmoticon);
        sb.append(" ");
        sb.append(drawDurationDays(durationFromLastToNow));
        sb.append(">");
        sb.append(drawDurationDays(durationFromNowToNext));
        sb.append(" ");
        sb.append(nextMoonPhase.phaseEmoticon);

        this.moonPhaseWidget = sb.toString();
    }

    private void prepareMoonPhaseWidgetSecondary() {
        StringBuilder sb = new StringBuilder();

        Duration durationFromLastToNow = calculateDurationBetween(lastMoonPhaseMoment, moment);
        Duration durationFromNowToNext = calculateDurationBetween(moment, nextMoonPhaseMoment);


        // sb.append("\n");
        // sb.append(durationFromLastToNow);

        //  sb.append("\n");
        //  sb.append(durationFromNowToNext);

        sb.append("\n");
        sb.append(nextMoonPhase.phaseEmoticon + formattedDate(nextMoonPhaseMoment) + " " + nextMoonPhase.time4jName);
        sb.append("\n");
        sb.append("\uD83E\uDC0A"+ " " + formattedDate(moment) + " NOW");
        sb.append("\n");
        sb.append(lastMoonPhase.phaseEmoticon + formattedDate(lastMoonPhaseMoment) + " " + lastMoonPhase.time4jName);


        this.moonPhaseWidgetSecondary = sb.toString();
    }

    /**
     * example:
     * P1DT15H9M (duration.toString boyle bir string cikti veriyor, gun saat dakikayi buradan akmak mumkun
     *            basinda eksi isareti de olur fark negatif cikarsa)
     * P6DT5H4M
     * 🌕 ->------ 🌗
     * @param duration
     * @return
     */
    private String drawDurationDays(Duration duration) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(duration.toString());
        if (matcher.find()) {
            return drawLines(Integer.parseInt(matcher.group()));
        } else {
            return null;
        }
    }

    private String drawLines(int number) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < number; i++) {
            sb.append("-");
        }
        return sb.toString();
    }

    private Duration calculateDurationBetween(Moment moment1, Moment moment2) {
        Timezone tz = Timezone.ofSystem();  // or choose an explicit zone
        PlainTimestamp tsp1 = moment1.toZonalTimestamp(tz.getID());
        PlainTimestamp tsp2 = moment2.toZonalTimestamp(tz.getID());

        TimeMetric<IsoUnit,Duration<IsoUnit>> zonedMetric =
                Duration.in(
                        tz,
                        CalendarUnit.YEARS, CalendarUnit.MONTHS, CalendarUnit.DAYS,
                        ClockUnit.HOURS, ClockUnit.MINUTES);
        Duration<IsoUnit> duration = zonedMetric.between(tsp1, tsp2);
        return duration;
    }


    private void prepareLastMoonPhase(Moment moment) {

        Map<MoonPhase, Moment> phaseMap = new HashMap<>();

        phaseMap.put(MoonPhase.NEW_MOON, MoonPhase.NEW_MOON.before(moment));
        phaseMap.put(MoonPhase.FIRST_QUARTER, MoonPhase.FIRST_QUARTER.before(moment));
        phaseMap.put(MoonPhase.FULL_MOON, MoonPhase.FULL_MOON.before(moment));
        phaseMap.put(MoonPhase.LAST_QUARTER, MoonPhase.LAST_QUARTER.before(moment));

        MoonPhase closestPhase = null;
        Moment closestDate = null;

        for (Map.Entry<MoonPhase, Moment> entry : phaseMap.entrySet()) {
            if (entry.getValue().isBefore(moment)) {
                if (closestDate == null || entry.getValue().isAfter(closestDate)) {
                    closestDate = entry.getValue();
                    closestPhase = entry.getKey();
                }
            }
        }

        this.lastMoonPhase = MoonPhaseEnum.byTime4jName(closestPhase.toString());

        this.lastMoonPhaseMoment = closestDate;

    }

    private void prepareNextMoonPhase(Moment moment) {

        Map<MoonPhase, Moment> phaseMap = new HashMap<>();

        phaseMap.put(MoonPhase.NEW_MOON, MoonPhase.NEW_MOON.atOrAfter(moment));
        phaseMap.put(MoonPhase.FIRST_QUARTER, MoonPhase.FIRST_QUARTER.atOrAfter(moment));
        phaseMap.put(MoonPhase.FULL_MOON, MoonPhase.FULL_MOON.atOrAfter(moment));
        phaseMap.put(MoonPhase.LAST_QUARTER, MoonPhase.LAST_QUARTER.atOrAfter(moment));

        MoonPhase closestPhase = null;
        Moment closestDate = null;

        for (Map.Entry<MoonPhase, Moment> entry : phaseMap.entrySet()) {
            if (entry.getValue().isAfter(moment)) {
                if (closestDate == null || entry.getValue().isBefore(closestDate)) {
                    closestDate = entry.getValue();
                    closestPhase = entry.getKey();
                }
            }
        }

        this.nextMoonPhase = MoonPhaseEnum.byTime4jName(closestPhase.toString());

        this.nextMoonPhaseMoment = closestDate;
    }

    // TODO make timezone and date format separate
    public String formattedDate(Moment moment) {
        // http://time4j.net/tutorial/format.html
        // https://stackoverflow.com/questions/44205662/formatting-time4j-moment kaynagi sayesinde zar zor buldum.
        System.out.println("zonal offset: " + zonalOffset);
        ChronoFormatter<Moment> f =
                ChronoFormatter.ofMomentPattern(
                        "uuuu-MM-dd HH:mm", PatternType.CLDR, Locale.ROOT, ZonalOffset.ofTotalSeconds(zonalOffset));


        return f.format(moment);
    }

    public int getIlluminationPercent() {
        return illuminationPercent;
    }

    public void setIlluminationPercent(int illuminationPercent) {
        this.illuminationPercent = illuminationPercent;
    }


    public String getMoonPhaseWidget() {
        return moonPhaseWidget;
    }

    public void setMoonPhaseWidget(String moonPhaseWidget) {
        this.moonPhaseWidget = moonPhaseWidget;
    }

    public MoonPhaseEnum getLastMoonPhase() {
        return lastMoonPhase;
    }

    public void setLastMoonPhase(MoonPhaseEnum lastMoonPhase) {
        this.lastMoonPhase = lastMoonPhase;
    }

    public MoonPhaseEnum getNextMoonPhase() {
        return nextMoonPhase;
    }

    public void setNextMoonPhase(MoonPhaseEnum nextMoonPhase) {
        this.nextMoonPhase = nextMoonPhase;
    }

    public Moment getLastMoonPhaseMoment() {
        return lastMoonPhaseMoment;
    }

    public void setLastMoonPhaseMoment(Moment lastMoonPhaseMoment) {
        this.lastMoonPhaseMoment = lastMoonPhaseMoment;
    }

    public Moment getNextMoonPhaseMoment() {
        return nextMoonPhaseMoment;
    }

    public void setNextMoonPhaseMoment(Moment nextMoonPhaseMoment) {
        this.nextMoonPhaseMoment = nextMoonPhaseMoment;
    }

    public String getMoonPhaseWidgetSecondary() {
        return moonPhaseWidgetSecondary;
    }

    public void setMoonPhaseWidgetSecondary(String moonPhaseWidgetSecondary) {
        this.moonPhaseWidgetSecondary = moonPhaseWidgetSecondary;
    }

    public int getZonalOffset() {
        return zonalOffset;
    }

    public void setZonalOffset(int zonalOffset) {
        this.zonalOffset = zonalOffset;
    }

    public Moment getMoment() {
        return moment;
    }

    public void setMoment(Moment moment) {
        this.moment = moment;
    }


}

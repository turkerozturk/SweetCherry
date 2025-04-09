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

import org.shredzone.commons.suncalc.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class CommonsSunCalc {

    @Value("${astronomy.timezone}")
    private String timezone;
    @Value("${astronomy.latitude}")
    private String latitudeStr;

    @Value("${astronomy.longitude}")
    private String longitudeStr;

    SolarSystem solarSystem;

    public String getAstronomy() {

        //https://shredzone.org/maven/commons-suncalc/examples.html
        StringBuilder stringBuilder = new StringBuilder();

        LocalDate currentDate = LocalDate.now();
        int year = currentDate.getYear();
        int month = currentDate.getMonthValue();
        int day = currentDate.getDayOfMonth();

        LocalTime currentTime = LocalTime.now();
        int hour = currentTime.getHour();
        int minute = currentTime.getMinute();
        int second = currentTime.getSecond();

        String[] latParts = latitudeStr.split("[^\\d]+");
        String[] longParts = longitudeStr.split("[^\\d]+");

        int latitudeDegrees = Integer.parseInt(latParts[0]);
        int latitudeMinutes = Integer.parseInt(latParts[1]);
        double latitudeSeconds = Double.parseDouble(latParts[2]);

        int longitudeDegrees = Integer.parseInt(longParts[0]);
        int longitudeMinutes = Integer.parseInt(longParts[1]);
        double longitudeSeconds = Double.parseDouble(longParts[2]);

        // SUN

        SunTimes sunTimes = SunTimes.compute()
                .on(year, month, day)    // starting midnight
                .timezone(timezone)
                .latitude(latitudeDegrees, latitudeMinutes, latitudeSeconds)     // Latitude
                .longitude(longitudeDegrees, longitudeMinutes, longitudeSeconds)    // Longitude
                .execute();
        stringBuilder.append("\n");

        stringBuilder.append("Gündoğumu: " + sunTimes.getRise());
        stringBuilder.append("\n");

        stringBuilder.append("Günbatımı:  " + sunTimes.getSet());

        stringBuilder.append("\n");

        // MOON

        MoonTimes moonTimes = MoonTimes.compute()
                .on(year, month, day)    // starting midnight
                .timezone(timezone)
                .latitude(latitudeDegrees, latitudeMinutes, latitudeSeconds)     // Latitude
                .longitude(longitudeDegrees, longitudeMinutes, longitudeSeconds)    // Longitude
                .execute();

        stringBuilder.append("\n");

        stringBuilder.append("Aydoğumu: " + moonTimes.getRise());
        stringBuilder.append("\n");

        stringBuilder.append("Aybatımı:  " + moonTimes.getSet());
        stringBuilder.append("\n");

        String dateTimeFormat = "HH:mm";
        //DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd hh:mm");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);

        String sunRiseFormatted = sunTimes.getRise().format(dateTimeFormatter);
        String sunSetFormatted = sunTimes.getSet().format(dateTimeFormatter);
        String moonRiseFormatted = moonTimes.getRise().format(dateTimeFormatter);
        String moonSetFormatted = moonTimes.getSet().format(dateTimeFormatter);


        solarSystem = new SolarSystem(sunTimes.getRise(),
                sunTimes.getSet(),
                moonTimes.getRise(),
                moonTimes.getSet(),
                sunRiseFormatted,
                sunSetFormatted,
                moonRiseFormatted,
                moonSetFormatted
        );

        // MOON POSITON

        MoonPosition moonPosition = MoonPosition.compute()
                .on(year, month, day, hour, minute, second)    // starting midnight
                .timezone(timezone)
                .latitude(latitudeDegrees, latitudeMinutes, latitudeSeconds)     // Latitude
                .longitude(longitudeDegrees, longitudeMinutes, longitudeSeconds)    // Longitude
                .execute();
        stringBuilder.append("\n");

        stringBuilder.append(String.format(
                "The moon can be seen %.1f° clockwise from the North and "
                        + "%.1f° above the horizon.\nIt is about %.0f km away right now.",
                moonPosition.getAzimuth(),
                moonPosition.getAltitude(),
                moonPosition.getDistance()));
        stringBuilder.append("\n");

        double azimuth = moonPosition.getAzimuth();
        double altitude = moonPosition.getAltitude();
        double trueAltitude = moonPosition.getTrueAltitude();
        double distance = moonPosition.getDistance();
        double angle = moonPosition.getParallacticAngle();

        StringBuilder sbmp = new StringBuilder();

        sbmp.append("\n");
        sbmp.append("azimuth");
        sbmp.append("\t");
        sbmp.append(azimuth);

        sbmp.append("\n");
        sbmp.append("altitude");
        sbmp.append("\t");
        sbmp.append(altitude);

        sbmp.append("\n");
        sbmp.append("trueAltitude");
        sbmp.append("\t");
        sbmp.append(trueAltitude);

        sbmp.append("\n");
        sbmp.append("distance");
        sbmp.append("\t");
        sbmp.append(distance);

        sbmp.append("\n");
        sbmp.append("angle");
        sbmp.append("\t");
        sbmp.append(angle);


        MoonPhase.Phase phase = MoonPhase.Phase.toPhase(moonPosition.getParallacticAngle());
        // Moon phase angle, in degrees. 0 = New Moon, 180 = Full Moon. Angles outside the [0,360) range are normalized into that range.
        String phaseName = phase.toString();


        sbmp.append("\n");
        sbmp.append("phaseName");
        sbmp.append("\t");
        sbmp.append(phaseName);

        //System.out.println(sbmp);






        // MOON PHASE

        MoonPhase moonPhase = MoonPhase.compute()
                .on(year, month, day, hour, minute, second)    // starting midnight
                .timezone(timezone)
                .execute();
      //  System.out.println(moonPhase.getTime());

        //LocalDate nextFullMoon = moonPhase.getTime().toLocalDate();

        //System.out.println( moonPhase.getTime());


        stringBuilder.append("<br />Ay Fazı:  " + moonPhase);
        stringBuilder.append("\n");

      //  stringBuilder.append("<br />Ay Fazı:  " + phaseName);

        stringBuilder.append("\n");

        MoonIllumination.Parameters parameters = MoonIllumination.compute()
                .on(2024, 3, 26, 21, 21, 00);

        for (int i = 1; i <= 31; i++) {
            long percent = Math.round(parameters.execute().getFraction() * 100.0);
            //System.out.println("On March " + i + " the moon was " + percent + "% lit.");
            parameters.plusDays(1);
        }

        double moonage = 29.0 * (angle / 360.0) + 1.0;
        String content = "Moon Age: " + moonage;
        //System.out.println(content);

        double oneMoonCycleAsDays = 29.53059;
        return stringBuilder.toString();
    }


    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getLatitudeStr() {
        return latitudeStr;
    }

    public void setLatitudeStr(String latitudeStr) {
        this.latitudeStr = latitudeStr;
    }

    public String getLongitudeStr() {
        return longitudeStr;
    }

    public void setLongitudeStr(String longitudeStr) {
        this.longitudeStr = longitudeStr;
    }

    public void testMoonPhase() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        MoonPhase.Parameters parameters = MoonPhase.compute()
                .phase(MoonPhase.Phase.FULL_MOON);

        while (true) {
            MoonPhase moonPhase = parameters
                    .on(date)
                    .execute();
            LocalDate nextFullMoon = moonPhase
                    .getTime()
                    .toLocalDate();
            if (nextFullMoon.getYear() == 2025) {
                break;      // we've reached the next year
            }

            System.out.print(nextFullMoon);
            if (moonPhase.isMicroMoon()) {
                System.out.print(" (micromoon)");
            }
            if (moonPhase.isSuperMoon()) {
                System.out.print(" (supermoon)");
            }
            System.out.println();

            date = nextFullMoon.plusDays(1);
        }
    }

    public SolarSystem getSolarSystem() {
        return solarSystem;
    }

    public void setSolarSystem(SolarSystem solarSystem) {
        this.solarSystem = solarSystem;
    }
}

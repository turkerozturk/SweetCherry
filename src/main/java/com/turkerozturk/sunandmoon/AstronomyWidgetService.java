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

import com.turkerozturk.helpers.CommonsSunCalc;
import com.turkerozturk.helpers.SolarSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class AstronomyWidgetService {
    public record Snapshot(SolarSystem sun, MoonTime4j.Snapshot moon, String timezone, String date) {}
    private record Settings(String latitude, String longitude, String timezone) {}
    private static final Logger LOG = LoggerFactory.getLogger(AstronomyWidgetService.class);
    private final AstronomyProperties properties;
    private final CommonsSunCalc sun;
    private final MoonTime4j moon;
    private final Clock clock;
    private Snapshot cached;
    private Settings cachedSettings;
    private Instant expires;

    @Autowired
    public AstronomyWidgetService(AstronomyProperties properties, CommonsSunCalc sun, MoonTime4j moon) {
        this(properties, sun, moon, Clock.systemUTC());
    }

    AstronomyWidgetService(AstronomyProperties properties, CommonsSunCalc sun, MoonTime4j moon, Clock clock) {
        this.properties = properties; this.sun = sun; this.moon = moon; this.clock = clock;
    }

    public boolean isEnabled() { return properties.isEnabled(); }

    /** Refreshes offline calculations at most once a minute; disabled widgets perform no calculations. */
    public synchronized Snapshot snapshot() {
        if (!isEnabled()) { expires = null; cached = null; return null; }
        Instant now = clock.instant();
        Settings settings = new Settings(properties.getLatitude(), properties.getLongitude(), properties.getTimezone());
        if (expires != null && now.isBefore(expires) && !now.isBefore(expires.minusSeconds(60))
                && Objects.equals(settings, cachedSettings)) return cached;
        cachedSettings = settings;
        expires = now.plusSeconds(60);
        try {
            ZoneId zone = ZoneId.of(settings.timezone());
            double latitude = AstronomyCoordinates.parse(settings.latitude(), true);
            double longitude = AstronomyCoordinates.parse(settings.longitude(), false);
            cached = new Snapshot(sun.calculate(now, zone, latitude, longitude), moon.calculate(now, zone),
                    zone.getId(), now.atZone(zone).toLocalDate().toString());
        } catch (RuntimeException exception) {
            cached = null;
            LOG.warn("Astronomy widget unavailable; check astronomy location/timezone settings: {}", exception.getMessage());
        }
        return cached;
    }
}

package com.turkerozturk.sunandmoon;

import com.turkerozturk.helpers.CommonsSunCalc;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class AstronomyCalculationsTest {
    @Test void riseSetUsesConfiguredLocalDayAcrossUtcMidnight() {
        var zone = ZoneId.of("Europe/Istanbul");
        var result = new CommonsSunCalc().calculate(Instant.parse("2026-01-01T21:30:00Z"), zone, 40.0, 29.0);
        assertThat(result.sunRise().withZoneSameInstant(zone).toLocalDate()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(result.sunRiseFormatted()).matches("\\d{2}:\\d{2}");
    }
    @Test void polarSummerHasNoSunriseOrSunsetWithoutThrowing() {
        var result = new CommonsSunCalc().calculate(Instant.parse("2026-06-21T12:00:00Z"), ZoneId.of("Europe/Oslo"), 78.2, 15.6);
        assertThat(result.sunRise()).isNull(); assertThat(result.sunSet()).isNull();
        assertThat(result.sunRiseFormatted()).isEqualTo("—");
        assertThat(result.sunSetFormatted()).isEqualTo("—");
    }
    @Test void moonDatesFollowIanaZoneIncludingSummerOffset() {
        var calculator = new MoonTime4j(); Instant now = Instant.parse("2026-07-01T12:00:00Z");
        var utc = calculator.calculate(now, ZoneOffset.UTC);
        var berlin = calculator.calculate(now, ZoneId.of("Europe/Berlin"));
        var parser = java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");
        assertThat(LocalDateTime.parse(berlin.nextFormatted(), parser))
                .isEqualTo(LocalDateTime.parse(utc.nextFormatted(), parser).plusHours(2));
        assertThat(berlin.illuminationPercent()).isBetween(0, 100);
        assertThat(berlin.moonPhaseWidget()).contains(">");
    }
}

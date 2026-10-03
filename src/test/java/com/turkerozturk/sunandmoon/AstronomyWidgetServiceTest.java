package com.turkerozturk.sunandmoon;

import com.turkerozturk.helpers.CommonsSunCalc;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AstronomyWidgetServiceTest {
    @Test void disabledWidgetDoesNotParseOrCalculate() {
        var properties = new AstronomyProperties(); properties.setEnabled(false);
        var sun = mock(CommonsSunCalc.class); var moon = mock(MoonTime4j.class);
        var service = new AstronomyWidgetService(properties, sun, moon, Clock.systemUTC());
        assertThat(service.snapshot()).isNull();
        verifyNoInteractions(sun, moon);
    }
    @Test void cachesAndRefreshesBothCalculators() {
        var properties = settings(); var sun = mock(CommonsSunCalc.class); var moon = mock(MoonTime4j.class);
        Clock clock = mock(Clock.class);
        Instant now = Instant.parse("2026-01-01T21:30:00Z");
        when(clock.instant()).thenReturn(now, now.plusSeconds(30), now.plusSeconds(61));
        var service = new AstronomyWidgetService(properties, sun, moon, clock);
        var first = service.snapshot();
        assertThat(first.date()).isEqualTo("2026-01-02");
        assertThat(service.snapshot()).isSameAs(first);
        assertThat(service.snapshot()).isNotSameAs(first);
        verify(sun, times(2)).calculate(any(), eq(ZoneId.of("Europe/Istanbul")), eq(40.0), eq(29.0));
        verify(moon, times(2)).calculate(any(), eq(ZoneId.of("Europe/Istanbul")));
    }
    @Test void invalidSettingsDoNotBreakPagesAndChangingThemRetries() {
        var properties = settings(); properties.setTimezone("invalid/zone");
        var sun = mock(CommonsSunCalc.class); var moon = mock(MoonTime4j.class);
        var service = new AstronomyWidgetService(properties, sun, moon, Clock.systemUTC());
        assertThat(service.snapshot()).isNull(); verifyNoInteractions(sun, moon);
        properties.setTimezone("Europe/Istanbul");
        assertThat(service.snapshot()).isNotNull();
        verify(sun).calculate(any(), any(), eq(40.0), eq(29.0));
    }
    @Test void disablingAndReenablingClearsCachedData() {
        var properties = settings(); var sun = mock(CommonsSunCalc.class); var moon = mock(MoonTime4j.class);
        var service = new AstronomyWidgetService(properties, sun, moon, Clock.systemUTC());
        service.snapshot(); properties.setEnabled(false); assertThat(service.snapshot()).isNull();
        properties.setEnabled(true); service.snapshot();
        verify(moon, times(2)).calculate(any(), any());
    }
    private static AstronomyProperties settings() {
        var properties = new AstronomyProperties(); properties.setLatitude("40");
        properties.setLongitude("29"); properties.setTimezone("Europe/Istanbul"); return properties;
    }
}

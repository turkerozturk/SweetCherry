package com.turkerozturk.calendar;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CalendarSettingsTest {
 @Test void weekStartsAcceptIsoNumbersAndEnglishNamesAndRejectZero(){assertThat(CalendarSettings.firstDay("1")).isEqualTo(DayOfWeek.MONDAY);assertThat(CalendarSettings.firstDay("7")).isEqualTo(DayOfWeek.SUNDAY);assertThat(CalendarSettings.firstDay(" sunday ")).isEqualTo(DayOfWeek.SUNDAY);assertThatThrownBy(()->CalendarSettings.firstDay("0")).isInstanceOf(IllegalArgumentException.class);assertThat(new CalendarSettings("bogus","bogus",10000).firstDay()).isEqualTo(DayOfWeek.MONDAY);}
 @Test void datesHandleLeapYearsDstGapsOverlapsAndYearBounds(){var settings=new CalendarSettings("MONDAY","Europe/Berlin",10000);assertThatThrownBy(()->settings.timestamp("2026-03-29T02:30")).hasMessageContaining("400");assertThat(settings.timestamp("2026-10-25T02:30")).isEqualTo(LocalDateTime.of(2026,10,25,2,30).toEpochSecond(ZoneOffset.ofHours(2)));assertThat(settings.timestamp("2024-02-29T12:30")).isPositive();assertThat(settings.timestamp("0001-01-01T00:00")).isNegative();assertThatThrownBy(()->settings.timestamp("2026-02-29T12:30")).hasMessageContaining("400");}
}

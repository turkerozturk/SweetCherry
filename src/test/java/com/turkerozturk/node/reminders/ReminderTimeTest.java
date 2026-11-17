package com.turkerozturk.node.reminders;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class ReminderTimeTest {
 @Test void explicitZonesRoundTripAndPastDatesAreAllowed(){
  assertThat(ReminderTime.resolve("2026-11-17T16:00","Europe/Istanbul")).isEqualTo(Instant.parse("2026-11-17T13:00:00Z"));
  assertThat(ReminderTime.local("2026-11-17T13:00:00Z","Europe/Istanbul")).isEqualTo("2026-11-17T16:00");
  assertThat(ReminderTime.resolve("2000-01-01T00:00","UTC")).isEqualTo(Instant.parse("2000-01-01T00:00:00Z"));
 }
 @Test void gapsOverlapsInvalidDatesAndSecondPrecisionAreRejected(){
  for(String local:new String[]{"2026-03-29T02:30","2026-10-25T02:30","2026-02-30T12:00","2026-01-01T10:00:15"})
   assertThatThrownBy(()->ReminderTime.resolve(local,"Europe/Berlin")).isInstanceOf(IllegalArgumentException.class);
  assertThat(ReminderTime.resolve("2026-10-25T02:30","+02:00")).isEqualTo(Instant.parse("2026-10-25T00:30:00Z"));
  assertThatThrownBy(()->ReminderTime.resolve("2026-11-17T16:00","bad/zone")).isInstanceOf(IllegalArgumentException.class);
 }
}

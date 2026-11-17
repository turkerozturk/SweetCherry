package com.turkerozturk.node.reminders;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class AnnualReminderTest {
 @Test void february29KeepsItsAnchorAndSupportsBothNonLeapPolicies(){
  String anchor="2024-02-29T10:00";
  assertThat(AnnualReminder.occurrence(anchor,"UTC","feb28",2025)).isEqualTo(Instant.parse("2025-02-28T10:00:00Z"));
  assertThat(AnnualReminder.occurrence(anchor,"UTC","mar1",2025)).isEqualTo(Instant.parse("2025-03-01T10:00:00Z"));
  assertThat(AnnualReminder.occurrence(anchor,"UTC","feb28",2028)).isEqualTo(Instant.parse("2028-02-29T10:00:00Z"));
 }
 @Test void currentWaitsForAnniversaryAndNextAlwaysSelectsAFutureOccurrence(){
  var stored=Instant.parse("2024-02-29T10:00:00Z");
  assertThat(AnnualReminder.current("2024-02-29T10:00","UTC","feb28",stored,Instant.parse("2025-02-28T09:59:00Z"))).isEqualTo(stored);
  assertThat(AnnualReminder.current("2024-02-29T10:00","UTC","feb28",stored,Instant.parse("2025-02-28T10:00:00Z"))).isEqualTo(Instant.parse("2025-02-28T10:00:00Z"));
  assertThat(AnnualReminder.next("2024-02-29T10:00","UTC","feb28",Instant.parse("2026-11-17T10:00:00Z"))).isEqualTo(Instant.parse("2027-02-28T10:00:00Z"));
 }
 @Test void recurrenceUsesDocumentedDstResolutionAndStopsAtDateLimit(){
  assertThat(AnnualReminder.occurrence("2023-03-31T02:30","Europe/Berlin","feb28",2024).atZone(ZoneId.of("Europe/Berlin")).getHour()).isEqualTo(3);
  assertThat(AnnualReminder.occurrence("2023-10-27T02:30","Europe/Berlin","feb28",2024)).isEqualTo(Instant.parse("2024-10-27T00:30:00Z"));
  assertThatThrownBy(()->AnnualReminder.next("2024-12-31T23:59","UTC","feb28",Instant.parse("9999-12-31T23:59:00Z"))).isInstanceOf(IllegalArgumentException.class);
 }
}

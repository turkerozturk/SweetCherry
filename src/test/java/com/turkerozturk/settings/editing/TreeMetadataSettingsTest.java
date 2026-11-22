package com.turkerozturk.settings.editing;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class TreeMetadataSettingsTest {
 @Test void disabledDefaultAndPaddingDoNotChangeIdentity(){
  var settings=new TreeMetadataSettings(false,"6","yyyy-MM-dd","created");
  assertThat(settings.enabled()).isFalse();assertThat(settings.modified()).isFalse();
  assertThat(settings.id(42)).isEqualTo("[000042]");assertThat(settings.id(12345678)).isEqualTo("[12345678]");
  assertThat(new TreeMetadataSettings(true,"3","yyyy-MM-dd","created").id(7)).isEqualTo("[007]");
 }
 @Test void formatsSelectedTimestampInServerTimeZoneAndHandlesMissingDates(){
  long created=ZonedDateTime.of(2026,11,20,12,0,0,0,ZoneId.systemDefault()).toEpochSecond();
  long modified=ZonedDateTime.of(2026,11,21,12,0,0,0,ZoneId.systemDefault()).toEpochSecond();
  assertThat(new TreeMetadataSettings(true,"6","yyyy-MM-dd","created").date(created,modified)).isEqualTo("2026-11-20");
  assertThat(new TreeMetadataSettings(true,"6","dd/MM/yyyy HH:mm","modified").date(created,modified)).isEqualTo("21/11/2026 12:00");
  assertThat(new TreeMetadataSettings(false,"6","yyyy-MM-dd","created").date(0,modified)).isEmpty();
 }
 @Test void unsafeYamlValuesFallBackAndSettingsRejectInvalidFormatsAndSources(){
  var safe=new TreeMetadataSettings(false,"999999","invalid[","other");assertThat(safe.id(1)).isEqualTo("[000001]");
  assertThat(safe.modified()).isFalse();assertThat(safe.date(1,0)).isNotEmpty();
  for(var bad:Map.of("myapp.tree.metadata.id-digits","20","myapp.tree.metadata.date-source","other","myapp.tree.metadata.date-format","invalid[").entrySet()){
   var values=new LinkedHashMap<String,String>();ApplicationSettingsService.SETTINGS.forEach(s->values.put(s.key(),s.fallback()));
   values.put(bad.getKey(),bad.getValue());assertThatThrownBy(()->ApplicationSettingsService.update("myapp: {}\n",values)).isInstanceOf(RuntimeException.class);
  }
 }
}

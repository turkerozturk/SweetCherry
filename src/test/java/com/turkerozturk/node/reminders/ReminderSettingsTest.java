package com.turkerozturk.node.reminders;

import com.turkerozturk.calendar.CalendarSettings;
import com.turkerozturk.node.properties.NodePropertiesService;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReminderSettingsTest {
 @Test void parentSwitchesOverrideChildren(){
  var off=new ReminderSettings(false,true,true);
  assertThat(off.enabled()).isFalse();assertThat(off.alarms()).isFalse();assertThat(off.sound()).isFalse();
  var silent=new ReminderSettings(true,true,false);assertThat(silent.alarms()).isTrue();assertThat(silent.sound()).isFalse();
  var noAlarms=new ReminderSettings(true,false,true);assertThat(noAlarms.enabled()).isTrue();assertThat(noAlarms.alarms()).isFalse();assertThat(noAlarms.sound()).isFalse();
  assertThat(new ReminderSettings(true,true,true).sound()).isTrue();
 }
 @Test void disabledRemindersRejectReadsAndWritesBeforeAccessingTheDatabase(){
  var properties=mock(NodePropertiesService.class);
  var service=new ReminderService(properties,new CalendarSettings("MONDAY","UTC",1000),Clock.systemUTC(),new ReminderSettings(false,true,true));
  denied(()->service.inspect(new ReminderService.Options("","pending",50,1)));
  denied(()->service.editor("",1));denied(()->service.save("",1,1,"","UTC","","revision"));
  denied(()->service.changeState("id","done","revision"));denied(service::alarms);denied(()->service.alarmAction("id","key","dismiss"));
  verifyNoInteractions(properties);
 }
 @Test void disabledAlarmsRejectBothAlarmEndpointsBeforeDatabaseAccess(){
  var properties=mock(NodePropertiesService.class);
  var service=new ReminderService(properties,new CalendarSettings("MONDAY","UTC",1000),Clock.systemUTC(),new ReminderSettings(true,false,true));
  denied(service::alarms);denied(()->service.alarmAction("id","key","snooze"));verifyNoInteractions(properties);
 }
 private void denied(org.assertj.core.api.ThrowableAssert.ThrowingCallable action){
  assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,error->{assertThat(error.getStatusCode().value()).isEqualTo(404);assertThat(error.getReason()).isEqualTo("disabled");});
 }
}

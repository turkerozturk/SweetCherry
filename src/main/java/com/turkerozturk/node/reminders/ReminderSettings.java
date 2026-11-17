package com.turkerozturk.node.reminders;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Parent feature gates apply to both UI and backend; stored records are never removed by a toggle. */
@ControllerAdvice
public class ReminderSettings {
    private final boolean reminders,alarms,sound;
    public ReminderSettings(@Value("${myapp.reminders.enabled:true}")boolean reminders,
            @Value("${myapp.reminders.alarms-enabled:true}")boolean alarms,
            @Value("${myapp.reminders.sound-enabled:true}")boolean sound){this.reminders=reminders;this.alarms=alarms;this.sound=sound;}
    @ModelAttribute("remindersEnabled") public boolean enabled(){return reminders;}
    @ModelAttribute("reminderAlarmsEnabled") public boolean alarms(){return reminders&&alarms;}
    @ModelAttribute("reminderSoundEnabled") public boolean sound(){return alarms()&&sound;}
    public void requireReminders(){if(!enabled())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"disabled");}
    public void requireAlarms(){if(!alarms())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"disabled");}
}

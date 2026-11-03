package com.turkerozturk.customlists.views;

import java.time.*;
import java.time.format.*;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="myapp.custom-lists.views")
public class RecordViewSettings {
    public static final String DEFAULT_FORMAT="yyyy-MM-dd HH:mm:ss";
    private String dateTimeFormat=DEFAULT_FORMAT;
    private String timeZone="";
    private int maxDesignCharacters=131072,maxRenderedCharacters=16777216;
    public String getDateTimeFormat(){return dateTimeFormat;}
    public void setDateTimeFormat(String value){
        if(value==null || value.isBlank() || value.length()>128)throw new IllegalArgumentException("Invalid record-view date format");
        DateTimeFormatter.ofPattern(value,Locale.ROOT);dateTimeFormat=value;
    }
    public String getTimeZone(){return timeZone;}
    public void setTimeZone(String value){if(value!=null&&!value.isBlank())ZoneId.of(value);timeZone=value==null?"":value;}
    public int getMaxDesignCharacters(){return maxDesignCharacters;}
    public void setMaxDesignCharacters(int value){if(value<1||value>1048576)throw new IllegalArgumentException("Invalid design limit");maxDesignCharacters=value;}
    public int getMaxRenderedCharacters(){return maxRenderedCharacters;}
    public void setMaxRenderedCharacters(int value){if(value<1||value>67108864)throw new IllegalArgumentException("Invalid output limit");maxRenderedCharacters=value;}
    public String now(Instant instant,Locale locale){return DateTimeFormatter.ofPattern(dateTimeFormat,locale).withZone(timeZone.isBlank()?ZoneId.systemDefault():ZoneId.of(timeZone)).format(instant);}
}

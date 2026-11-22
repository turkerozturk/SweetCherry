package com.turkerozturk.settings.editing;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/** Display metadata only; IDs and timestamps in the CTB are never rewritten. */
@ControllerAdvice
public class TreeMetadataSettings {
    private final boolean enabled;
    private final int digits;
    private final boolean modified;
    private final DateTimeFormatter formatter;
    public TreeMetadataSettings(@Value("${myapp.tree.metadata.enabled:false}") boolean enabled,
            @Value("${myapp.tree.metadata.id-digits:6}") String digits,
            @Value("${myapp.tree.metadata.date-format:yyyy-MM-dd}") String pattern,
            @Value("${myapp.tree.metadata.date-source:created}") String source) {
        this.enabled=enabled;
        this.digits=TreeAppearanceAdvice.bounded(digits,6,1,19);
        this.modified="modified".equalsIgnoreCase(source);
        DateTimeFormatter format;
        try { format=dateFormatter(pattern); }
        catch (RuntimeException invalid) { format=dateFormatter("yyyy-MM-dd"); }
        this.formatter=format.withZone(ZoneId.systemDefault());
    }
    static DateTimeFormatter dateFormatter(String pattern) {
        if(pattern==null||pattern.isBlank()||pattern.length()>128||pattern.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Invalid date format");
        var format=DateTimeFormatter.ofPattern(pattern,Locale.ROOT);
        format.format(Instant.EPOCH.atZone(ZoneId.systemDefault()));
        return format;
    }
    @ModelAttribute("treeMetadataEnabled") public boolean enabled(){return enabled;}
    @ModelAttribute("treeMetadataModified") public boolean modified(){return modified;}
    public String id(long id){return "["+String.format(Locale.ROOT,"%0"+digits+"d",id)+"]";}
    public String date(long created,long modified){
        long timestamp=this.modified?modified:created;
        if(timestamp<=0)return "";
        try{return formatter.format(Instant.ofEpochSecond(timestamp));}
        catch(DateTimeException invalid){return "";}
    }
}

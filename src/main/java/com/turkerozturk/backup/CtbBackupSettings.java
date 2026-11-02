package com.turkerozturk.backup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="myapp.ctb-backup")
public class CtbBackupSettings {
    public static class Automatic {
        private boolean enabled;
        private int intervalMinutes=1440, retryMinutes=5, keepLast;
        private java.util.List<String> tenants=new java.util.ArrayList<>();
        public boolean isEnabled(){return enabled;} public void setEnabled(boolean value){enabled=value;}
        public int getIntervalMinutes(){return intervalMinutes;} public void setIntervalMinutes(int value){intervalMinutes=positive(value);}
        public int getRetryMinutes(){return retryMinutes;} public void setRetryMinutes(int value){retryMinutes=positive(value);}
        public int getKeepLast(){return keepLast;} public void setKeepLast(int value){if(value<0)throw new IllegalArgumentException("keep-last must be nonnegative");keepLast=value;}
        public java.util.List<String> getTenants(){return java.util.List.copyOf(tenants);}
        public void setTenants(java.util.List<String> value){if(value!=null && value.stream().anyMatch(name->name==null || name.isBlank()))throw new IllegalArgumentException("Backup tenant names must not be blank");tenants=value==null?new java.util.ArrayList<>():new java.util.ArrayList<>(value);}
        private static int positive(int value){if(value<1)throw new IllegalArgumentException("Backup interval must be positive");return value;}
    }
    private final Automatic automatic=new Automatic();
    public Automatic getAutomatic(){return automatic;}
    private boolean enabled = true;
    private String directory = "backups/ctb";
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public String getDirectory() { return directory; }
    public void setDirectory(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Backup directory must not be blank");
        directory = value;
    }
}

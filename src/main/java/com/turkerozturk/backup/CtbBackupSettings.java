package com.turkerozturk.backup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="myapp.ctb-backup")
public class CtbBackupSettings {
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

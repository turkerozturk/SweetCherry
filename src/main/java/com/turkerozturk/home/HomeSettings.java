package com.turkerozturk.home;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "myapp.home")
public class HomeSettings {
    private boolean openLatestModified;
    private int modifiedMinimum = 5;
    private int createdMinimum = 5;

    public boolean isOpenLatestModified() { return openLatestModified; }
    public void setOpenLatestModified(boolean value) { openLatestModified = value; }
    public int getModifiedMinimum() { return modifiedMinimum; }
    public void setModifiedMinimum(int value) { modifiedMinimum = nonnegative(value); }
    public int getCreatedMinimum() { return createdMinimum; }
    public void setCreatedMinimum(int value) { createdMinimum = nonnegative(value); }
    private static int nonnegative(int value) {
        if (value < 0) throw new IllegalArgumentException("Home activity minimum must be nonnegative");
        return value;
    }
}

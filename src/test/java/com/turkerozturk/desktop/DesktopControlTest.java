package com.turkerozturk.desktop;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DesktopControlTest {
    @Test
    void commandLineCanDisableExeDesktopDefaultForServerTests() {
        String old = System.getProperty("myapp.desktop.enabled");
        try {
            System.setProperty("myapp.desktop.enabled", "true");
            assertThat(DesktopControl.requested(new String[0])).isTrue();
            assertThat(DesktopControl.requested(new String[]{"--myapp.desktop.enabled=false"})).isFalse();
        } finally {
            if (old == null) System.clearProperty("myapp.desktop.enabled");
            else System.setProperty("myapp.desktop.enabled", old);
        }
    }

    @Test
    void scriptsRemainHeadlessUnlessExplicitlyEnabled() {
        String old = System.getProperty("myapp.desktop.enabled");
        try {
            System.clearProperty("myapp.desktop.enabled");
            assertThat(DesktopControl.requested(new String[0])).isFalse();
            assertThat(DesktopControl.requested(new String[]{"--myapp.desktop.enabled=true"})).isTrue();
        } finally {
            if (old != null) System.setProperty("myapp.desktop.enabled", old);
        }
    }
}

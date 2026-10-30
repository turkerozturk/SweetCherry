package com.turkerozturk.login;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class FirstRunLoginTest {
    @TempDir Path directory;

    @Test void initialCredentialAnnouncementHappensOnlyOnCreation() throws Exception {
        Path file = directory.resolve("login-credentials.properties");
        var first = LocalLoginCredentials.loadWithStatus(file);
        var second = LocalLoginCredentials.loadWithStatus(file);
        assertThat(first.created()).isTrue(); assertThat(second.created()).isFalse();
        var bytes = new ByteArrayOutputStream(); var out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        InitialLoginHint.print(out, first.created(), true, false, "admin", first.credentials().getProperty("admin.password"));
        String once = bytes.toString(StandardCharsets.UTF_8);
        assertThat(once).contains(first.credentials().getProperty("admin.password"), "admin", "login-credentials.properties");
        InitialLoginHint.print(out, second.created(), true, false, "admin", "must-not-print");
        assertThat(bytes.toString(StandardCharsets.UTF_8)).isEqualTo(once);
        assertThat(first.toString()).doesNotContain(first.credentials().getProperty("admin.password"));
    }

    @Test void disabledHintsAndExternalPasswordsAreNeverPrinted() {
        var bytes = new ByteArrayOutputStream(); var out = new PrintStream(bytes);
        InitialLoginHint.print(out, true, false, false, "admin", "SECRET");
        InitialLoginHint.print(out, true, true, true, "admin", "SECRET");
        InitialLoginHint.print(out, false, true, false, "admin", "SECRET");
        assertThat(bytes.toString()).isEmpty();
    }

    @Test void manuallyChangedPasswordsRemainUntouchedAndAreNotAnnounced() throws Exception {
        Path file = directory.resolve("login-credentials.properties");
        String custom = "admin.password=my-new-password\nuser.password=another-password\n";
        Files.writeString(file, custom);
        var loaded = LocalLoginCredentials.loadWithStatus(file);
        assertThat(loaded.created()).isFalse();
        assertThat(loaded.credentials().getProperty("admin.password")).isEqualTo("my-new-password");
        assertThat(Files.readString(file)).isEqualTo(custom);
    }

    @Test void loginPageModelContainsUsernameAndNoPassword() {
        var model = new org.springframework.ui.ExtendedModelMap();
        assertThat(new LoginController().login(null, model)).isEqualTo("login/login");
        assertThat(model).containsEntry("loginAdminName", "admin").containsEntry("expired", false);
        assertThat(model.keySet()).containsExactlyInAnyOrder("loginAdminName", "expired");
    }
}

package com.turkerozturk.login;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class LocalLoginCredentialsTest {
    @TempDir Path directory;

    @Test
    void generatesDistinctPasswordsAndReusesThemOnRestart() throws Exception {
        Path file = directory.resolve("login-credentials.properties");
        Properties first = LocalLoginCredentials.load(file);
        assertTrue(first.getProperty("admin.password").length() >= 24);
        assertNotEquals(first.getProperty("admin.password"), first.getProperty("user.password"));
        Properties second = LocalLoginCredentials.load(file);
        assertEquals(first, second);
    }
}

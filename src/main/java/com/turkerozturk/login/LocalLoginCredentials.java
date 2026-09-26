package com.turkerozturk.login;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Properties;

/** Persistent first-run passwords for the local distribution. */
final class LocalLoginCredentials {
    private static final String ADMIN = "admin.password";
    private static final String USER = "user.password";

    static Properties load(Path file) throws IOException {
        if (Files.exists(file)) {
            Properties credentials = new Properties();
            try (InputStream input = Files.newInputStream(file)) {
                credentials.load(input);
            }
            if (credentials.getProperty(ADMIN, "").isBlank()
                    || credentials.getProperty(USER, "").isBlank()) {
                throw new IOException("Incomplete login credentials file: " + file);
            }
            return credentials;
        }

        SecureRandom random = new SecureRandom();
        Properties credentials = new Properties();
        credentials.setProperty(ADMIN, password(random));
        credentials.setProperty(USER, password(random));
        try {
            Files.createFile(file);
            try {
                Files.setPosixFilePermissions(file, EnumSet.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            } catch (UnsupportedOperationException ignored) {
                // Windows uses filesystem ACLs instead of POSIX permissions.
            }
            try (OutputStream output = Files.newOutputStream(file)) {
                credentials.store(output, "SweetCherry local login credentials; keep this file private");
            }
        } catch (IOException exception) {
            throw new IOException("Cannot create login credentials file: " + file, exception);
        }
        return credentials;
    }

    private static String password(SecureRandom random) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private LocalLoginCredentials() { }
}

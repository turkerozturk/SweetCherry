package com.turkerozturk.startup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

/** Runs before Spring reads configuration. Existing root/config YAML always wins. */
public final class ExternalConfiguration {
    private static final List<String> NAMES = List.of("application", "application-operations", "application-experiments");

    public static List<String> initialize(Path directory, ClassLoader loader) {
        List<String> warnings = new ArrayList<>();
        for (String name : NAMES) {
            boolean exists = false;
            for (Path parent : List.of(directory, directory.resolve("config")))
                for (String extension : List.of(".yml", ".yaml"))
                    exists |= Files.exists(parent.resolve(name + extension), LinkOption.NOFOLLOW_LINKS);
            if (exists) continue;
            Path target = directory.resolve(name + ".yml");
            boolean created = false;
            try (InputStream input = loader.getResourceAsStream(name + ".yml")) {
                if (input == null) throw new IOException("Bundled configuration is missing");
                try {
                    if (Files.getFileStore(directory).supportsFileAttributeView("posix"))
                        Files.createFile(target, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                    else Files.createFile(target);
                    created = true;
                } catch (FileAlreadyExistsException otherStartup) { continue; }
                Files.write(target, input.readAllBytes());
            } catch (IOException error) {
                if (created) try { Files.delete(target); } catch (IOException ignored) { }
                warnings.add("Cannot create " + target.toAbsolutePath() + "; bundled defaults remain available. Use a writable application folder.");
            }
        }
        return List.copyOf(warnings);
    }

    private ExternalConfiguration() { }
}

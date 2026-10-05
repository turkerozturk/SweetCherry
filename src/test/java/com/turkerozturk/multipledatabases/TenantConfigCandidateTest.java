package com.turkerozturk.multipledatabases;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class TenantConfigCandidateTest {
    @TempDir Path directory;
    @Test void ignoresSubfoldersAndSqlitePayloads() throws Exception {
        Path folder=Files.createDirectory(directory.resolve("other-configs"));
        Path database=Files.write(directory.resolve("demo.ctb"),"SQLite format 3\0binary".getBytes(StandardCharsets.US_ASCII));
        assertThat(MultitenantConfiguration.isTenantConfigCandidate(folder)).isFalse();
        assertThat(MultitenantConfiguration.isTenantConfigCandidate(database)).isFalse();
    }
    @Test void acceptsTextConfigsRegardlessOfFilenameOrExtension() throws Exception {
        for(String filename:java.util.List.of("demo.txt","other.properties","connection-without-extension")) {
            Path file=Files.writeString(directory.resolve(filename),"name=Demo\ndatasource.url=jdbc:sqlite:demo.ctb");
            assertThat(MultitenantConfiguration.isTenantConfigCandidate(file)).isTrue();
        }
    }
}

package com.turkerozturk.startup;

import com.turkerozturk.settings.editing.ApplicationSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class ExternalConfigurationTest {
    @TempDir Path directory;
    private final ClassLoader loader = getClass().getClassLoader();

    @Test void createsDefaultsBeforeConfigurationLoadingAndMakesWebSettingsEditable() throws Exception {
        assertThat(ExternalConfiguration.initialize(directory, loader)).isEmpty();
        for (String name : new String[]{"application", "application-operations", "application-experiments"})
            assertThat(directory.resolve(name + ".yml")).hasBinaryContent(loader.getResourceAsStream(name + ".yml").readAllBytes());
        var environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load("external", new FileSystemResource(directory.resolve("application.yml")))
                .forEach(source -> environment.getPropertySources().addLast(source));
        assertThat(environment.getProperty("server.port")).isEqualTo("8443");
        assertThat(environment.getProperty("astronomy.latitude")).isEqualTo("51.4779");
        assertThat(environment.getProperty("astronomy.longitude")).isEqualTo("-0.0015");
        assertThat(environment.getProperty("astronomy.timezone")).isEqualTo("Europe/London");
        var settings = new ApplicationSettingsService(environment);
        var snapshot = settings.read(); var values = new java.util.LinkedHashMap<>(snapshot.values());
        values.put("myapp.debug", "true"); settings.save(snapshot.revision(), values);
        assertThat(Files.readString(directory.resolve("application.yml"))).contains("debug: true");
    }

    @Test void neverOverwritesExistingSettingsOrAnAlternativeYaml() throws Exception {
        Files.writeString(directory.resolve("application.yaml"), "myapp: {debug: true}\n");
        Files.writeString(directory.resolve("application-operations.yml"), "# user-owned\n");
        assertThat(ExternalConfiguration.initialize(directory, loader)).isEmpty();
        assertThat(directory.resolve("application.yml")).doesNotExist();
        assertThat(Files.readString(directory.resolve("application-operations.yml"))).isEqualTo("# user-owned\n");
        assertThat(ExternalConfiguration.initialize(directory, loader)).isEmpty();
        assertThat(directory.resolve("application.yml")).doesNotExist();
    }

    @Test void configSubdirectoryPreventsShadowDefaults() throws Exception {
        Path config = Files.createDirectory(directory.resolve("config"));
        Files.writeString(config.resolve("application.yaml"), "myapp: {debug: true}\n");
        Files.writeString(config.resolve("application-experiments.yml"), "myapp: {scheduling: {enabled: false}}\n");
        ExternalConfiguration.initialize(directory, loader);
        assertThat(directory.resolve("application.yml")).doesNotExist();
        assertThat(directory.resolve("application-experiments.yml")).doesNotExist();
    }

    @Test void reportsUnwritableDestinationWithoutPreventingClasspathStartup() throws Exception {
        Path file = Files.writeString(directory.resolve("not-a-folder"), "keep");
        assertThat(ExternalConfiguration.initialize(file, loader)).hasSize(3);
        assertThat(Files.readString(file)).isEqualTo("keep");
    }
}

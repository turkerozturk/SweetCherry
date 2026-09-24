package com.turkerozturk.export;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteSchemaLoaderTest {

    @Test
    void loadsTheCanonicalSchemaFromTheClasspath() throws IOException {
        String schema = SqliteSchemaLoader.load();

        assertThat(schema)
                .isNotBlank()
                .containsIgnoringCase("CREATE TABLE IF NOT EXISTS node")
                .containsIgnoringCase("CREATE TABLE IF NOT EXISTS children");
    }
}

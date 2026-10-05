package com.turkerozturk.settings;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LogLevel;

@Configuration
public class PoolLoggingConfiguration {
    /** Hides pool start/stop INFO chatter in regular use without hiding WARN/ERROR diagnostics. */
    public PoolLoggingConfiguration(Environment environment) {
        if (environment.containsProperty("logging.level.com.zaxxer.hikari")) return;
        boolean debug = environment.getProperty("myapp.debug", Boolean.class, false);
        LoggingSystem.get(getClass().getClassLoader()).setLogLevel("com.zaxxer.hikari", debug ? LogLevel.INFO : LogLevel.WARN);
    }
}

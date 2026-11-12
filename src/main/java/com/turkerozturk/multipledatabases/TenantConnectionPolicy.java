package com.turkerozturk.multipledatabases;

import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import org.springframework.boot.jdbc.DataSourceBuilder;

/** ADMIN-configured database endpoints may include localhost and private networks. */
final class TenantConnectionPolicy {
    private static final Map<String,String> PREFIXES = Map.of(
            "org.sqlite.JDBC", "jdbc:sqlite:",
            "org.postgresql.Driver", "jdbc:postgresql:",
            "org.mariadb.jdbc.Driver", "jdbc:mariadb:",
            "com.mysql.cj.jdbc.Driver", "jdbc:mysql:");

    private TenantConnectionPolicy() { }

    static void validate(String driver, String url) {
        if (driver == null || !PREFIXES.containsKey(driver))
            throw new IllegalArgumentException("wizard.unsupportedDriver");
        String prefix = PREFIXES.get(driver);
        if (url == null || !url.startsWith(prefix) || url.length() <= prefix.length()
                || url.length() > 4096 || url.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("wizard.invalidUrl");
        // Xerial's :resource: form can download remote resources; a tenant must use a CTB file.
        if (driver.equals("org.sqlite.JDBC")
                && url.substring(prefix.length()).toLowerCase(java.util.Locale.ROOT).startsWith(":resource:"))
            throw new IllegalArgumentException("wizard.invalidUrl");
    }

    static HikariDataSource pool(String driver, String url, String username, String password) {
        validate(driver, url);
        return (HikariDataSource) DataSourceBuilder.create().driverClassName(driver).url(url)
                .username(username).password(password).build();
    }
}

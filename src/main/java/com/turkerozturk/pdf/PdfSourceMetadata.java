package com.turkerozturk.pdf;

import org.springframework.stereotype.Component;
import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.multipledatabases.ManagedTenantDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

/** Source information is read afresh at export time, never stored in session preferences. */
@Component
public class PdfSourceMetadata {
    private final DataSource dataSource;
    public PdfSourceMetadata(DataSource dataSource) { this.dataSource = dataSource; }
    public record Source(String name, String address) {}
    public Source current(PdfExportOptions options) {
        String name = TenantContext.getCurrentTenant();
        String address = null;
        if (options.metadata() && options.sourceAddress() && dataSource instanceof AbstractRoutingDataSource routing) {
            var selected = routing.getResolvedDataSources().get(name);
            if (selected instanceof ManagedTenantDataSource managed) selected = managed.configuration();
            if (selected instanceof HikariDataSource hikari) address = safeAddress(hikari.getJdbcUrl());
        }
        return new Source(options.metadata() && options.sourceName() ? name : null, address);
    }
    /** Allow listed JDBC forms only. Query, fragment, user-info and filesystem directories are omitted. */
    static String safeAddress(String jdbc) {
        if (jdbc == null) return null;
        try {
            if (jdbc.startsWith("jdbc:sqlite:")) {
                String file = jdbc.substring(12).split("[?#]", 2)[0].replace('\\', '/');
                if (file.startsWith("file:")) file = file.substring(5);
                String basename = file.substring(file.lastIndexOf('/') + 1);
                return basename.matches("[\\p{L}\\p{N} ._-]{1,160}") ? "jdbc:sqlite:" + basename : "jdbc:sqlite";
            }
            for (String type : new String[]{"mysql", "mariadb", "postgresql"}) {
                String prefix = "jdbc:" + type + ":";
                if (!jdbc.startsWith(prefix)) continue;
                var uri = java.net.URI.create(jdbc.substring(5));
                if (uri.getHost() == null) return null;
                // Database names can be arbitrary; omit path as well as credentials and parameters.
                return "jdbc:" + type + "://" + uri.getHost() + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
            }
        } catch (IllegalArgumentException invalid) { return null; }
        return null;
    }
}

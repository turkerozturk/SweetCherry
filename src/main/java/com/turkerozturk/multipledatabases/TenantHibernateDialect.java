package com.turkerozturk.multipledatabases;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.community.dialect.SQLiteDialect;

/** Dialect metadata belongs to a connection generation, not the application-wide YAML. */
public record TenantHibernateDialect(Dialect dialect) {
    private static final Map<String,String> OVERRIDES=Map.of(
        "sqlite", "org.hibernate.community.dialect.SQLiteDialect",
        "mysql", "org.hibernate.dialect.MySQLDialect",
        "mariadb", "org.hibernate.dialect.MariaDBDialect",
        "postgresql", "org.hibernate.dialect.PostgreSQLDialect");
    public static TenantHibernateDialect detect(Connection connection) throws SQLException {
        var metadata=connection.getMetaData();
        Dialect dialect;
        if ("SQLite".equalsIgnoreCase(metadata.getDatabaseProductName())) {
            dialect=new SQLiteDialect(DatabaseVersion.make(metadata.getDatabaseMajorVersion(),metadata.getDatabaseMinorVersion()));
        } else {
            dialect=new StandardDialectResolver().resolveDialect(new DatabaseMetaDataDialectResolutionInfoAdapter(metadata));
        }
        if(dialect==null) throw new SQLException("Unsupported database dialect; check the tenant JDBC driver and server version.");
        return new TenantHibernateDialect(dialect);
    }
    public String key() {
        String key=dialect.getClass().getName()+":"+dialect.getVersion();
        if(dialect instanceof org.hibernate.dialect.MySQLDialect mysql)
            key+=":"+mysql.getMaxVarcharLength()+":"+mysql.isNoBackslashEscapesEnabled();
        return key;
    }
    public Object setting(String override) {
        if(override==null || override.isBlank() || override.trim().equalsIgnoreCase("auto")) return dialect;
        String value=override.trim();
        String selected=OVERRIDES.get(value.toLowerCase(java.util.Locale.ROOT));
        if(selected==null && OVERRIDES.containsValue(value)) selected=value;
        if(selected==null || !selected.equals(dialect.getClass().getName()))
            throw new IllegalArgumentException("Tenant dialect does not match the connected database.");
        // Retain the server version detected from JDBC even when an explicit matching name was given.
        return dialect;
    }
}

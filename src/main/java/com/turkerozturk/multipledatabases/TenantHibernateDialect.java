package com.turkerozturk.multipledatabases;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Pattern;
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
        String product=metadata.getDatabaseProductName();
        String version=metadata.getDatabaseProductVersion();
        // Connector/J can report MariaDB as MySQL, including the 5.5.5 compatibility prefix.
        boolean maria="MariaDB".equalsIgnoreCase(product) || containsMariaDb(version);
        if(!maria && "MySQL".equalsIgnoreCase(product)) {
            try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT VERSION()")) {
                if(rows.next() && containsMariaDb(rows.getString(1))) {
                    maria=true;version=rows.getString(1);
                }
            }
        }
        final boolean mariaServer=maria;
        final int[] mariaVersion=maria ? mariaVersion(version) : null;
        final String serverVersion=maria ? mariaVersion[0]+"."+mariaVersion[1]+"."+mariaVersion[2] : version;
        final boolean incompatibleKeywords=maria && metadata.getDriverName().toLowerCase(Locale.ROOT).contains("mysql");
        Dialect dialect;
        if ("SQLite".equalsIgnoreCase(product)) {
            dialect=new SQLiteDialect(DatabaseVersion.make(metadata.getDatabaseMajorVersion(),metadata.getDatabaseMinorVersion()));
        } else {
            var info=new DatabaseMetaDataDialectResolutionInfoAdapter(metadata) {
                @Override public String getDatabaseName() { return mariaServer ? "MariaDB" : super.getDatabaseName(); }
                @Override public String getDatabaseVersion() { return serverVersion; }
                @Override public int getDatabaseMajorVersion() { return mariaVersion==null ? super.getDatabaseMajorVersion() : mariaVersion[0]; }
                @Override public int getDatabaseMinorVersion() { return mariaVersion==null ? super.getDatabaseMinorVersion() : mariaVersion[1]; }
                @Override public String getSQLKeywords() {
                    // MySQL Connector/J queries KEYWORDS.RESERVED, absent on MariaDB.
                    // Keep Hibernate's built-in keywords; do not suppress unrelated metadata failures.
                    return incompatibleKeywords ? "" : super.getSQLKeywords();
                }
            };
            dialect=new StandardDialectResolver().resolveDialect(info);
        }
        if(dialect==null) throw new SQLException("Unsupported database dialect; check the tenant JDBC driver and server version.");
        return new TenantHibernateDialect(dialect);
    }
    private static boolean containsMariaDb(String version) {
        return version!=null && version.toLowerCase(Locale.ROOT).contains("mariadb");
    }
    private static int[] mariaVersion(String version) throws SQLException {
        if(version==null) throw new SQLException("MariaDB server version is unavailable");
        int marker=version.toLowerCase(Locale.ROOT).indexOf("mariadb");
        String prefix=marker<0 ? version : version.substring(0,marker);
        var matches=Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?").matcher(prefix);
        int[] result=null;
        while(matches.find()) result=new int[]{Integer.parseInt(matches.group(1)),Integer.parseInt(matches.group(2)),matches.group(3)==null?0:Integer.parseInt(matches.group(3))};
        if(result==null) throw new SQLException("MariaDB server version could not be resolved");
        return result;
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

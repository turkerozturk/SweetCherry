package com.turkerozturk.multipledatabases;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** A reusable tenant registration whose JDBC pool can be explicitly released and reopened. */
public final class ManagedTenantDataSource extends AbstractDataSource implements AutoCloseable {
    private final Supplier<HikariDataSource> factory;
    private HikariDataSource pool;
    private boolean closed;
    private int borrowed;
    private String generation = UUID.randomUUID().toString();

    public ManagedTenantDataSource(Supplier<HikariDataSource> factory) {
        this.factory = factory;
        this.pool = configure(factory.get());
    }

    /** Disables SQLite's CREATE flag without changing the behavior of other JDBC drivers. */
    private static HikariDataSource configure(HikariDataSource source) {
        if (source.getJdbcUrl().startsWith("jdbc:sqlite:")) {
            SQLiteConfig sqlite = new SQLiteConfig();
            sqlite.resetOpenMode(SQLiteOpenMode.CREATE);
            source.addDataSourceProperty("open_mode", sqlite.toProperties().getProperty("open_mode"));
        }
        source.setMinimumIdle(0);
        return source;
    }

    /** Exposes configured metadata without opening a JDBC connection. */
    public synchronized HikariDataSource configuration() { return pool; }
    public synchronized String generation() { return generation; }
    public synchronized boolean isClosed() { return closed; }

    /** Verifies connectivity and the required CTB tables before a session selects this source. */
    public synchronized String activate() throws SQLException {
        if (closed) { pool = configure(factory.get()); closed = false; }
        try (Connection connection = getConnection()) {
            if (pool.getJdbcUrl().startsWith("jdbc:sqlite:")) {
                for (String table : new String[]{"node", "children", "bookmark", "image", "grid", "codebox"}) {
                    try (var statement = connection.createStatement()) {
                        statement.executeQuery("SELECT 1 FROM " + table + " LIMIT 0").close();
                    }
                }
            }
        } catch (SQLException | RuntimeException error) {
            close();
            throw error;
        }
        return generation;
    }

    /** Counts borrowed connections so explicit close cannot interrupt an active transaction. */
    @Override public synchronized Connection getConnection() throws SQLException {
        if (closed) throw new SQLException("Tenant data source is closed; select it again.");
        Connection delegate = pool.getConnection();
        borrowed++;
        AtomicBoolean returned = new AtomicBoolean();
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
            if (method.getName().equals("close")) {
                if (returned.compareAndSet(false, true)) {
                    try { delegate.close(); } finally { synchronized (ManagedTenantDataSource.this) { borrowed--; } }
                }
                return null;
            }
            try { return method.invoke(delegate, args); }
            catch (InvocationTargetException error) { throw error.getCause(); }
        });
    }

    @Override public Connection getConnection(String username, String password) throws SQLException { return getConnection(); }

    /** Releases the pool only when no caller currently owns a connection; invalidates previous selections. */
    @Override public synchronized void close() {
        if (borrowed != 0) throw new IllegalStateException("Data source has active requests; try closing it again shortly.");
        if (!closed) {
            pool.close(); closed = true; generation = UUID.randomUUID().toString();
        }
    }
}

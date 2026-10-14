package com.turkerozturk.multipledatabases;

/** Safe categories: raw server errors can contain credentials, JDBC URLs or SQL. */
public final class TenantConnectionFailure {
    private TenantConnectionFailure() { }
    public static String classify(Throwable error) {
        var visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        boolean network = false;
        for (Throwable cause = error; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql) {
                String state = sql.getSQLState();
                if ("42501".equals(state)) return "connect";
                if (state != null && state.startsWith("28")) return "credentials";
                if ("3D000".equals(state) || sql.getErrorCode() == 1049) return "database";
                if (sql.getErrorCode() == 1044) return "connect";
                if (sql.getErrorCode() == 1045) return "credentials";
                if (state != null && state.startsWith("08")) network = true;
            }
            if (cause instanceof java.net.ConnectException || cause instanceof java.net.UnknownHostException
                    || cause instanceof java.net.SocketTimeoutException) network = true;
        }
        return network ? "network" : "unknown";
    }
}

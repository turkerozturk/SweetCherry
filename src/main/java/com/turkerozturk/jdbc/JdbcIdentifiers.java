package com.turkerozturk.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/** Only developer-owned table/column identifiers; never accepts user SQL fragments. */
public final class JdbcIdentifiers {
    private JdbcIdentifiers() { }
    public static String quote(Connection connection,String identifier) throws SQLException {
        if(!identifier.matches("[A-Za-z_][A-Za-z_0-9]*")) throw new IllegalArgumentException("Invalid SQL identifier");
        String quote=connection.getMetaData().getIdentifierQuoteString().trim();
        return quote+identifier+quote;
    }
}

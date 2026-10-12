package com.turkerozturk.jdbc;

import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JdbcIdentifiersTest {
    @Test void usesDriverQuotesAndRejectsSqlFragments() throws Exception {
        var connection=mock(Connection.class);var metadata=mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getIdentifierQuoteString()).thenReturn("`");
        assertThat(JdbcIdentifiers.quote(connection,"offset")).isEqualTo("`offset`");
        when(metadata.getIdentifierQuoteString()).thenReturn("\"");
        assertThat(JdbcIdentifiers.quote(connection,"offset")).isEqualTo("\"offset\"");
        assertThatThrownBy(()->JdbcIdentifiers.quote(connection,"offset; DROP TABLE node"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

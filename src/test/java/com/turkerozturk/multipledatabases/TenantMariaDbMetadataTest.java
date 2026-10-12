package com.turkerozturk.multipledatabases;

import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class TenantMariaDbMetadataTest {
    private record Jdbc(Connection connection,DatabaseMetaData metadata,Statement statement) { }
    private Jdbc jdbc(String product,String version,String driver) throws Exception {
        var connection=mock(Connection.class);var metadata=mock(DatabaseMetaData.class);var statement=mock(Statement.class);var rows=mock(ResultSet.class);
        when(connection.getMetaData()).thenReturn(metadata);when(connection.createStatement()).thenReturn(statement);
        when(metadata.getConnection()).thenReturn(connection);when(metadata.getDatabaseProductName()).thenReturn(product);
        when(metadata.getDatabaseProductVersion()).thenReturn(version);when(metadata.getDriverName()).thenReturn(driver);
        when(metadata.getDatabaseMajorVersion()).thenReturn(5);when(metadata.getDatabaseMinorVersion()).thenReturn(5);
        when(metadata.getSQLKeywords()).thenThrow(new SQLSyntaxErrorException("Unknown column 'RESERVED' in 'WHERE'","42000"));
        when(statement.executeQuery(anyString())).thenReturn(rows);when(rows.next()).thenReturn(true,false);
        when(rows.getString(1)).thenReturn("utf8mb4");when(rows.getString(2)).thenReturn("");
        return new Jdbc(connection,metadata,statement);
    }
    @Test void connectorJCompatibilityPrefixResolvesRealMariaDbWithoutBrokenKeywordQuery() throws Exception {
        var jdbc=jdbc("MySQL","5.5.5-10.11.14-MariaDB-0+deb12u2","MySQL Connector/J");
        var result=TenantHibernateDialect.detect(jdbc.connection());
        assertThat(result.dialect()).isInstanceOf(org.hibernate.dialect.MariaDBDialect.class);
        assertThat(result.dialect().getVersion().getDatabaseMajorVersion()).isEqualTo(10);
        assertThat(result.dialect().getVersion().getDatabaseMinorVersion()).isEqualTo(11);
        assertThat(result.setting("mariadb")).isSameAs(result.dialect());
        verify(jdbc.metadata(),never()).getSQLKeywords();
        verify(jdbc.statement(),never()).executeQuery("SELECT VERSION()");
    }
    @Test void missingMariaMarkerIsResolvedFromServerVersionQuery() throws Exception {
        var jdbc=jdbc("MySQL","5.5.5","MySQL Connector/J");var version=mock(ResultSet.class);
        when(jdbc.statement().executeQuery("SELECT VERSION()")).thenReturn(version);
        when(version.next()).thenReturn(true);when(version.getString(1)).thenReturn("10.11.14-MariaDB");
        var result=TenantHibernateDialect.detect(jdbc.connection());
        assertThat(result.dialect()).isInstanceOf(org.hibernate.dialect.MariaDBDialect.class);
        assertThat(result.dialect().getVersion().getDatabaseMajorVersion()).isEqualTo(10);
        verify(jdbc.metadata(),never()).getSQLKeywords();
    }
    @Test void realMySqlMetadataErrorsAreNotHidden() throws Exception {
        var jdbc=jdbc("MySQL","8.0.40","MySQL Connector/J");
        assertThatThrownBy(()->TenantHibernateDialect.detect(jdbc.connection()))
            .isInstanceOf(org.hibernate.JDBCException.class).hasRootCauseInstanceOf(SQLSyntaxErrorException.class);
        verify(jdbc.metadata()).getSQLKeywords();
    }
    @Test void mariaDbDriverRetainsItsOwnKeywordMetadata() throws Exception {
        var jdbc=jdbc("MariaDB","10.11.14-MariaDB","MariaDB Connector/J");
        doReturn("VENDORWORD").when(jdbc.metadata()).getSQLKeywords();
        assertThat(TenantHibernateDialect.detect(jdbc.connection()).dialect()).isInstanceOf(org.hibernate.dialect.MariaDBDialect.class);
        verify(jdbc.metadata()).getSQLKeywords();
    }
}

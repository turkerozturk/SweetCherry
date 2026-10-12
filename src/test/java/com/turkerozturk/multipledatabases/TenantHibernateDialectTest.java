package com.turkerozturk.multipledatabases;

import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class TenantHibernateDialectTest {
    private TenantHibernateDialect detect(String product,int major,int minor) throws Exception {
        var connection=mock(Connection.class);var metadata=mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getConnection()).thenReturn(connection);
        var statement=mock(Statement.class);var rows=mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(rows);
        when(rows.next()).thenReturn(true,false);
        when(rows.getString(1)).thenReturn(product.equals("PostgreSQL")?"PostgreSQL "+major+"."+minor:"utf8mb4");
        when(rows.getString(2)).thenReturn("");
        when(metadata.getDatabaseProductName()).thenReturn(product);
        when(metadata.getDatabaseProductVersion()).thenReturn(major+"."+minor);
        when(metadata.getDatabaseMajorVersion()).thenReturn(major);
        when(metadata.getDatabaseMinorVersion()).thenReturn(minor);
        when(metadata.getDriverName()).thenReturn(product+" JDBC");
        when(metadata.getSQLKeywords()).thenReturn("");
        return TenantHibernateDialect.detect(connection);
    }
    @Test void detectsSupportedEnginesFromJdbcMetadata() throws Exception {
        assertThat(detect("SQLite",3,50).dialect()).isInstanceOf(org.hibernate.community.dialect.SQLiteDialect.class);
        assertThat(detect("MariaDB",10,11).dialect()).isInstanceOf(org.hibernate.dialect.MariaDBDialect.class);
        assertThat(detect("MySQL",8,0).dialect()).isInstanceOf(org.hibernate.dialect.MySQLDialect.class);
        assertThat(detect("PostgreSQL",15,0).dialect()).isInstanceOf(org.hibernate.dialect.PostgreSQLDialect.class);
    }
    @Test void overridesAreAllowlistedAndMustMatchActualConnection() throws Exception {
        var dialect=detect("SQLite",3,50);
        assertThat(dialect.setting(null)).isSameAs(dialect.dialect());
        assertThat(dialect.setting("auto")).isSameAs(dialect.dialect());
        assertThat(dialect.setting("sqlite")).isSameAs(dialect.dialect());
        assertThat(dialect.setting("org.hibernate.community.dialect.SQLiteDialect")).isSameAs(dialect.dialect());
        assertThatThrownBy(()->dialect.setting("postgresql")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->dialect.setting("java.lang.Runtime")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->detect("Unknown",1,0)).isInstanceOf(SQLException.class);
    }
    @Test void serverVersionIsPartOfTheFactoryKey() throws Exception {
        assertThat(detect("PostgreSQL",15,0).key()).isNotEqualTo(detect("PostgreSQL",16,0).key());
        var version=org.hibernate.dialect.DatabaseVersion.make(8);
        var normal=new TenantHibernateDialect(new org.hibernate.dialect.MySQLDialect(version,4,false));
        assertThat(normal.key()).isNotEqualTo(new TenantHibernateDialect(new org.hibernate.dialect.MySQLDialect(version,3,false)).key());
        assertThat(normal.key()).isNotEqualTo(new TenantHibernateDialect(new org.hibernate.dialect.MySQLDialect(version,4,true)).key());
    }
}

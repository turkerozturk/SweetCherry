package com.turkerozturk.richtext.editing;

import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RichTextObjectSqlCompatibilityTest {
    @Test void readsPayloadsAndOffsetsWithQualifiedWildcardAndMysqlQuoting() throws Exception {
        try (var actual = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (var sql = actual.createStatement()) {
                sql.execute("CREATE TABLE image (node_id INTEGER, offset INTEGER, png BLOB)");
                sql.execute("CREATE TABLE codebox (node_id INTEGER, offset INTEGER, txt TEXT)");
                sql.execute("CREATE TABLE grid (node_id INTEGER, offset INTEGER, txt TEXT)");
                sql.execute("INSERT INTO image VALUES (12, 9, X'0102')");
                sql.execute("INSERT INTO codebox VALUES (12, 3, 'code')");
                sql.execute("INSERT INTO grid VALUES (12, 6, 'table')");
                sql.execute("INSERT INTO image VALUES (99, 1, X'FF')");
            }
            var connection = mock(java.sql.Connection.class);
            var metadata = mock(java.sql.DatabaseMetaData.class);
            when(connection.getMetaData()).thenReturn(metadata);
            when(metadata.getIdentifierQuoteString()).thenReturn("`");
            when(connection.prepareStatement(anyString())).thenAnswer(call -> {
                String sql = call.getArgument(0);
                // MariaDB rejects a bare wildcard after another select expression.
                assertThat(sql).doesNotContain(", * FROM");
                return actual.prepareStatement(sql);
            });
            var objects = RichTextEditingService.readObjects(connection, 12);
            assertThat(objects).hasSize(3);
            assertThat(objects.stream().map(o -> o.reference().offset()).toList()).containsExactly(3, 6, 9);
            assertThat(objects.get(2).values()[3]).isEqualTo(new byte[]{1, 2});
            for (String table : java.util.List.of("image", "codebox", "grid")) {
                verify(connection).prepareStatement("SELECT `offset` AS editor_offset, " + table
                        + ".* FROM " + table + " WHERE node_id = ? ORDER BY `offset`");
            }
        }
    }
}

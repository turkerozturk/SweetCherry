package com.turkerozturk.richtext.editing;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Runs the production object reader against SQLite rows containing mixed binary and text values. */
class SqliteObjectPayloadTest {
    @Test void readsMixedObjectsWithoutBlobOrDecimalExtraction() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE image (node_id INTEGER, offset INTEGER, justification TEXT, anchor TEXT, png BLOB, filename TEXT, link TEXT, time INTEGER)");
                statement.execute("CREATE TABLE grid (node_id INTEGER, offset INTEGER, txt BLOB)");
                statement.execute("CREATE TABLE codebox (node_id INTEGER, offset INTEGER, txt TEXT)");
                statement.execute("INSERT INTO image VALUES (53, 0, 'left', 'heading', NULL, '', '', 0)");
                statement.execute("INSERT INTO image VALUES (53, 1, 'left', '', X'', '', '', 0)");
                statement.execute("INSERT INTO image VALUES (53, 2, 'left', '', X'000102FF89504E47', '', '', 0)");
                statement.execute("INSERT INTO image VALUES (53, 3, 'left', '', X'255044462D', 'file.pdf', '', 0)");
                statement.execute("INSERT INTO grid VALUES (53, 4, X'3C726F773E746578743C2F726F773E')");
                statement.execute("INSERT INTO codebox VALUES (53, 5, 'int x = 123;')");
                statement.execute("INSERT INTO image VALUES (99, 0, 'left', '', X'01', '', '', 0)");
            }
            var objects = RichTextEditingService.readObjects(connection, 53);
            assertThat(objects).hasSize(6);
            assertThat(objects.get(0).values()[5]).isNull();
            assertThat((byte[]) objects.get(1).values()[5]).isEmpty();
            assertThat((byte[]) objects.get(2).values()[5]).containsExactly((byte) 0, (byte) 1, (byte) 2, (byte) 255, (byte) 137, (byte) 80, (byte) 78, (byte) 71);
            assertThat(objects.get(3).values()[6]).isEqualTo("file.pdf");
            assertThat(objects.get(4).reference().table()).isEqualTo("grid");
            assertThat((byte[]) objects.get(4).values()[3]).isEqualTo("<row>text</row>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertThat(objects.get(5).reference().table()).isEqualTo("codebox");
            assertThat(objects.get(5).values()[3]).isEqualTo("int x = 123;");
            assertThat(connection.isClosed()).isFalse();
        }
    }
}

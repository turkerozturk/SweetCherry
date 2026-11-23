package com.turkerozturk.importing.dtt;

import com.turkerozturk.importing.CtbTreeWriter;
import com.turkerozturk.node.dtt.DttCatalog;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DttTopicImportTest {
    private final DttTopicImportService service = new DttTopicImportService(null, null, JsonMapper.builder().build());

    @Test
    void importsAllHierarchyLevelsAndStopsOnExistingRoot() throws Exception {
        var catalog = new DttCatalog(
                List.of(new DttCatalog.Group("1", "Group"), new DttCatalog.Group("2", "Empty group")),
                List.of(new DttCatalog.Category("10", "Category", "1"), new DttCatalog.Category("11", "Empty category", "1")),
                List.of(new DttCatalog.Topic("9007199254740993", "Walk & read", "10"),
                        new DttCatalog.Topic("43", "Walk & read", "10")));
        var preview = service.preview(catalog, "Ungrouped", "Uncategorized");
        assertThat(preview.count()).isEqualTo(2);

        try (var db = DriverManager.getConnection("jdbc:sqlite::memory:"); var sql = db.createStatement();
             var schema = getClass().getResourceAsStream("/schemas/schema-sqlite.sql")) {
            for (String statement : new String(schema.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
                if (!statement.isBlank()) sql.execute(statement);
            }
            db.setAutoCommit(false);

            var tree = DttTopicImportService.entries("Imported", preview, 123);
            assertThat(CtbTreeWriter.insert(db, tree, true)).isEqualTo(new CtbTreeWriter.Result(1L, 7));
            db.commit();
            assertThat(CtbTreeWriter.insert(db, tree, true)).isNull();
            db.commit();

            var records = new ArrayList<List<String>>();
            try (var rows = sql.executeQuery("""
                    SELECT n.name, n.txt, n.syntax, n.tags, c.father_id, c.sequence
                    FROM node n JOIN children c ON c.node_id=n.node_id ORDER BY n.node_id
                    """)) {
                while (rows.next()) records.add(List.of(rows.getString(1), rows.getString(2), rows.getString(3),
                        rows.getString(4), rows.getString(5), rows.getString(6)));
            }
            assertThat(records).containsExactly(
                    List.of("Imported", "", "plain-text", "", "0", "1"),
                    List.of("Group", "", "plain-text", "", "1", "1"),
                    List.of("Category", "", "plain-text", "", "2", "1"),
                    List.of("Walk & read", "9007199254740993", "plain-text", "dttid", "3", "1"),
                    List.of("Walk & read", "43", "plain-text", "dttid", "3", "2"),
                    List.of("Empty category", "", "plain-text", "", "2", "2"),
                    List.of("Empty group", "", "plain-text", "", "1", "2"));

            sql.executeUpdate("INSERT INTO children (node_id,father_id,sequence,master_id) VALUES (20,0,3,4)");
            assertThat(CtbTreeWriter.insert(db, DttTopicImportService.entries("Walk & read", preview, 123), true)).isNull();
            db.rollback();
        }
    }

    @Test
    void keepsLegacyUngroupedCategoriesAndRejectsInvalidBridgeIds() {
        var catalog = new DttCatalog(List.of(), List.of(new DttCatalog.Category("5", "Legacy category", "0")),
                List.of(new DttCatalog.Topic("42", "Legacy topic", "5"), new DttCatalog.Topic("43", "Orphan", "0")));
        var preview = service.preview(catalog, "Ungrouped", "Uncategorized");
        assertThat(preview.groups()).containsExactly(new DttTopicImportService.Group("Ungrouped", List.of(
                new DttTopicImportService.Category("Legacy category", List.of(new DttTopicImportService.Topic("Legacy topic", "42"))),
                new DttTopicImportService.Category("Uncategorized", List.of(new DttTopicImportService.Topic("Orphan", "43"))))));
        assertThatThrownBy(() -> service.preview(new DttCatalog(List.of(), List.of(),
                List.of(new DttCatalog.Topic("0", "Invalid", "0"))), "Ungrouped", "Uncategorized"))
                .isInstanceOf(ResponseStatusException.class);
    }
}

package com.turkerozturk.node.duplication;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SharedParentDuplicationFixtureTest {
    @Test void everyFixtureOccurrenceHasOptionsWithoutChangingAnyRows() throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:sqlite::memory:");
            var script=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")) {
            assertThat(script).isNotNull();
            String sql=new String(script.readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
            try(var statement=connection.createStatement()) {
                for(String part:sql.split(";")) if(!part.isBlank()) statement.execute(part);
            }
            var before=NodeDuplicationService.read(connection);
            for(var row:before.rows()) assertThat(NodeDuplicationService.state(before,row.id()).revision()).isNotBlank();
            assertThat(NodeDuplicationService.read(connection)).isEqualTo(before);
            assertThat(NodeDuplicationService.state(before,11).shared()).isTrue();
            assertThat(NodeDuplicationService.state(before,19).shared()).isFalse();
        }
    }
}

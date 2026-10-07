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
    @Test void sharedRootSubtreeCopiesOwnChildrenAndRetainsOriginalMasters() throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:sqlite::memory:");
            var script=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")) {
            String sql=new String(script.readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
            try(var statement=connection.createStatement()) {
                for(String part:sql.split(";")) if(!part.isBlank()) statement.execute(part);
            }
            connection.setAutoCommit(false);
            var before=NodeDuplicationService.read(connection);
            assertThat(NodeDuplicationService.state(before,11).subtreeCount()).isEqualTo(7);
            long id=NodeDuplicationService.duplicate(connection,11,true,
                    NodeDuplicationService.state(before,11).revision(),1000);
            var after=NodeDuplicationService.read(connection);
            assertThat(id).isEqualTo(20);
            assertThat(after.rows().stream().filter(row->row.id()==20).findFirst().orElseThrow().master()).isEqualTo(2);
            assertThat(after.rows().stream().filter(row->row.id()==23).findFirst().orElseThrow().master()).isEqualTo(1);
            assertThat(after.rows().stream().filter(row->row.id()==26).findFirst().orElseThrow().parent()).isEqualTo(23);
            assertThat(after.realIds()).contains(21L,22L,24L,25L,26L).doesNotContain(20L,23L);
            try(var statement=connection.createStatement()) {
                statement.execute("UPDATE node SET txt='Changed original' WHERE node_id=2");
                try(var result=statement.executeQuery("SELECT node.txt FROM children JOIN node ON node.node_id=children.master_id WHERE children.node_id=20")) {
                    assertThat(result.next()).isTrue();assertThat(result.getString(1)).isEqualTo("Changed original");
                }
            }
        }
    }

}

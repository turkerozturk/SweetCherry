package com.turkerozturk.node.moving;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SharedParentMoveFixtureTest {
    @Test void everyFixtureOccurrenceHasMoveOptionsAndNestedSharedParentsRemainIntact() throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:sqlite::memory:");
            var script=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")) {
            assertThat(script).isNotNull();
            String sql=new String(script.readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
            try(var statement=connection.createStatement()) {
                for(String part:sql.split(";")) if(!part.isBlank()) statement.execute(part);
            }
            connection.setAutoCommit(false);
            var before=NodeMoveService.read(connection);
            for(var row:before) assertThat(NodeMoveService.state(before,row.id()).revision()).isNotBlank();
            NodeMoveService.move(connection,19,NodeMoveService.Direction.LEFT,
                    NodeMoveService.state(before,19).revision());
            var after=NodeMoveService.read(connection);
            assertThat(after.stream().filter(row->row.id()==19).findFirst().orElseThrow().parent()).isEqualTo(11);
            for(long id:new long[]{11,16,18})
                assertThat(after.stream().filter(row->row.id()==id).findFirst().orElseThrow().master())
                        .isEqualTo(before.stream().filter(row->row.id()==id).findFirst().orElseThrow().master());
            var revision=NodeMoveService.state(after,19).revision();
            NodeMoveService.move(connection,19,NodeMoveService.Direction.RIGHT,revision);
            assertThat(NodeMoveService.read(connection).stream().filter(row->row.id()==19).findFirst().orElseThrow().parent()).isEqualTo(18);
        }
    }
}

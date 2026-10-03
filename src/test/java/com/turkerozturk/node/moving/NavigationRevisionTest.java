package com.turkerozturk.node.moving;
import com.turkerozturk.export.SqliteSchemaLoader;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;
class NavigationRevisionTest {
    @Test void externalMoveAndTitleUpdatesChangeRevisionButContentEditDoesNot() throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:sqlite::memory:");var statement=connection.createStatement()) {
            for(String sql:SqliteSchemaLoader.load().split(";")) if(!sql.isBlank()) statement.execute(sql);
            statement.execute("INSERT INTO node(node_id,name,txt,syntax,is_ro,is_richtxt) VALUES(1,'Node','text','plain-text',0,0),(2,'Parent','','plain-text',0,0)");
            statement.execute("INSERT INTO children VALUES(1,0,1,0),(2,0,2,0)");
            var initial=NavigationRevisionController.fingerprint(connection);
            statement.execute("UPDATE node SET txt='new content' WHERE node_id=1");
            assertThat(NavigationRevisionController.fingerprint(connection)).isEqualTo(initial);
            statement.execute("UPDATE children SET father_id=2 WHERE node_id=1");
            var moved=NavigationRevisionController.fingerprint(connection);assertThat(moved).isNotEqualTo(initial);
            statement.execute("UPDATE node SET name='Changed' WHERE node_id=1");
            assertThat(NavigationRevisionController.fingerprint(connection)).isNotEqualTo(moved);
        }
    }
}

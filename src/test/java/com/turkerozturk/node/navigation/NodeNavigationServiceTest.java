package com.turkerozturk.node.navigation;

import com.turkerozturk.bookmark.BookmarkShortcuts;
import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class NodeNavigationServiceTest {
    @Test void resolvesRealAndSharedOccurrencesWithoutLoadingContentOrWriting() throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:sqlite::memory:");var st=connection.createStatement()) {
            st.execute("CREATE TABLE children(node_id INTEGER,father_id INTEGER,sequence INTEGER,master_id INTEGER)");
            // No txt column or BLOB table exists: this feature must read metadata only.
            st.execute("CREATE TABLE node(node_id INTEGER,name TEXT,is_ro INTEGER,is_richtxt INTEGER,syntax TEXT)");
            st.execute("CREATE TABLE bookmark(node_id INTEGER,sequence INTEGER)");
            st.execute("INSERT INTO children VALUES (2,0,1,0),(11,0,2,2),(12,0,3,999)");
            st.execute("INSERT INTO node VALUES (2,'Türkçe <title>',0,3,'custom-colors')");
            st.execute("INSERT INTO bookmark VALUES (11,1),(12,2),(999,3)");
            st.execute("PRAGMA query_only=ON");
            assertThat(NodeNavigationService.exists(connection,2)).isTrue();
            assertThat(NodeNavigationService.exists(connection,11)).isTrue();
            assertThat(NodeNavigationService.exists(connection,12)).isFalse();
            assertThat(NodeNavigationService.exists(connection,999)).isFalse();
            var choices=BookmarkShortcuts.read(connection);
            assertThat(choices).hasSize(1);
            assertThat(choices.get(0).nodeId()).isEqualTo("11");
            assertThat(choices.get(0).name()).isEqualTo("Türkçe <title>");
            assertThat(choices.get(0).bold()).isTrue();
        }
    }
}

package com.turkerozturk.richtext.editing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.properties.NodePropertiesService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RichTextEditingServiceTest {
    private final NodePropertiesService properties = mock(NodePropertiesService.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final Node node = mock(Node.class);
    private final RichTextEditingService service = new RichTextEditingService(properties, new EmbeddedUploadPolicy(new com.turkerozturk.multipledatabases.CustomPropertiesHolder()));
    private final java.sql.Connection connection;
    RichTextEditingServiceTest() throws Exception {
        connection = java.sql.DriverManager.getConnection("jdbc:sqlite::memory:");
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE image (node_id INTEGER, offset INTEGER, justification TEXT, anchor TEXT, png BLOB, filename TEXT, link TEXT, time INTEGER)");
            statement.execute("CREATE TABLE grid (node_id INTEGER, offset INTEGER, justification TEXT, txt TEXT, col_min INTEGER, col_max INTEGER)");
            statement.execute("CREATE TABLE codebox (node_id INTEGER, offset INTEGER, txt TEXT)");
        }
        var session = mock(org.hibernate.Session.class);
        when(entityManager.unwrap(org.hibernate.Session.class)).thenReturn(session);
        when(session.doReturningWork(any())).thenAnswer(call -> {
            org.hibernate.jdbc.ReturningWork<?> work = call.getArgument(0);
            return work.execute(connection);
        });
        doAnswer(call -> {
            org.hibernate.jdbc.Work work = call.getArgument(0); work.execute(connection); return null;
        }).when(session).doWork(any());
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        when(properties.writable()).thenReturn(true);
        when(properties.realNode(12)).thenReturn(node);
        when(node.getSyntax()).thenReturn("plain-text");
        when(node.getTxt()).thenReturn("Text");
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(0L);
        when(query.executeUpdate()).thenReturn(1);
    }

    @org.junit.jupiter.api.AfterEach void closeConnection() throws Exception { connection.close(); }

    private void addImage() {
        try (var statement = connection.createStatement()) {
            statement.execute("INSERT INTO image VALUES (12, 1, 'left', '', X'0102', '', '', 0)");
        } catch (java.sql.SQLException error) { throw new IllegalStateException(error); }
    }

    @Test void opensPlainTextAsTextModelWithoutWriting() {
        var editor = service.open(12);
        assertThat(editor.document().runs().get(0).text()).isEqualTo("Text");
        verify(query, never()).executeUpdate();
    }

    @Test void savesRichXmlAndTimestampWithConditionalUpdate() {
        String revision = service.open(12).revision();
        service.save(12, "<node><rich_text weight='heavy'>New</rich_text></node>", revision);
        verify(entityManager).createNativeQuery(contains("is_richtxt = (is_richtxt | 1)"));
        verify(query).setParameter(eq("timestamp"), any(Long.class));
        verify(query).executeUpdate();
    }

    @Test void rejectsReadonlyTenantAndReadonlyNode() {
        when(properties.writable()).thenReturn(false);
        assertThatThrownBy(() -> service.open(12)).isInstanceOf(AccessDeniedException.class);
        when(properties.writable()).thenReturn(true);
        when(node.getIsReadOnly16bit()).thenReturn(1L);
        assertThatThrownBy(() -> service.open(12)).isInstanceOf(AccessDeniedException.class);
    }

    @Test void protectsExistingObjectsEvenWhenFlagsAreMissing() {
        addImage();
        var editor = service.open(12);
        assertThat(editor.document().runs()).anySatisfy(run -> assertThat(run.attributes()).containsEntry("__sweet_object", "image:1"));
        assertThatThrownBy(() -> service.save(12, "<node><rich_text>Removed</rich_text></node>", editor.revision()))
                .isInstanceOf(ResponseStatusException.class);
        verify(query, never()).executeUpdate();
    }

    @Test void savesTextAndMovesObjectKeysThroughTemporaryOffsets() {
        addImage();
        var editor = service.open(12);
        service.save(12, "<node><rich_text>ZZT</rich_text><rich_text __sweet_object='image:1'>\uFFFC</rich_text><rich_text>ext</rich_text></node>", editor.revision());
        verify(query).setParameter("from", 1);
        verify(query).setParameter("to", -2);
        verify(query).setParameter("from", -2);
        verify(query).setParameter("to", 3);
        verify(query, times(3)).executeUpdate();
        verify(entityManager).clear();
    }

    @Test void insertsValidatedImageAtItsFinalUnicodeOffset() throws Exception {
        var picture = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(picture, "png", bytes);
        String data = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());
        String images = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("new-image:test", data));
        String revision = service.open(12).revision();
        service.save(12, "<node><rich_text>😀</rich_text><rich_text __sweet_object='new-image:test'>\uFFFC</rich_text><rich_text>Text</rich_text></node>", revision, images);
        var stored = RichTextEditingService.readObjects(connection, 12);
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).reference().offset()).isEqualTo(1);
        assertThat(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream((byte[]) stored.get(0).values()[5])).getWidth()).isEqualTo(1);
        verify(entityManager).createNativeQuery(contains("has_image = 1"));
    }

    @Test void savesAttachmentFilenameAndUnchangedBinaryInImageTable() throws Exception {
        byte[] original = {0, 1, (byte) 255, (byte) 137};
        String files = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("new-file:test",
                java.util.Map.of("name", "Türkçe test.bin", "data", java.util.Base64.getEncoder().encodeToString(original))));
        service.save(12, "<node><rich_text>😀</rich_text><rich_text __sweet_object='new-file:test'>\uFFFC</rich_text></node>", service.open(12).revision(), "{}", files);
        var stored = RichTextEditingService.readObjects(connection, 12);
        assertThat(stored).hasSize(1); assertThat(stored.get(0).reference().offset()).isEqualTo(1);
        assertThat((byte[]) stored.get(0).values()[5]).isEqualTo(original);
        assertThat(stored.get(0).values()[6]).isEqualTo("Türkçe test.bin");
        assertThat(((Number) stored.get(0).values()[8]).longValue()).isPositive();
        verify(entityManager).createNativeQuery(contains("has_image = 1"));
    }

    @Test void savesExternalTargetWithoutLosingTextFormatting() {
        service.save(12, "<node><rich_text weight='heavy' foreground='#3584e4' link='webs https://example.org/?a=1&amp;b=2'>Text</rich_text></node>", service.open(12).revision());
        var xml = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(query).setParameter(eq("xml"), xml.capture());
        var saved = new com.turkerozturk.richtext.experimental.RichTextXmlReader().read(xml.getValue());
        assertThat(saved.runs().get(0).attributes()).containsEntry("link", "webs https://example.org/?a=1&b=2").containsEntry("weight", "heavy").containsEntry("foreground", "#3584e4");
    }
    @Test void rejectsUnsafeNewExternalTargetBeforeAnyWrite() {
        String revision = service.open(12).revision();
        assertThatThrownBy(() -> service.save(12, "<node><rich_text link='webs javascript:alert(1)'>Text</rich_text></node>", revision)).isInstanceOf(ResponseStatusException.class);
        verify(query, never()).executeUpdate();
    }

    @Test void insertsTableWithHeaderLastAndUpdatesNodeFlag() throws Exception {
        String tables = "{\"new-table:test\":[[\"Header\"],[\"Body\"]]}";
        service.save(12, "<node><rich_text>😀</rich_text><rich_text __sweet_object='new-table:test'>\uFFFC</rich_text></node>", service.open(12).revision(), "{}", "{}", tables);
        var stored = RichTextEditingService.readObjects(connection, 12);
        assertThat(stored).hasSize(1); assertThat(stored.get(0).reference().table()).isEqualTo("grid");
        assertThat(stored.get(0).reference().offset()).isEqualTo(1);
        assertThat(RichTextTableCodec.open(stored.get(0).values()[4].toString()).get(0)).containsExactly("Header");
        verify(entityManager).createNativeQuery(contains("has_table = 1"));
    }
    @Test void editsExistingTableCellsWhilePreservingMetadata() throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("INSERT INTO grid VALUES (12, 1, 'right', '<table col_widths=\"120\" future=\"keep\"><row><cell>Old</cell></row></table>', 80, 240)");
        }
        var editor = service.open(12);
        assertThat(editor.objects().get("grid:1").tableRows().get(0)).containsExactly("Old");
        service.save(12, "<node><rich_text>T</rich_text><rich_text __sweet_object='grid:1'>\uFFFC</rich_text><rich_text>ext</rich_text></node>", editor.revision(), "{}", "{}", "{\"grid:1\":[[\"New\"]]}");
        var row = RichTextEditingService.readObjects(connection, 12).get(0).values();
        assertThat(row[3]).isEqualTo("right"); assertThat(((Number) row[5]).intValue()).isEqualTo(80); assertThat(((Number) row[6]).intValue()).isEqualTo(240);
        var table = new com.turkerozturk.richtext.experimental.EmbeddedContentAdapter().readTable(row[4].toString());
        assertThat(table.attributes()).containsEntry("future", "keep").containsEntry("col_widths", "120");
        assertThat(table.rows().get(0)).containsExactly("New");
    }
    @Test void rejectsUnknownTableBeforeAnyWrite() {
        assertThatThrownBy(() -> service.save(12, "<node><rich_text>Text</rich_text></node>", service.open(12).revision(), "{}", "{}", "{\"grid:999\":[[\"X\"]]}"))
                .isInstanceOf(ResponseStatusException.class);
        verify(query, never()).executeUpdate();
    }

    @Test void rejectsUnsupportedTypeAndAliasFromRealNodeGuard() {
        when(node.getSyntax()).thenReturn("java");
        assertThatThrownBy(() -> service.open(12)).isInstanceOf(ResponseStatusException.class);
        when(properties.realNode(12)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.open(12)).isInstanceOf(ResponseStatusException.class);
    }

    @Test void rejectsStaleRevisionAndInvalidXml() {
        String revision = service.open(12).revision();
        assertThatThrownBy(() -> service.save(12, "<node/>", "old")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.save(12, "<!DOCTYPE node><node/>", revision)).isInstanceOf(ResponseStatusException.class);
        verify(query, never()).executeUpdate();
    }

    @Test void rejectsConcurrentUpdateAndPreservesUnknownAttributes() {
        when(node.getSyntax()).thenReturn("custom-colors");
        when(node.getTxt()).thenReturn("<node><rich_text future='keep'>Text</rich_text></node>");
        var editor = service.open(12);
        assertThat(editor.document().runs().get(0).attributes()).containsEntry("future", "keep");
        when(query.executeUpdate()).thenReturn(0);
        assertThatThrownBy(() -> service.save(12, node.getTxt(), editor.revision())).isInstanceOf(ResponseStatusException.class);
    }

    @Test void deletesSelectedRowsAndClearsWidgetFlagsOnlyOnSave() throws Exception {
        addImage();
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE node (node_id INTEGER, has_image INTEGER, has_table INTEGER, has_codebox INTEGER)");
            statement.execute("INSERT INTO node VALUES (12,1,1,1)");
            statement.execute("INSERT INTO grid VALUES (12,2,'left','<table><row><cell>X</cell></row></table>',200,200)");
            statement.execute("INSERT INTO codebox VALUES (12,3,'code')");
        }
        var revision = service.open(12).revision();
        service.save(12, "<node><rich_text>Text</rich_text></node>", revision, "{}", "{}", "{}",
                "[\"image:1\",\"grid:2\",\"codebox:3\"]", true);
        assertThat(RichTextEditingService.readObjects(connection,12)).isEmpty();
        try (var statement = connection.createStatement(); var row = statement.executeQuery("SELECT * FROM node WHERE node_id=12")) {
            assertThat(row.next()).isTrue();
            assertThat(row.getInt("has_image")).isZero(); assertThat(row.getInt("has_table")).isZero(); assertThat(row.getInt("has_codebox")).isZero();
        }
    }
    @Test void rejectsLockedOrIncompleteDeletionBeforeWriting() {
        addImage();var revision=service.open(12).revision();
        assertThatThrownBy(() -> service.save(12,"<node/>",revision,"{}","{}","{}","[\"image:1\"]",false))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.save(12,"<node/>",revision,"{}","{}","{}","[]",true))
                .isInstanceOf(ResponseStatusException.class);
        verify(query,never()).executeUpdate();
    }
}

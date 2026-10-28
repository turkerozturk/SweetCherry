package com.turkerozturk.editor;

import com.turkerozturk.multipledatabases.*;
import com.turkerozturk.node.contentediting.*;
import com.turkerozturk.richtext.editing.*;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EditorRecoveryTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void namespaceIsStableOpaqueAndSeparatesUsersTenantsAndDatabaseLocations() {
        String first=EditorRecoveryContext.fingerprint("admin","Notes","jdbc:sqlite:/tmp/notes.ctb");
        assertThat(first).matches("[a-f0-9]{64}").isEqualTo(EditorRecoveryContext.fingerprint("admin","Notes","jdbc:sqlite:/tmp/notes.ctb"));
        assertThat(first).isNotEqualTo(EditorRecoveryContext.fingerprint("reader","Notes","jdbc:sqlite:/tmp/notes.ctb"))
                .isNotEqualTo(EditorRecoveryContext.fingerprint("admin","Other","jdbc:sqlite:/tmp/notes.ctb"))
                .isNotEqualTo(EditorRecoveryContext.fingerprint("admin","Notes","jdbc:sqlite:/tmp/replaced.ctb"));
        assertThat(EditorRecoveryContext.fingerprint("a","bc","d")).isNotEqualTo(EditorRecoveryContext.fingerprint("ab","c","d"));
    }
    @Test void namespaceNeedsAnAuthenticatedUserAndKnownSourceButNoDatabaseConnection() {
        var tenants=mock(TenantService.class);var advice=new EditorRecoveryContext(tenants);var request=new MockHttpServletRequest();
        assertThat(advice.scope(request)).isEmpty();TenantContext.setCurrentTenant("Notes");request.setUserPrincipal(()->"admin");
        var configuration=new HikariDataSource();configuration.setJdbcUrl("jdbc:sqlite:/missing.ctb");
        when(tenants.getAllTenants()).thenReturn(java.util.Map.of("Notes",configuration));
        assertThat(advice.scope(request)).hasSize(64);assertThat(configuration.isRunning()).isFalse();
        when(tenants.getAllTenants()).thenReturn(java.util.Map.of());assertThat(advice.scope(request)).isEmpty();configuration.close();
    }
    @Test void receiptIsExplicitOnlyAfterTheExistingSaveAndNeverForAnException()throws Exception {
        var plain=mock(NodeContentEditingService.class);var rich=mock(RichTextEditingService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new NodeContentEditingController(plain),new RichTextEditingController(rich))
                .setViewResolvers((name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view")).build();
        mvc.perform(post("/nodes/content/15").header("X-SweetCherry-Editor-Save","1").param("text","İçerik").param("continueEditing","true"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.saved").value(true)).andExpect(jsonPath("$.redirect").value("/nodes/content/15"));
        verify(plain).update(15,"İçerik");
        mvc.perform(post("/nodes/richtext/edit/15").header("X-SweetCherry-Editor-Save","1").param("xml","<node/>").param("revision","revision"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.redirect").value("/nodes/15"));
        verify(rich).save(15,"<node/>","revision","{}","{}","{}","[]",false,"{}");
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT)).when(plain).update(15,"changed");
        mvc.perform(post("/nodes/content/15").header("X-SweetCherry-Editor-Save","1").param("text","changed")).andExpect(status().isConflict());
        mvc.perform(post("/nodes/content/15").param("text","old browser")).andExpect(view().name("redirect:/nodes/15"));
    }
}

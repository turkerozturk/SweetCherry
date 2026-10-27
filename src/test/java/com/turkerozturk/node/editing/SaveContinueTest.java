package com.turkerozturk.node.editing;

import com.turkerozturk.node.contentediting.*;
import com.turkerozturk.richtext.editing.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SaveContinueTest {
    @Test void plainSaveAndContinueRedirectsToEditorWhileNormalSaveKeepsExistingRoute()throws Exception {
        var service=mock(NodeContentEditingService.class);var mvc=MockMvcBuilders.standaloneSetup(new NodeContentEditingController(service)).build();
        mvc.perform(post("/nodes/content/12").param("text","Türkçe").param("continueEditing","true")).andExpect(redirectedUrl("/nodes/content/12"));
        mvc.perform(post("/nodes/content/12").param("text","Türkçe")).andExpect(redirectedUrl("/nodes/12"));
        verify(service,times(2)).update(12,"Türkçe");
    }
    @Test void richSaveAndContinueKeepsAllPayloadAndRevisionChecks()throws Exception {
        var service=mock(RichTextEditingService.class);var mvc=MockMvcBuilders.standaloneSetup(new RichTextEditingController(service)).build();
        mvc.perform(post("/nodes/richtext/edit/12").param("xml","<node/>").param("revision","r1").param("images","{image}").param("tables","{table}").param("continueEditing","true")).andExpect(redirectedUrl("/nodes/richtext/edit/12"));
        verify(service).save(12,"<node/>","r1","{image}","{}","{table}","[]",false,"{}");
        doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT)).when(service).save(12,"<node/>","stale","{}","{}","{}","[]",false,"{}");
        mvc.perform(post("/nodes/richtext/edit/12").param("xml","<node/>").param("revision","stale").param("continueEditing","true")).andExpect(status().isConflict());
    }
}

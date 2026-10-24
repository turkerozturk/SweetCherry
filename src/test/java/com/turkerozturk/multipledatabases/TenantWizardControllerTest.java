package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantWizardControllerTest {
    @TempDir Path directory;
    @Test void databaseIconIsBoundFromTheRealHttpForm()throws Exception {
        var service=mock(TenantWizardService.class);var selection=mock(DatabaseSwitchController.class);
        when(service.save(any())).thenReturn("Demo");when(selection.setTenant(eq("Demo"),any())).thenReturn("redirect:/");
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new TenantWizardController(service,selection)).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tenants/wizard")
            .param("databaseIcon","CHERRY_BLUE").param("name","Demo"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/"));
        var saved=org.mockito.ArgumentCaptor.forClass(TenantWizardForm.class);verify(service).save(saved.capture());
        assertThat(saved.getValue().databaseIcon).isEqualTo("CHERRY_BLUE");
    }
    @Test void browseListsOnlyFoldersAndCtbWithoutContentAndDisablesCaching()throws Exception {
        Files.createDirectory(directory.resolve("notes"));Files.writeString(directory.resolve("one.ctb"),"payload");Files.writeString(directory.resolve("secret.txt"),"private");
        var controller=new TenantWizardController(mock(TenantWizardService.class),mock(DatabaseSwitchController.class));
        var response=controller.browse(directory.toString());assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody().entries()).extracting(TenantWizardController.Entry::name).containsExactly("notes","one.ctb");
    }
    @Test void saveDelegatesToExistingSelectionFlowAndErrorsNeverReturnPassword()throws Exception {
        var service=mock(TenantWizardService.class);var selection=mock(DatabaseSwitchController.class);var c=new TenantWizardController(service,selection);
        var f=new TenantWizardForm();var req=new MockHttpServletRequest();var res=new MockHttpServletResponse();var model=new ExtendedModelMap();
        var binding=new org.springframework.validation.BeanPropertyBindingResult(f,"wizard");
        when(service.save(f)).thenReturn("Demo");when(selection.setTenant("Demo",req)).thenReturn("redirect:/");
        assertThat(c.save(f,binding,model,req,res)).isEqualTo("redirect:/");verify(selection).setTenant("Demo",req);
        f.password="secret";when(service.save(f)).thenThrow(new IllegalArgumentException("wizard.stale"));
        assertThat(c.save(f,binding,model,req,res)).isEqualTo("tenantWizard");assertThat(f.password).isEmpty();assertThat(model.get("wizardError")).isEqualTo("wizard.stale");
    }
}

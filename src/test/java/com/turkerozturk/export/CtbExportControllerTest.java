package com.turkerozturk.export;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CtbExportControllerTest {
    @Test void collectionPassesSelectedOccurrenceAndBothScopesToCommonService() throws Exception {
        for(boolean descendants:new boolean[]{false,true}) {
            var service=mock(CtbExportService.class);var flash=mock(RedirectAttributes.class);
            when(service.export(11,descendants,true)).thenReturn(new CtbExportService.Result(1,"exportednodes.ctb"));
            assertThat(new ExportManipulatedRecursiveController(service).export1(11,descendants,flash))
                    .isEqualTo("redirect:/export-result");
            verify(service).export(11,descendants,true);
        }
    }
    @Test void separatePassesSelectedOccurrenceAndBothScopesToCommonService() throws Exception {
        for(boolean descendants:new boolean[]{false,true}) {
            var service=mock(CtbExportService.class);var flash=mock(RedirectAttributes.class);
            when(service.export(11,descendants,false)).thenReturn(new CtbExportService.Result(1,"branch.ctb"));
            assertThat(new ExportRecursiveController(service).export1(11,descendants,flash))
                    .isEqualTo("redirect:/export-result");
            verify(service).export(11,descendants,false);
        }
    }
    @Test void getRequestsDoNotCreateExportFiles() {
        var service=mock(CtbExportService.class);
        assertThat(new ExportRecursiveController(service).exportPage(11)).isEqualTo("redirect:/nodes/11");
        assertThat(new ExportManipulatedRecursiveController(service).exportPage(11)).isEqualTo("redirect:/nodes/11");
        verifyNoInteractions(service);
    }
    @Test void flashStoresStructuredResultAndDirectVisitShowsTranslatedEmptyState() throws Exception {
        var service=mock(CtbExportService.class);
        when(service.export(11,true,true)).thenReturn(new CtbExportService.Result(7,"Türkçe.ctb"));
        var flash=new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        new ExportManipulatedRecursiveController(service).export1(11,true,flash);
        assertThat(flash.getFlashAttributes().get("exportStatus")).isEqualTo("success");
        assertThat(flash.getFlashAttributes().get("exportCount")).isEqualTo(7);
        assertThat(flash.getFlashAttributes().get("exportFilename")).isEqualTo("Türkçe.ctb");
        assertThat(flash.getFlashAttributes()).doesNotContainKey("contentText");
        var model=new org.springframework.ui.ExtendedModelMap();
        assertThat(new ExportManipulatedRecursiveController(service).exportResult(model)).isEqualTo("expo");
        assertThat(model).containsEntry("exportStatus","none");
    }

}

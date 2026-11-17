package com.turkerozturk.multipledatabases.info;

import com.turkerozturk.node.links.InternalLinkReportService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseInfoControllerTest {
    @Test void defaultsInvalidInputToBrokenLinksAndKeepsPaginationOnInfoPage() {
        var info = mock(DatabaseInfoService.class);
        var links = mock(InternalLinkReportService.class);
        var model = new ExtendedModelMap();
        assertThat(new DatabaseInfoController(info,links).show("", "broken", "oops", "1", model))
                .isEqualTo("database-info");
        assertThat(model).containsEntry("invalidOptions",true).containsEntry("reportPath","/database-info");
        verify(info).inspect();
        verify(links).inspect(new InternalLinkReportService.Options("","broken",50,1));
    }
    @Test void passesSearchStatusAndPageToExistingReport() {
        var info = mock(DatabaseInfoService.class);
        var links = mock(InternalLinkReportService.class);
        var model = new ExtendedModelMap();
        new DatabaseInfoController(info,links).show("source", "all", "25", "3", model);
        verify(links).inspect(new InternalLinkReportService.Options("source","all",25,3));
        assertThat(model).doesNotContainKey("invalidOptions");
    }
}

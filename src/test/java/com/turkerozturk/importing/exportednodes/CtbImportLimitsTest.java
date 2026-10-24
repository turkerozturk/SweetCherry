package com.turkerozturk.importing.exportednodes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import org.springframework.test.util.ReflectionTestUtils;
import com.turkerozturk.node.properties.NodePropertiesService;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CtbImportLimitsTest {
    @TempDir Path directory;
    @Test void configuredCombinedRecordAndDecodedByteBudgetsAreEnforced()throws Exception {
        var fixture=new CtbImportTest();fixture.directory=directory;var file=fixture.source();
        assertThat(CtbImportData.read(file,67108864,10).counts()).containsEntry("children",3);
        assertThatThrownBy(()->CtbImportData.read(file,67108864,9)).hasMessage("limit");
        assertThatThrownBy(()->CtbImportData.read(file,10,200000)).hasMessage("limit");
        assertThatThrownBy(()->CtbImportData.read(file,0,200000)).hasMessage("limit");
    }
    @Test void serviceUsesConfiguredFileBudgetWithOriginalDefaults()throws Exception {
        var fixture=new CtbImportTest();fixture.directory=directory;var file=fixture.source();
        var properties=mock(NodePropertiesService.class);when(properties.writable()).thenReturn(true);
        var service=new CtbImportService(properties);
        ReflectionTestUtils.setField(service,"applicationPath",directory.toString());ReflectionTestUtils.setField(service,"folder","exports");
        assertThat(service.maxBytes()).isEqualTo(67108864);assertThat(service.maxRecords()).isEqualTo(200000);
        ReflectionTestUtils.setField(service,"maxBytes",Files.size(file)-1);
        assertThatThrownBy(()->service.preview(file.getFileName().toString())).hasMessage("limit");
        ReflectionTestUtils.setField(service,"maxBytes",Files.size(file));
        assertThat(service.preview(file.getFileName().toString()).counts()).containsEntry("children",3);
    }
}

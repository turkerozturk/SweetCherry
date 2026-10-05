package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;

class TenantConfigDownloadControllerTest {
    @TempDir Path directory;
    private TenantConfigDownloadController controller(String filename) {
        var service = mock(TenantService.class);
        when(service.getAllTenants()).thenReturn(Map.of("Demo", mock(DataSource.class)));
        var holder = new CustomPropertiesHolder();
        holder.addCustomProperties("Demo", Map.of("propertyFileName", filename));
        return new TenantConfigDownloadController(service, holder, directory);
    }
    @Test void downloadsExactRegisteredBytesWithoutCaching() throws Exception {
        byte[] contents = "name=Demo\ndatasource.password=secret\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(directory.resolve("demo.txt"), contents);
        var response = controller("demo.txt").download("Demo");
        assertThat(response.getBody()).isEqualTo(contents);
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("demo.txt");
    }
    @Test void rejectsUnregisteredTenant() {
        assertThatThrownBy(() -> controller("demo.txt").download("Unknown")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsPathTraversalAndMissingFile() {
        assertThatThrownBy(() -> controller("../secret.txt").download("Demo")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller("missing.txt").download("Demo")).isInstanceOf(ResponseStatusException.class);
    }
}

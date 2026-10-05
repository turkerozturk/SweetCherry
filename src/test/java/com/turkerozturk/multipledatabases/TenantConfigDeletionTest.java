package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import javax.sql.DataSource;
import java.nio.file.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantConfigDeletionTest {
    @TempDir Path directory;
    private MultitenantConfiguration configuration(MultitenantDataSource routing, CustomPropertiesHolder holder) {
        var configuration = new MultitenantConfiguration() {
            @Override public DataSource dataSource() { return routing; }
        };
        ReflectionTestUtils.setField(configuration, "customPropertiesHolder", holder);
        return configuration;
    }
    @Test void deletesOnlyConfigAndUnregistersOnlyItsPool() throws Exception {
        Path config=Files.writeString(directory.resolve("demo.txt"),"name=Demo");
        Path database=Files.writeString(directory.resolve("demo.ctb"),"database payload");
        var pool=mock(ManagedTenantDataSource.class);
        var other=mock(ManagedTenantDataSource.class);
        var routing=new MultitenantDataSource();
        routing.setTargetDataSources(Map.of("Demo",pool,"Other",other)); routing.afterPropertiesSet();
        var holder=new CustomPropertiesHolder(); holder.addCustomProperties("Demo",Map.of("propertyFileName","demo.txt"));
        configuration(routing,holder).deleteTenantConfig("Demo",config);
        assertThat(Files.exists(config)).isFalse();
        assertThat(Files.readString(database)).isEqualTo("database payload");
        assertThat(routing.getResolvedDataSources()).containsKey("Other").doesNotContainKey("Demo");
        assertThat(holder.getCustomProperties("Demo")).isNull();
        verify(pool).close(); verifyNoInteractions(other);
    }
    @Test void busyPoolPreventsFileAndRegistrationDeletion() throws Exception {
        Path config=Files.writeString(directory.resolve("demo.txt"),"name=Demo");
        var pool=mock(ManagedTenantDataSource.class); doThrow(new IllegalStateException("busy")).when(pool).close();
        var routing=new MultitenantDataSource(); routing.setTargetDataSources(Map.of("Demo",pool)); routing.afterPropertiesSet();
        assertThatThrownBy(()->configuration(routing,new CustomPropertiesHolder()).deleteTenantConfig("Demo",config)).isInstanceOf(IllegalStateException.class);
        assertThat(Files.exists(config)).isTrue(); assertThat(routing.getResolvedDataSources()).containsKey("Demo");
    }
}

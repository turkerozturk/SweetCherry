package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class TenantOrderingTest {
    @TempDir Path directory;
    @Test void ctbChoicesUseNewestFileFirstButTheTableUsesNamesWithoutOpeningPools()throws Exception {
        Path older=Files.writeString(directory.resolve("older.ctb"),"test"),newer=Files.writeString(directory.resolve("newer.ctb"),"test");
        Files.setLastModifiedTime(older,FileTime.fromMillis(1000000));Files.setLastModifiedTime(newer,FileTime.fromMillis(2000000));
        var pools=new LinkedHashMap<String,HikariDataSource>();
        for(var e:Map.of("A older","jdbc:sqlite:"+older,"Z newer","jdbc:sqlite:"+newer,"Missing","jdbc:sqlite:"+directory.resolve("missing.ctb"),"B Postgres","jdbc:postgresql://unreachable/test","a MariaDB","jdbc:mysql://unreachable/test").entrySet()) {
            var pool=new HikariDataSource();pool.setJdbcUrl(e.getValue());pools.put(e.getKey(),pool);
        }
        try {
            var routing=new MultitenantDataSource();routing.setTargetDataSources(new HashMap<>(pools));routing.afterPropertiesSet();
            var holder=new CustomPropertiesHolder();pools.keySet().forEach(name->holder.addCustomProperties(name,Map.of("propertyFileName","sample.txt")));
            var service=new TenantService();ReflectionTestUtils.setField(service,"dataSource",routing);ReflectionTestUtils.setField(service,"customPropertiesHolder",holder);
            assertThat(service.getSelectionNames()).containsExactly("Z newer","A older","Missing","a MariaDB","B Postgres");
            assertThat(service.getTenantForms()).extracting(TenantForm::getTenantName).containsExactly("a MariaDB","A older","B Postgres","Missing","Z newer");
            assertThat(service.getTenantForms().stream().filter(f->f.getTenantName().equals("Z newer")).findFirst().orElseThrow().getModifiedDate()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}-\\d{2}-\\d{2}");
            Files.setLastModifiedTime(older,FileTime.fromMillis(3000000));assertThat(service.getSelectionNames().get(0)).isEqualTo("A older");
            assertThat(Files.exists(directory.resolve("missing.ctb"))).isFalse();
            assertThat(pools.values()).allMatch(pool->!pool.isRunning());
        }finally{pools.values().forEach(HikariDataSource::close);}
    }
    @Test void sqliteUriAndNonFileSourcesAreDistinguished()throws Exception {
        Path file=Files.writeString(directory.resolve("Türkçe notes.ctb"),"test");
        assertThat(TenantFileMetadata.ctbPath("jdbc:sqlite:"+file.toUri()+"?mode=ro")).isEqualTo(file.toAbsolutePath());
        assertThat(TenantFileMetadata.modified("jdbc:sqlite:"+file.toUri()+"?mode=ro")).isEqualTo(Files.getLastModifiedTime(file).toMillis());
        assertThat(TenantFileMetadata.ctbPath("jdbc:sqlite:file:relative%20notes.ctb?mode=ro"))
                .isEqualTo(Path.of("relative notes.ctb").toAbsolutePath().normalize());
        assertThat(TenantFileMetadata.ctbPath("jdbc:sqlite::memory:")).isNull();
        assertThat(TenantFileMetadata.ctbPath("jdbc:postgresql://example/test")).isNull();
    }
}

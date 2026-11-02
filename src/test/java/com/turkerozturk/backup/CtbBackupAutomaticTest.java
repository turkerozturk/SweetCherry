package com.turkerozturk.backup;

import com.turkerozturk.multipledatabases.*;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CtbBackupAutomaticTest {
    @TempDir Path folder;
    private CtbBackupSettings settings(){var s=new CtbBackupSettings();s.setDirectory(folder.resolve("backups").toString());return s;}
    private Path fixture(String name)throws Exception {
        Path file=folder.resolve(name+".ctb");
        try(var in=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql");var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement()) {
            String sql=new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().filter(line->!line.stripLeading().startsWith("--")).collect(java.util.stream.Collectors.joining("\n"));
            for(String statement:sql.split(";"))if(!statement.isBlank())q.execute(statement);
        }
        return file;
    }
    private SQLiteDataSource source(Path file){var s=new SQLiteDataSource();s.setUrl("jdbc:sqlite:"+file);return s;}
    private CtbBackupService.Status done(CtbBackupService service,String id)throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        while(service.isRunning() && System.nanoTime()<end)Thread.sleep(10);
        assertThat(service.status(id).state()).isEqualTo("SUCCEEDED");return service.status(id);
    }
    @Test void settingsAreOptInAndOldCatalogsRemainManualAndReadable()throws Exception {
        var s=settings();assertThat(s.getAutomatic().isEnabled()).isFalse();assertThat(s.getAutomatic().getTenants()).isEmpty();assertThat(s.getAutomatic().getKeepLast()).isZero();
        assertThatThrownBy(()->s.getAutomatic().setIntervalMinutes(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->s.getAutomatic().setRetryMinutes(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->s.getAutomatic().setKeepLast(-1)).isInstanceOf(IllegalArgumentException.class);
        var store=new CtbBackupStore(s);String id="20261102-000000-"+UUID.randomUUID();Path file=store.file(id);Files.copy(fixture("legacy"),file);
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var old=new LinkedHashMap<String,Object>();old.put("id",id);old.put("fileName",file.getFileName().toString());old.put("sourceName","Old");old.put("created","2026-11-02T00:00:00Z");old.put("bytes",Files.size(file));old.put("sha256",CtbBackupStore.sha256(file));old.put("counts",Map.of("node",1));
        json.writeValue(store.root().resolve("backup-"+id+".json").toFile(),old);
        assertThat(store.checked(id).automatic()).isFalse();assertThat(store.checked(id).sourceKey()).isNull();
    }
    @Test void aliasesAreGroupedWithoutOpeningPoolsAndUnsupportedOrMissingNamesAreVisible()throws Exception {
        Path file=fixture("Türkçe notes");var tenants=mock(TenantService.class);
        var managed=new ManagedTenantDataSource(()->{var pool=new HikariDataSource();pool.setJdbcUrl("jdbc:sqlite:"+file);return pool;});
        try {
            managed.close();var alias=new SQLiteDataSource();alias.setUrl("jdbc:sqlite:"+file.toUri()+"?mode=ro");
            var server=new HikariDataSource();server.setJdbcUrl("jdbc:postgresql://unreachable.invalid/database");
            when(tenants.getAllTenants()).thenReturn(Map.of("A",managed,"B",alias,"Server",server));
            var groups=new CtbBackupSources(tenants).resolve(List.of("A","B","Server","Missing"));
            assertThat(groups.stream().filter(g->g.error()==null).toList()).singleElement().satisfies(g->assertThat(g.names()).containsExactly("A","B"));
            assertThat(groups).extracting(CtbBackupSources.Source::error).contains("ctbBackup.auto.missing","ctbBackup.error.unsupported");
            assertThat(managed.isClosed()).isTrue();assertThat(managed.configuration().getHikariPoolMXBean()).isNull();
        }finally{managed.close();}
    }
    @Test void retentionKeepsManualLegacyOtherSourcesAndWorkingCopies()throws Exception {
        Path a=fixture("A"),b=fixture("B");var s=settings();s.getAutomatic().setEnabled(true);s.getAutomatic().setKeepLast(1);
        var store=new CtbBackupStore(s);var tenants=mock(TenantService.class);when(tenants.getAllTenants()).thenReturn(Map.of());
        var retention=new CtbBackupRetention(s,store,new CtbBackupSources(tenants));var engine=new CtbBackupEngine();var service=new CtbBackupService(s,engine,store,retention);
        try {
            String manual=service.start(source(a),"Manual");done(service,manual);
            String first=service.startAutomatic(a,"Auto A");done(service,first);Path working=store.workingCopy(first,engine);
            String other=service.startAutomatic(b,"Auto B");done(service,other);
            String latest=service.startAutomatic(a,"Renamed connection A");assertThat(done(service,latest).removed()).isEqualTo(1);
            assertThat(store.catalog().backups()).extracting(CtbBackupStore.Backup::id).containsExactlyInAnyOrder(manual,other,latest);
            assertThat(Files.exists(working)).isTrue();assertThat(store.get(latest).sourceKey()).isEqualTo(store.get(manual).sourceKey());
            assertThatThrownBy(()->store.get(first)).isInstanceOf(CtbBackupProblem.class);
        }finally{service.close();}
    }
    @Test void retentionSkipsEditedFilesAndFilesRegisteredAsTenants()throws Exception {
        Path file=fixture("protected");var s=settings();s.getAutomatic().setEnabled(true);s.getAutomatic().setKeepLast(0);
        var store=new CtbBackupStore(s);var tenants=mock(TenantService.class);when(tenants.getAllTenants()).thenReturn(Map.of());
        var retention=new CtbBackupRetention(s,store,new CtbBackupSources(tenants));var service=new CtbBackupService(s,new CtbBackupEngine(),store,retention);
        try {
            String edited=service.startAutomatic(file,"A");done(service,edited);
            byte[] bytes=Files.readAllBytes(store.file(edited));bytes[bytes.length-1]^=1;Files.write(store.file(edited),bytes);
            String registered=service.startAutomatic(file,"A");done(service,registered);
            String journal=service.startAutomatic(file,"A");done(service,journal);
            Files.writeString(Path.of(store.file(journal).toString()+"-wal"),"in use");
            when(tenants.getAllTenants()).thenReturn(Map.of("Directly registered snapshot",source(store.file(registered))));
            s.getAutomatic().setKeepLast(1);String latest=service.startAutomatic(file,"A");assertThat(done(service,latest).skipped()).isEqualTo(3);
            assertThat(store.catalog().backups()).hasSize(4);assertThat(store.checked(latest).automatic()).isTrue();
        }finally{service.close();}
    }
    static class Time extends Clock {
        Instant now=Instant.parse("2026-11-02T12:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}
    }
    @Test void schedulerUsesExplicitSourcesDeduplicatesAliasesAndBacksOffFailures()throws Exception {
        var s=settings();var sources=mock(CtbBackupSources.class);var service=mock(CtbBackupService.class);var store=mock(CtbBackupStore.class);var time=new Time();
        var scheduler=new CtbBackupScheduler(s,sources,service,store,time);
        try {
            scheduler.tick();verifyNoInteractions(sources,service,store);
            s.getAutomatic().setEnabled(true);s.getAutomatic().setTenants(List.of("A","Alias","B"));
            var a=new CtbBackupSources.Source("a".repeat(64),folder.resolve("a.ctb"),List.of("A","Alias"),null);
            var b=new CtbBackupSources.Source("b".repeat(64),folder.resolve("b.ctb"),List.of("B"),null);
            when(sources.resolve(s.getAutomatic().getTenants())).thenReturn(List.of(a,b));when(store.catalog()).thenReturn(new CtbBackupStore.Catalog(List.of(),0));
            when(service.startAutomatic(a.path(),"A")).thenThrow(new CtbBackupProblem("source"));
            scheduler.tick();verify(service).startAutomatic(a.path(),"A");scheduler.tick();verify(service).startAutomatic(b.path(),"B");
            scheduler.tick();verify(service,times(1)).startAutomatic(a.path(),"A");verify(service,times(1)).startAutomatic(b.path(),"B");
            time.now=time.now.plusSeconds(301);scheduler.tick();verify(service,times(2)).startAutomatic(a.path(),"A");
        }finally{scheduler.close();}
    }
    @Test void restartUsesPersistentCompletionAndSuccessfulManualBackupPostponesAutomaticWork()throws Exception {
        var s=settings();s.getAutomatic().setEnabled(true);s.getAutomatic().setIntervalMinutes(1);
        var sources=mock(CtbBackupSources.class);var service=mock(CtbBackupService.class);var store=mock(CtbBackupStore.class);var time=new Time();
        String key="a".repeat(64);var source=new CtbBackupSources.Source(key,folder.resolve("a.ctb"),List.of("A"),null);
        when(sources.resolve(anyList())).thenReturn(List.of(source));
        var recent=new CtbBackupStore.Backup("id","file","A",time.now.toString(),1,"hash",Map.of(),key,false);
        when(store.catalog()).thenReturn(new CtbBackupStore.Catalog(List.of(recent),0));
        var scheduler=new CtbBackupScheduler(s,sources,service,store,time);
        try {
            scheduler.tick();verify(service,never()).startAutomatic(any(),any());time.now=time.now.plusSeconds(61);scheduler.tick();verify(service).startAutomatic(source.path(),"A");
            var completed=new CtbBackupStore.Backup("new","file","A",time.now.plusSeconds(1).toString(),1,"hash",Map.of(),key,true);
            when(store.catalog()).thenReturn(new CtbBackupStore.Catalog(List.of(completed),0));
            time.now=time.now.plusSeconds(62);scheduler.tick();verify(service,times(2)).startAutomatic(source.path(),"A");
        }finally{scheduler.close();}
    }
    @Test void failedAutomaticSnapshotCannotDeleteEarlierSuccessfulBackups()throws Exception {
        Path file=fixture("failure");var s=settings();s.getAutomatic().setEnabled(true);s.getAutomatic().setKeepLast(1);
        var store=new CtbBackupStore(s);var tenants=mock(TenantService.class);when(tenants.getAllTenants()).thenReturn(Map.of());
        var engine=spy(new CtbBackupEngine());var retention=new CtbBackupRetention(s,store,new CtbBackupSources(tenants));var service=new CtbBackupService(s,engine,store,retention);
        try {
            String first=service.startAutomatic(file,"A");done(service,first);
            doThrow(new CtbBackupProblem("integrity")).when(engine).verify(any());String failed=service.startAutomatic(file,"A");
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(service.isRunning() && System.nanoTime()<end)Thread.sleep(10);
            assertThat(service.status(failed).state()).isEqualTo("FAILED");assertThat(store.catalog().backups()).extracting(CtbBackupStore.Backup::id).containsExactly(first);assertThat(store.checked(first)).isNotNull();
        }finally{service.close();}
    }
    @Test void busyOrDisabledServiceDoesNotScanOrOpenAutomaticSources()throws Exception {
        var s=settings();s.getAutomatic().setEnabled(true);var sources=mock(CtbBackupSources.class);var service=mock(CtbBackupService.class);var store=mock(CtbBackupStore.class);
        var scheduler=new CtbBackupScheduler(s,sources,service,store,new Time());
        try {
            when(service.isRunning()).thenReturn(true);scheduler.tick();verifyNoInteractions(sources,store);
            reset(service);s.setEnabled(false);scheduler.tick();verifyNoInteractions(service,sources,store);
        }finally{scheduler.close();}
    }
    @Test void slowFailedJobGetsRetryDelayAfterItsCompletionNotItsStart()throws Exception {
        var s=settings();s.getAutomatic().setEnabled(true);var sources=mock(CtbBackupSources.class);var service=mock(CtbBackupService.class);var store=mock(CtbBackupStore.class);var time=new Time();
        var source=new CtbBackupSources.Source("a".repeat(64),folder.resolve("a.ctb"),List.of("A"),null);
        when(sources.resolve(anyList())).thenReturn(List.of(source));when(store.catalog()).thenReturn(new CtbBackupStore.Catalog(List.of(),0));
        when(service.startAutomatic(any(),any())).thenReturn("job");
        when(service.status("job")).thenReturn(new CtbBackupService.Status("job","A",time.now.toString(),"FAILED","FINISHED",0,0,"ctbBackup.error.failed"));
        var scheduler=new CtbBackupScheduler(s,sources,service,store,time);
        try {
            scheduler.tick();time.now=time.now.plusSeconds(600);scheduler.tick();
            verify(service,times(1)).startAutomatic(any(),any());time.now=time.now.plusSeconds(301);scheduler.tick();
            verify(service,times(2)).startAutomatic(any(),any());
        }finally{scheduler.close();}
    }
}

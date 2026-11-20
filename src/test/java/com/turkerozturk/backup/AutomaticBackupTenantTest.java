package com.turkerozturk.backup;

import com.turkerozturk.settings.editing.ApplicationSettingsService;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutomaticBackupTenantTest {
    @TempDir Path directory;
    @Test void enablesWithoutReplacingOtherNamesIntervalsRetentionOrComments(){
        for(String list:List.of("[]","['Other', 'O''Brien']","\n      - Other\n      - O'Brien")){
            String yaml="# 🍒 keep\nmyapp:\n ctb-backup:\n  automatic:\n   enabled: false # flag\n   tenants: "+list+" # names\n   interval-minutes: 90\n   keep-last: 5\n";
            String after=AutomaticBackupTenantList.enable(yaml,"Türkçe");
            assertThat(after).contains("enabled: true # flag","Türkçe","interval-minutes: 90","keep-last: 5","# 🍒 keep","# names");
            assertThat(AutomaticBackupTenantList.enable(after,"Türkçe")).isEqualTo(after);
        }
        assertThat(AutomaticBackupTenantList.enable("server.port: 8080\n","Notes")).contains("myapp.ctb-backup.automatic.tenants: ['Notes']","myapp.ctb-backup.automatic.enabled: true");
    }
    @Test void enablesLiveOnlyAfterPersistenceAndOnlyForEligibleSources(){
        var settings=new CtbBackupSettings();settings.getAutomatic().setTenants(List.of("Other"));var sources=mock(CtbBackupSources.class);
        when(sources.resolve(List.of("Notes"))).thenReturn(List.of(new CtbBackupSources.Source("key",directory.resolve("notes.ctb"),List.of("Notes"),null)));
        var scheduler=new CtbBackupScheduler(settings,sources,mock(CtbBackupService.class),mock(CtbBackupStore.class));
        try{
            assertThatThrownBy(()->scheduler.enableTenant("Notes",()->{throw new IllegalArgumentException("stale");})).isInstanceOf(IllegalArgumentException.class);
            assertThat(settings.getAutomatic().isEnabled()).isFalse();assertThat(settings.getAutomatic().getTenants()).containsExactly("Other");
            scheduler.enableTenant("Notes",()->{});assertThat(settings.getAutomatic().isEnabled()).isTrue();assertThat(settings.getAutomatic().getTenants()).containsExactly("Other","Notes");
            assertThatThrownBy(()->scheduler.enableTenant("Unknown",()->{throw new AssertionError("must not persist");})).isInstanceOf(CtbBackupProblem.class);
        }finally{scheduler.close();}
    }
    @Test void removesOnlyRequestedNameFromFlowAndBlockListsIncludingUnicodeAndLastEntry(){
        for(var list:List.of("['Notes', 'Türkçe', 'O''Brien']", "\n        - Notes\n        - Türkçe\n        - O'Brien")){
            String yaml="# 🍒 preserve\nmyapp:\n  ctb-backup:\n    automatic:\n      enabled: true\n      tenants: "+list+" # retain\n      interval-minutes: 60\nserver:\n  port: 8080 # keep\n";
            String after=AutomaticBackupTenantList.remove(yaml,"Türkçe");
            assertThat(after).contains("# 🍒 preserve","# retain","interval-minutes: 60","port: 8080 # keep","Notes","O''Brien").doesNotContain("Türkçe");
            after=AutomaticBackupTenantList.remove(after,"O'Brien");after=AutomaticBackupTenantList.remove(after,"Notes");assertThat(after).contains("[]");
        }
        assertThat(AutomaticBackupTenantList.remove("myapp.ctb-backup.automatic.tenants: [Only]\n","Only")).contains("tenants: []");
    }
    @Test void staleOrUnsafeYamlIsRejected(){
        for(var yaml:List.of("myapp.ctb-backup.automatic.tenants: []\n","myapp.ctb-backup.automatic.tenants: Notes\n","myapp: {}\n","x: !!java.lang.Runtime {}\n","myapp.ctb-backup.automatic.tenants: &a [Notes]\nother: *a\n","myapp.ctb-backup.automatic.tenants: [Notes]\nmyapp.ctb-backup.automatic.tenants: [Notes]\n"))
            assertThatThrownBy(()->AutomaticBackupTenantList.remove(yaml,"Notes")).isInstanceOf(RuntimeException.class);
    }
    @Test void fileRevisionIsCheckedAndSavedRemovalSurvivesReload()throws Exception{
        Path file=directory.resolve("application.yml");Files.writeString(file,"myapp.ctb-backup.automatic.tenants: [Notes, Other]\n");
        var environment=new StandardEnvironment();new YamlPropertySourceLoader().load("test",new FileSystemResource(file)).forEach(s->environment.getPropertySources().addLast(s));
        var configuration=new ApplicationSettingsService(environment);String revision=configuration.read().revision();
        configuration.edit(revision,yaml->AutomaticBackupTenantList.remove(yaml,"Notes"));
        assertThat(Files.readString(file)).contains("['Other']");
        assertThatThrownBy(()->configuration.edit(revision,yaml->AutomaticBackupTenantList.remove(yaml,"Other"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(Files.readString(file)).contains("Other");
        configuration.edit(configuration.read().revision(),yaml->AutomaticBackupTenantList.enable(yaml,"Notes"));
        assertThat(Files.readString(file)).contains("['Other', 'Notes']","myapp.ctb-backup.automatic.enabled: true");
    }
    @Test void liveSchedulerRemovesOnlyAfterSuccessfulPersistenceAndKeepsOtherAliases(){
        var settings=new CtbBackupSettings();settings.getAutomatic().setTenants(List.of("Notes","Other"));
        var scheduler=new CtbBackupScheduler(settings,mock(CtbBackupSources.class),mock(CtbBackupService.class),mock(CtbBackupStore.class));
        try{
            assertThatThrownBy(()->scheduler.removeTenant("Notes",()->{throw new IllegalArgumentException("stale");})).isInstanceOf(IllegalArgumentException.class);
            assertThat(settings.getAutomatic().getTenants()).containsExactly("Notes","Other");
            scheduler.removeTenant("Notes",()->{});assertThat(settings.getAutomatic().getTenants()).containsExactly("Other");
            scheduler.removeTenant("Other",()->{});assertThat(settings.getAutomatic().getTenants()).isEmpty();
        }finally{scheduler.close();}
    }
}

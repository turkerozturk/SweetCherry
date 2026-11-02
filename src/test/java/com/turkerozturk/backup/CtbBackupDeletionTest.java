package com.turkerozturk.backup;

import com.turkerozturk.multipledatabases.TenantService;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpSession;
import org.sqlite.SQLiteDataSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CtbBackupDeletionTest {
    @TempDir Path folder;
    private final TenantService tenants=mock(TenantService.class);
    private final CtbBackupService jobs=mock(CtbBackupService.class);
    private CtbBackupStore store;
    private CtbBackupDeletionService service;
    private final Time time=new Time();
    static class Time extends Clock {
        Instant now=Instant.parse("2026-11-02T12:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}
    }
    @BeforeEach void setup() {
        var settings=new CtbBackupSettings();settings.setDirectory(folder.resolve("backups").toString());store=new CtbBackupStore(settings);
        when(tenants.getAllTenants()).thenReturn(Map.of());service=new CtbBackupDeletionService(store,new CtbBackupSources(tenants),jobs,time);
    }
    private String backup(boolean automatic) throws Exception {
        String id="20261102-120000-"+UUID.randomUUID();Path file=store.file(id);Files.writeString(file,"Closed snapshot payload "+id);
        store.publish(new CtbBackupStore.Backup(id,file.getFileName().toString(),"<Notes>",time.now.toString(),Files.size(file),CtbBackupStore.sha256(file),Map.of("node",1L),automatic?"a".repeat(64):null,automatic));return id;
    }
    @Test void previewIsReadOnlyAndConfirmationDeletesOnlyReviewedFilesOnce() throws Exception {
        String a=backup(false),b=backup(true);var session=new MockHttpSession();var preview=service.preview(List.of(a,a,b),session);
        assertThat(preview.items()).hasSize(2);assertThat(preview.eligible()).isEqualTo(2);assertThat(store.catalog().backups()).hasSize(2);
        String later=backup(false);var result=service.confirm(preview.token(),session);
        assertThat(result.removed()).isEqualTo(2);assertThat(result.skipped()).isZero();assertThat(store.catalog().backups()).extracting(CtbBackupStore.Backup::id).containsExactly(later);
        assertThatThrownBy(()->service.confirm(preview.token(),session)).hasMessage("expired");
        assertThat(store.catalog().backups()).hasSize(1);
    }
    @Test void expiredChangedOrOtherSessionConfirmationCannotDelete()throws Exception {
        String id=backup(false);var session=new MockHttpSession();var preview=service.preview(List.of(id),session);
        assertThatThrownBy(()->service.confirm(preview.token(),new MockHttpSession())).hasMessage("expired");
        assertThatThrownBy(()->service.confirm("other",session)).hasMessage("expired");
        time.now=time.now.plusSeconds(601);assertThatThrownBy(()->service.confirm(preview.token(),session)).hasMessage("expired");assertThat(store.checked(id)).isNotNull();
    }
    @Test void metadataAndSameSizeContentChangesAreSkippedWithPerFileReasons()throws Exception {
        String content=backup(false),metadata=backup(false),allowed=backup(false);var session=new MockHttpSession();var preview=service.preview(List.of(content,metadata,allowed),session);
        byte[] bytes=Files.readAllBytes(store.file(content));bytes[0]^=1;Files.write(store.file(content),bytes);
        var original=store.get(metadata);store.publish(new CtbBackupStore.Backup(original.id(),original.fileName(),"Changed name",original.created(),original.bytes(),original.sha256(),original.counts()));
        var result=service.confirm(preview.token(),session);
        assertThat(result.removed()).isEqualTo(1);assertThat(result.skipped()).isEqualTo(2);
        assertThat(result.items()).extracting(CtbBackupDeletionService.Outcome::reason).contains("ctbBackup.error.integrity","ctbBackup.error.changed");
        assertThat(Files.exists(store.file(content))).isTrue();assertThat(Files.exists(store.file(metadata))).isTrue();
    }
    @Test void registeredJournaledOrRunningFilesAndWorkingCopiesAreProtected()throws Exception {
        String registered=backup(false),journal=backup(true),running=backup(false),allowed=backup(false);
        var source=new SQLiteDataSource();source.setUrl("jdbc:sqlite:"+store.file(registered));when(tenants.getAllTenants()).thenReturn(Map.of("Registered",source));
        Files.writeString(Path.of(store.file(journal)+"-wal"),"active");
        when(jobs.current()).thenReturn(new CtbBackupService.Status(running,"Running",time.now.toString(),"RUNNING","CLEANING",0,0,null));
        Path working=Files.createDirectories(store.root().resolve("working-copies")).resolve("notes.ctb");Files.writeString(working,"User notes");
        var session=new MockHttpSession();var preview=service.preview(List.of(registered,journal,running,allowed),session);assertThat(preview.eligible()).isEqualTo(1);
        var result=service.confirm(preview.token(),session);assertThat(result.removed()).isEqualTo(1);assertThat(result.skipped()).isEqualTo(3);assertThat(Files.readString(working)).isEqualTo("User notes");
    }
    @Test void fileRegisteredAfterPreviewOrDuringChecksumIsNotRemoved()throws Exception {
        String id=backup(false);var session=new MockHttpSession();var preview=service.preview(List.of(id),session);
        var source=new SQLiteDataSource();source.setUrl("jdbc:sqlite:"+store.file(id));
        when(tenants.getAllTenants()).thenReturn(Map.of(),Map.of("New registration",source));
        var result=service.confirm(preview.token(),session);assertThat(result.removed()).isZero();assertThat(result.items().get(0).reason()).isEqualTo("ctbBackup.error.protected");assertThat(Files.exists(store.file(id))).isTrue();
    }
    @Test void missingSelectionOrTraversalIsRejectedAndMissingReviewedFilesAreNotReplaced()throws Exception {
        assertThatThrownBy(()->service.preview(List.of(),new MockHttpSession())).hasMessage("selection");
        assertThatThrownBy(()->service.preview(Collections.nCopies(201,"id"),new MockHttpSession())).hasMessage("selection");
        assertThatThrownBy(()->service.preview(List.of("../notes"),new MockHttpSession())).hasMessage("missing");
        String id=backup(false);var session=new MockHttpSession();var preview=service.preview(List.of(id),session);Files.delete(store.file(id));String later=backup(false);
        var result=service.confirm(preview.token(),session);assertThat(result.removed()).isZero();assertThat(result.skipped()).isEqualTo(1);assertThat(store.checked(later)).isNotNull();
    }
}

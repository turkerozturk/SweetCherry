package com.turkerozturk.backup;

import com.turkerozturk.multipledatabases.ManagedTenantDataSource;
import com.turkerozturk.multipledatabases.TenantContext;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CtbBackupTest {
    @TempDir Path directory;
    private final CtbBackupEngine engine=new CtbBackupEngine();
    private CtbBackupSettings settings() {var s=new CtbBackupSettings();s.setDirectory(directory.resolve("backups").toString());return s;}
    private Path fixture(String name) throws Exception {
        Path file=directory.resolve(name+".ctb");
        try(var input=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql");var c=DriverManager.getConnection("jdbc:sqlite:"+file);var s=c.createStatement()) {
            for(String sql:new String(input.readAllBytes(),StandardCharsets.UTF_8).lines().filter(line->!line.stripLeading().startsWith("--")).collect(java.util.stream.Collectors.joining("\n")).split(";"))if(!sql.isBlank())s.execute(sql);
            s.execute("INSERT INTO bookmark VALUES(11,1)");
            s.execute("INSERT INTO codebox VALUES(2,0,'left','print(1)','python',300,150,1,0,0)");
            s.execute("INSERT INTO grid VALUES(2,1,'left','<table/>',0,0)");
        }
        return file;
    }
    private DataSource source(Path file) {var source=new SQLiteDataSource();source.setUrl("jdbc:sqlite:"+file);return source;}
    private CtbBackupService.Status await(CtbBackupService service,String id) throws Exception {
        long limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        while("RUNNING".equals(service.status(id).state()) && System.nanoTime()<limit)Thread.sleep(10);
        assertThat(service.status(id).state()).isNotEqualTo("RUNNING");return service.status(id);
    }
    @AfterEach void clear() {TenantContext.clear();}

    @Test void walSnapshotIncludesCommittedObjectsExcludesUncommittedChangesAndPreservesDates() throws Exception {
        Path file=fixture("Türkçe WAL notes");Path target=directory.resolve("snapshot.ctb");byte[] image=new byte[8192];new Random(4).nextBytes(image);
        try(var writer=DriverManager.getConnection("jdbc:sqlite:"+file);var statement=writer.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");statement.execute("PRAGMA wal_autocheckpoint=0");
            statement.execute("UPDATE node SET txt='Committed Türkçe',ts_creation=1700000000,ts_lastsave=1800000000 WHERE node_id=2");
            try(var q=writer.prepareStatement("INSERT INTO image(node_id,offset,png,filename) VALUES(2,2,?,'attachment.bin')")){q.setBytes(1,image);q.executeUpdate();}
            assertThat(Files.size(Path.of(file+"-wal"))).isPositive();
            writer.setAutoCommit(false);statement.execute("UPDATE node SET txt='Not committed' WHERE node_id=2");
            try(var reader=CtbBackupEngine.readOnly(file)) {
                assertThat(engine.sourcePath(reader)).isEqualTo(file.toRealPath());engine.copy(reader,target,(left,total)->{});
            }
            assertThat(engine.verify(target)).containsEntry("image",1L).containsEntry("bookmark",1L).containsEntry("codebox",1L).containsEntry("grid",1L);
            try(var copy=CtbBackupEngine.readOnly(target);var q=copy.createStatement()) {
                try(var r=q.executeQuery("SELECT txt,ts_creation,ts_lastsave FROM node WHERE node_id=2")){assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("Committed Türkçe");assertThat(r.getLong(2)).isEqualTo(1700000000L);assertThat(r.getLong(3)).isEqualTo(1800000000L);}
                try(var r=q.executeQuery("SELECT png FROM image")){r.next();assertThat(r.getBytes(1)).isEqualTo(image);}
                try(var r=q.executeQuery("SELECT master_id,father_id FROM children WHERE node_id=18")){r.next();assertThat(r.getLong(1)).isEqualTo(1);assertThat(r.getLong(2)).isEqualTo(11);}
            }
            writer.rollback();
        }
    }
    @Test void sourceIsPinnedAcrossTenantSwitchAndPoolCloseAndDuplicateAliasesAreRejected() throws Exception {
        Path file=fixture("source");var settings=settings();var store=new CtbBackupStore(settings);var controlled=spy(engine);
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        doAnswer(call->{entered.countDown();if(!release.await(10,TimeUnit.SECONDS))throw new AssertionError("Worker not released");return call.callRealMethod();}).when(controlled).copy(any(),any(),any());
        var source=new ManagedTenantDataSource(()->{var pool=new HikariDataSource();pool.setJdbcUrl("jdbc:sqlite:"+file);return pool;});
        var service=new CtbBackupService(settings,controlled,store);
        try {
            String id=service.start(source,"Original connection");assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
            TenantContext.setCurrentTenant("Another database");
            assertThatThrownBy(()->service.start(source(file),"Alias of the same CTB")).isInstanceOf(CtbBackupProblem.class).hasMessage("busy");
            assertThatThrownBy(source::close).isInstanceOf(IllegalStateException.class);
            release.countDown();assertThat(await(service,id).state()).isEqualTo("SUCCEEDED");
            var backup=store.checked(id);assertThat(backup.sourceName()).isEqualTo("Original connection");
            assertThat(backup.counts()).containsEntry("bookmark",1L);assertThat(store.catalog().backups()).hasSize(1);
        } finally {release.countDown();service.close();source.close();}
    }
    @Test void verificationFailureNeverPublishesPartialSnapshots() throws Exception {
        var settings=settings();var store=new CtbBackupStore(settings);var failing=spy(engine);
        doThrow(new CtbBackupProblem("integrity")).when(failing).verify(any());
        var service=new CtbBackupService(settings,failing,store);
        try {
            String id=service.start(source(fixture("failure")),"Failure");
            assertThat(await(service,id).state()).isEqualTo("FAILED");assertThat(store.catalog().backups()).isEmpty();
            try(var files=Files.list(store.root())){assertThat(files.toList()).isEmpty();}
        } finally {service.close();}
    }
    @Test void workingCopyCanBeEditedWithoutChangingTheBackupOrOriginal() throws Exception {
        Path original=fixture("original");var settings=settings();var store=new CtbBackupStore(settings);var service=new CtbBackupService(settings,engine,store);
        try {
            String id=service.start(source(original),"Original");assertThat(await(service,id).state()).isEqualTo("SUCCEEDED");
            var backup=store.checked(id);Path restored=store.workingCopy(id,engine);
            assertThat(restored).isNotEqualTo(original).isNotEqualTo(store.file(id));assertThat(CtbBackupStore.sha256(restored)).isEqualTo(backup.sha256());
            try(var c=DriverManager.getConnection("jdbc:sqlite:"+restored);var s=c.createStatement()){s.execute("UPDATE node SET txt='Edited restore' WHERE node_id=2");}
            assertThat(store.checked(id).sha256()).isEqualTo(backup.sha256());
            try(var c=CtbBackupEngine.readOnly(original);var s=c.createStatement();var r=s.executeQuery("SELECT txt FROM node WHERE node_id=2")){r.next();assertThat(r.getString(1)).doesNotContain("Edited restore");}
            assertThat(new CtbBackupStore(settings).catalog().backups()).hasSize(1);
        } finally {service.close();}
    }
    @Test void tamperedAndPartialFilesAreNotDownloadableOrUsableForRestore() throws Exception {
        var settings=settings();var store=new CtbBackupStore(settings);var service=new CtbBackupService(settings,engine,store);
        try {
            String id=service.start(source(fixture("tamper")),"Tamper");assertThat(await(service,id).state()).isEqualTo("SUCCEEDED");
            byte[] bytes=Files.readAllBytes(store.file(id));bytes[bytes.length-1]^=1;Files.write(store.file(id),bytes);
            assertThatThrownBy(()->store.checked(id)).isInstanceOf(CtbBackupProblem.class).hasMessage("integrity");
            assertThatThrownBy(()->store.workingCopy(id,engine)).isInstanceOf(CtbBackupProblem.class);
            Files.writeString(store.root().resolve("pending-interrupted.ctb.partial"),"partial");
            assertThat(store.catalog().backups()).hasSize(1); // Structural catalog is cheap; actions verify the checksum.
            for(String invalid:List.of("../source","%2e%2e",id+"/../../source","not-an-id"))assertThatThrownBy(()->store.get(invalid)).isInstanceOf(CtbBackupProblem.class);
        } finally {service.close();}
    }
    @Test void disabledAndUnsupportedSourcesNeverStartAJobOrLeakTheConnection() throws Exception {
        var settings=settings();var store=new CtbBackupStore(settings);var service=new CtbBackupService(settings,engine,store);
        var data=mock(DataSource.class);var connection=mock(Connection.class);when(data.getConnection()).thenReturn(connection);
        try {
            settings.setEnabled(false);assertThatThrownBy(()->service.start(data,"Disabled")).hasMessage("disabled");verifyNoInteractions(data);
            settings.setEnabled(true);assertThatThrownBy(()->service.start(data,"PostgreSQL")).hasMessage("unsupported");verify(connection).close();
            assertThat(service.current()).isNull();
        } finally {service.close();}
    }
    @Test void shutdownDuringCopyCannotPublishTheJobAsCompleted() throws Exception {
        var settings=settings();var store=new CtbBackupStore(settings);var controlled=spy(engine);
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        doAnswer(call->{entered.countDown();if(!release.await(10,TimeUnit.SECONDS))throw new AssertionError("Worker not released");return call.callRealMethod();}).when(controlled).copy(any(),any(),any());
        var service=new CtbBackupService(settings,controlled,store);
        try {
            String id=service.start(source(fixture("interrupted")),"Interrupted");
            assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();service.close();release.countDown();
            assertThat(await(service,id).error()).isEqualTo("ctbBackup.error.interrupted");
            assertThat(store.catalog().backups()).isEmpty();
        } finally {release.countDown();service.close();}
    }
    @Test void readOnlyConnectionCanCreateAFullBackupWithoutChangingTheSourceFile() throws Exception {
        Path file=fixture("read only");String before=CtbBackupStore.sha256(file);
        var settings=settings();var store=new CtbBackupStore(settings);var service=new CtbBackupService(settings,engine,store);
        var readOnly=new org.springframework.jdbc.datasource.AbstractDataSource() {
            @Override public Connection getConnection() throws SQLException {return CtbBackupEngine.readOnly(file);}
            @Override public Connection getConnection(String user,String password) throws SQLException {return getConnection();}
        };
        try {
            String id=service.start(readOnly,"Read only");assertThat(await(service,id).state()).isEqualTo("SUCCEEDED");
            assertThat(store.checked(id).counts()).containsEntry("grid",1L);assertThat(CtbBackupStore.sha256(file)).isEqualTo(before);
        } finally {service.close();}
    }
}
